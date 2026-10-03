package com.waste.power.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.power.common.BizException;
import com.waste.power.common.ConflictException;
import com.waste.power.common.Dict;
import com.waste.power.domain.AbnormalFurnaceSnapshot;
import com.waste.power.domain.AbnormalGrabSnapshot;
import com.waste.power.domain.AbnormalOrderAlarm;
import com.waste.power.domain.AbnormalWorkOrder;
import com.waste.power.domain.EmissionRecord;
import com.waste.power.domain.EnvAlarm;
import com.waste.power.domain.FluidizedBed;
import com.waste.power.domain.GrateFurnace;
import com.waste.power.domain.Incinerator;
import com.waste.power.domain.RotaryKiln;
import com.waste.power.domain.StoragePit;
import com.waste.power.dto.AbnormalOrderPayload;
import com.waste.power.repository.AbnormalFurnaceSnapshotRepository;
import com.waste.power.repository.AbnormalGrabSnapshotRepository;
import com.waste.power.repository.AbnormalOrderAlarmRepository;
import com.waste.power.repository.AbnormalOrderEventRepository;
import com.waste.power.repository.AbnormalWorkOrderRepository;
import com.waste.power.repository.EmissionRecordRepository;
import com.waste.power.repository.EnvAlarmRepository;
import com.waste.power.repository.IncineratorRepository;
import com.waste.power.repository.StoragePitRepository;

/**
 * 异常工况处置单闭环：触发取证、冻结投料、处置申请、双人复核恢复、数据缺失选择。
 */
@Service
@Transactional
public class AbnormalOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "PROCESSING", "REVIEWING", "RECOVERED");

    private final AbnormalWorkOrderRepository orderRepository;
    private final AbnormalFurnaceSnapshotRepository furnaceSnapshotRepository;
    private final AbnormalOrderAlarmRepository orderAlarmRepository;
    private final AbnormalGrabSnapshotRepository grabSnapshotRepository;
    private final AbnormalOrderEventRepository eventRepository;
    private final IncineratorRepository incineratorRepository;
    private final EnvAlarmRepository alarmRepository;
    private final EmissionRecordRepository emissionRepository;
    private final StoragePitRepository pitRepository;
    private final AbnormalOrderEventRecorder eventRecorder;
    private final ObjectMapper objectMapper;

    public AbnormalOrderService(AbnormalWorkOrderRepository orderRepository,
            AbnormalFurnaceSnapshotRepository furnaceSnapshotRepository,
            AbnormalOrderAlarmRepository orderAlarmRepository,
            AbnormalGrabSnapshotRepository grabSnapshotRepository,
            AbnormalOrderEventRepository eventRepository,
            IncineratorRepository incineratorRepository,
            EnvAlarmRepository alarmRepository,
            EmissionRecordRepository emissionRepository,
            StoragePitRepository pitRepository,
            AbnormalOrderEventRecorder eventRecorder,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.furnaceSnapshotRepository = furnaceSnapshotRepository;
        this.orderAlarmRepository = orderAlarmRepository;
        this.grabSnapshotRepository = grabSnapshotRepository;
        this.eventRepository = eventRepository;
        this.incineratorRepository = incineratorRepository;
        this.alarmRepository = alarmRepository;
        this.emissionRepository = emissionRepository;
        this.pitRepository = pitRepository;
        this.eventRecorder = eventRecorder;
        this.objectMapper = objectMapper;
    }

    /** 投料前联锁检查结果；allow=false 时由调用方拒绝本次投料。 */
    public static class FeedingGuardResult {
        private final boolean allowed;
        private final String reason;
        private final AbnormalWorkOrder order;

        private FeedingGuardResult(boolean allowed, String reason, AbnormalWorkOrder order) {
            this.allowed = allowed;
            this.reason = reason;
            this.order = order;
        }

        public static FeedingGuardResult allow() {
            return new FeedingGuardResult(true, null, null);
        }

        public static FeedingGuardResult blocked(String reason, AbnormalWorkOrder order) {
            return new FeedingGuardResult(false, reason, order);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public AbnormalWorkOrder getOrder() {
            return order;
        }
    }

    // ---------------- 查询 ----------------

    @Transactional(readOnly = true)
    public List<AbnormalOrderPayload.VO> list(String status) {
        List<AbnormalWorkOrder> orders = (status == null || status.isBlank())
                ? orderRepository.findTop100ByOrderByCreatedAtDesc()
                : orderRepository.findTop100ByStatusOrderByCreatedAtDesc(normalizeStatus(status));
        return orders.stream().map(this::toListVO).toList();
    }

    @Transactional(readOnly = true)
    public AbnormalOrderPayload.VO detail(Long id) {
        AbnormalWorkOrder order = requireOrder(id);
        AbnormalOrderPayload.VO vo = toDetailVO(order);
        enrichAlarmVerification(vo, order.getFurnaceCode());
        return vo;
    }

    @Transactional(readOnly = true)
    public Optional<AbnormalOrderPayload.VO> activeForFurnace(String furnaceCode) {
        return orderRepository.findActiveByFurnaceCode(furnaceCode).stream()
                .findFirst()
                .map(this::toListVO);
    }

    @Transactional(readOnly = true)
    public boolean hasClosedFrozenOrder(String furnaceCode) {
        return !orderRepository.findClosedFrozenByFurnaceCode(furnaceCode).isEmpty();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evaluate(String furnaceCode) {
        Incinerator furnace = incineratorRepository.findByFurnaceCode(furnaceCode)
                .orElseThrow(() -> new BizException("焚烧炉不存在：" + furnaceCode));
        List<String> items = unqualifiedItems(furnace);
        List<EnvAlarm> severe = activeSevereAlarms(furnaceCode);
        Optional<AbnormalWorkOrder> active = orderRepository.findActiveByFurnaceCode(furnaceCode).stream().findFirst();
        List<AbnormalOrderPayload.AlarmVO> alarmVOs = active.map(o -> orderAlarmRepository
                .findByOrderIdOrderByAlarmTimeAsc(o.getId()).stream()
                .map(AbnormalOrderPayload.AlarmVO::from).toList()).orElse(List.of());
        alarmVOs.forEach(alarm -> verifyAlarm(alarm, furnaceCode));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("furnaceCode", furnaceCode);
        result.put("combustionQualified", items.isEmpty());
        result.put("severeAlarmActive", !severe.isEmpty());
        result.put("qualified", items.isEmpty() && severe.isEmpty());
        result.put("unqualifiedItems", items);
        result.put("severeAlarms", severe.stream().map(alarm -> {
            Map<String, Object> alarmInfo = new LinkedHashMap<>();
            alarmInfo.put("alarmNo", nullToEmpty(alarm.getAlarmNo()));
            alarmInfo.put("indicatorCode", nullToEmpty(alarm.getIndicatorCode()));
            alarmInfo.put("indicatorName", nullToEmpty(alarm.getIndicatorName()));
            alarmInfo.put("measuredValue", nvl(alarm.getMeasuredValue()));
            alarmInfo.put("alarmTime", alarm.getAlarmTime());
            return alarmInfo;
        }).toList());
        result.put("activeOrder", active.map(o -> AbnormalOrderPayload.VO.from(o)).orElse(null));
        boolean closedFrozen = active.isEmpty()
                && !orderRepository.findClosedFrozenByFurnaceCode(furnaceCode).isEmpty();
        result.put("feedingAllowed", active.map(o -> !Boolean.TRUE.equals(o.getFeedingLocked()))
                .orElse(!closedFrozen));
        result.put("attachedAlarms", alarmVOs);
        return result;
    }

    // ---------------- 触发 / 合并 ----------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> registerCombustionIfNeeded(String furnaceCode, String source, String operator,
            BigDecimal submittedTempC, BigDecimal submittedResidenceS) {
        Incinerator current = lockFurnace(furnaceCode);
        // 用瞬态副本判断本次提交，不污染外层事务；若外层最终回滚，已提交快照仍保留当时被拒绝参数
        Incinerator probe = switch (current.getFurnaceTypeCode() == null ? "" : current.getFurnaceTypeCode()) {
            case "FLUIDIZED" -> new FluidizedBed();
            case "ROTARY" -> new RotaryKiln();
            default -> new GrateFurnace();
        };
        copyFurnace(current, probe);
        if (submittedTempC != null) {
            probe.setFurnaceTempC(submittedTempC);
        }
        if (submittedResidenceS != null) {
            probe.setFlueGasResidenceS(submittedResidenceS);
        }
        return registerInternal(probe, source, operator);
    }

    /**
     * 炉况判定入口：无论合格与否均返回判定结果；异常时保存证据并冻结，不用异常承载正常业务结果。
     */
    public Map<String, Object> registerCombustionIfNeeded(String furnaceCode, String source, String operator) {
        return registerInternal(lockFurnace(furnaceCode), source, operator);
    }

    private Map<String, Object> registerInternal(Incinerator furnace, String source, String operator) {
        String furnaceCode = furnace.getFurnaceCode();
        List<String> items = unqualifiedItems(furnace);
        List<EnvAlarm> severeAlarms = activeSevereAlarms(furnaceCode);
        List<String> reasons = new ArrayList<>(items);
        if (!severeAlarms.isEmpty()) {
            reasons.add("同炉存在严重排放告警 " + severeAlarms.size() + " 条");
        }
        AbnormalWorkOrder order = null;
        boolean created = false;
        if (!reasons.isEmpty()) {
            String reason = String.join("；", reasons);
            AbnormalWorkOrder existing = findExistingForUpdate(furnaceCode);
            if (existing == null) {
                String triggerSource = "MANUAL".equals(source) ? "MANUAL"
                        : severeAlarms.isEmpty() ? "AUTO_COMBUSTION" : "AUTO_EMISSION";
                order = createOrder(furnace, triggerSource, reason,
                        operator, Map.of("unqualifiedItems", items, "severeAlarmCount", severeAlarms.size()), severeAlarms);
                created = true;
            } else {
                order = mergeLateEvidence(existing, reason, operator, severeAlarms,
                        "MANUAL".equals(source) ? "MANUAL_COMBUSTION_MERGED" : "AUTO_COMBUSTION_MERGED", "MERGED");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("furnaceCode", furnaceCode);
        result.put("combustionQualified", items.isEmpty());
        result.put("severeAlarmActive", !severeAlarms.isEmpty());
        result.put("qualified", items.isEmpty() && severeAlarms.isEmpty());
        result.put("unqualifiedItems", items);
        result.put("orderCreated", created);
        result.put("orderId", order == null ? null : order.getId());
        result.put("orderNo", order == null ? null : order.getOrderNo());
        result.put("feedingLocked", order != null && Boolean.TRUE.equals(order.getFeedingLocked()));
        result.put("message", order == null
                ? "炉号 " + furnaceCode + " 当前燃烧工况达标且无未解除严重告警"
                : "炉号 " + furnaceCode + " 燃烧工况异常或存在严重告警，已保存处置证据并冻结后续投料");
        return result;
    }

    /** 人工发起：即使当前炉况看起来合格，也按人工确认的原因建立联锁。 */
    public AbnormalOrderPayload.VO createManual(AbnormalOrderPayload.ManualCreateRequest request) {
        if (request == null || isBlank(request.getFurnaceCode())) {
            throw new BizException("炉号不能为空");
        }
        if (isBlank(request.getOwnerName())) {
            throw new BizException("发起负责人不能为空");
        }
        if (isBlank(request.getReason())) {
            throw new BizException("人工发起原因不能为空");
        }
        Incinerator furnace = lockFurnace(request.getFurnaceCode());
        List<String> items = unqualifiedItems(furnace);
        List<EnvAlarm> severe = activeSevereAlarms(furnace.getFurnaceCode());
        String reason = trim(request.getReason(), 512);
        AbnormalWorkOrder existing = findExistingForUpdate(furnace.getFurnaceCode());
        AbnormalWorkOrder order;
        if (existing == null) {
            order = createOrder(furnace, "MANUAL", reason, request.getOwnerName(),
                    Map.of("unqualifiedItems", items, "severeAlarmCount", severe.size()), severe);
        } else {
            order = mergeManual(existing, reason, request.getOwnerName(), severe);
        }
        return toListVO(order);
    }

    /**
     * 新排放严重告警到达：自动建单；同炉已有有效单时合并证据并在待复核/已恢复时反弹回处置中。
     */
    public AbnormalWorkOrder registerSevereAlarm(EnvAlarm alarm) {
        String furnaceCode = furnaceCodeFromStack(alarm.getStackCode());
        if (furnaceCode == null) {
            return null;
        }
        Incinerator furnace = lockFurnace(furnaceCode);
        AbnormalWorkOrder existing = findExistingForUpdate(furnaceCode);
        String reason = "同炉出现严重排放告警：" + nullToEmpty(alarm.getIndicatorName())
                + " 实测 " + alarm.getMeasuredValue() + "，等级 SEVERE";
        if (existing == null) {
            return createOrder(furnace, "AUTO_EMISSION", reason, "系统自动",
                    Map.of("alarmNo", nullToEmpty(alarm.getAlarmNo())), List.of(alarm));
        }
        return mergeLateEvidence(existing, reason, "系统自动", List.of(alarm),
                "AUTO_EMISSION_MERGED", decideAttachSource(existing));
    }

    /**
     * 扫描已有未解除严重告警（含种子数据），用于炉况入口或人工补偿自动触发。
     */
    @Transactional
    public int scanActiveSevereAlarms() {
        List<EnvAlarm> activeAlarms = alarmRepository.findByAlarmLevelOrderByAlarmTimeDesc("SEVERE").stream()
                .filter(alarm -> furnaceCodeFromStack(alarm.getStackCode()) != null)
                .filter(alarm -> {
                    String furnaceCode = furnaceCodeFromStack(alarm.getStackCode());
                    LocalDateTime after = alarm.getAlarmTime() == null ? LocalDateTime.now().minusDays(1) : alarm.getAlarmTime();
                    Optional<EmissionRecord> latest = emissionRepository.findLatestForRecovery(
                            alarm.getIndicatorCode(), nullToEmpty(alarm.getStackCode()), furnaceCode, after);
                    return latest.isEmpty() || Boolean.TRUE.equals(latest.get().getExceeded())
                            || latest.get().getMeasuredValue().compareTo(latest.get().getLimitValue()) > 0;
                })
                .toList();
        int count = 0;
        for (EnvAlarm alarm : activeAlarms) {
            if (registerSevereAlarm(alarm) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 投料前检查：有效单未恢复或关闭但保持冻结时拒绝；当前炉况重新异常也会补建/合并处置单。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "transactionManager")
    public FeedingGuardResult checkFeedingAllowed(String furnaceCode, String operator) {
        Incinerator furnace = lockFurnace(furnaceCode);
        AbnormalWorkOrder active = findExistingForUpdate(furnaceCode);
        if (active == null) {
            List<AbnormalWorkOrder> closedFrozen = orderRepository.findClosedFrozenByFurnaceCode(furnaceCode);
            if (!closedFrozen.isEmpty()) {
                AbnormalWorkOrder closed = closedFrozen.get(0);
                String reason = "炉号 " + furnaceCode + " 的处置单 " + closed.getOrderNo()
                        + " 已按「数据缺失关闭但保持投料冻结」处理，补齐数据并复核前禁止投料";
                eventRecorder.record(closed.getId(), "FEED_BLOCKED", closed.getStatus(), closed.getStatus(),
                        operator, "抓斗投料尝试被联锁拒绝", false, reason);
                return FeedingGuardResult.blocked(reason, closed);
            }
            List<String> items = unqualifiedItems(furnace);
            if (!items.isEmpty()) {
                String reason = "燃烧工况未达标：" + String.join("；", items);
                AbnormalWorkOrder order = createOrder(furnace, "AUTO_COMBUSTION", reason,
                        "投料联锁", Map.of("unqualifiedItems", items), List.of());
                String blockReason = "炉号 " + furnaceCode + " 燃烧工况不达标，已自动冻结后续投料";
                return FeedingGuardResult.blocked(blockReason, order);
            }
            List<EnvAlarm> severe = activeSevereAlarms(furnaceCode);
            if (!severe.isEmpty()) {
                String reason = "同炉存在严重排放告警 " + severe.size() + " 条";
                AbnormalWorkOrder order = createOrder(furnace, "AUTO_EMISSION", reason,
                        "投料联锁", Map.of("alarmCount", severe.size()), severe);
                return FeedingGuardResult.blocked("炉号 " + furnaceCode + " 存在严重排放告警，已自动冻结后续投料", order);
            }
            return FeedingGuardResult.allow();
        }

        if (Boolean.TRUE.equals(active.getFeedingLocked())) {
            String reason = "炉号 " + furnaceCode + " 的处置单 " + active.getOrderNo() + " 当前为 "
                    + Dict.abnormalOrderStatusName(active.getStatus()) + "，投料联锁未解除";
            eventRecorder.record(active.getId(), "FEED_BLOCKED", active.getStatus(), active.getStatus(),
                    operator, "抓斗投料尝试被联锁拒绝", false, reason);
            return FeedingGuardResult.blocked(reason, active);
        }
        return FeedingGuardResult.allow();
    }

    /**
     * 在外层投料事务持有焚烧炉行锁后做最终联锁确认，防止检查后并发恢复/反弹造成误放行。
     */
    public void assertFeedingStillLocked(String furnaceCode, String operator) {
        AbnormalWorkOrder active = findExistingForUpdate(furnaceCode);
        if (active != null && Boolean.TRUE.equals(active.getFeedingLocked())) {
            String reason = "炉号 " + furnaceCode + " 的处置单 " + active.getOrderNo()
                    + " 在投料确认期间仍为冻结状态，投料已取消";
            eventRecorder.record(active.getId(), "FEED_BLOCKED", active.getStatus(), active.getStatus(),
                    operator, "投料最终确认时发现联锁仍有效", false, reason);
            throw new BizException(reason);
        }
    }

    // ---------------- 状态推进 ----------------

    public AbnormalOrderPayload.VO start(Long id, AbnormalOrderPayload.StartRequest request) {
        AbnormalWorkOrder order = requireVersion(id, request == null ? null : request.getExpectedVersion());
        requireText(request == null ? null : request.getOwnerName(), "负责人");
        if (!"PENDING".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            return rejectTransition(order, "START_PROCESSING", request.getOwnerName(), "仅待处置/处置中单据可开始处置");
        }
        String from = order.getStatus();
        order.setOwnerName(trim(request.getOwnerName(), 32));
        order.setStatus("PROCESSING");
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "START_PROCESSING", from, "PROCESSING", request.getOwnerName(),
                "负责人接单并开始处置", true, null);
        return toListVO(saved);
    }

    public AbnormalOrderPayload.VO saveMeasures(Long id, AbnormalOrderPayload.MeasureRequest request) {
        AbnormalWorkOrder order = requireVersion(id, request == null ? null : request.getExpectedVersion());
        requireText(request == null ? null : request.getOwnerName(), "负责人");
        requireText(request == null ? null : request.getMeasures(), "处置措施");
        if (!"PENDING".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            return rejectTransition(order, "SAVE_MEASURES", request.getOwnerName(), "仅待处置/处置中单据可记录措施");
        }
        String from = order.getStatus();
        order.setOwnerName(trim(request.getOwnerName(), 32));
        order.setMeasures(trim(request.getMeasures(), 1024));
        order.setStatus("PROCESSING");
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "SAVE_MEASURES", from, "PROCESSING", request.getOwnerName(),
                order.getMeasures(), true, null);
        return toListVO(saved);
    }

    public AbnormalOrderPayload.VO requestReview(Long id, AbnormalOrderPayload.ReviewRequest request) {
        AbnormalWorkOrder order = requireVersion(id, request == null ? null : request.getExpectedVersion());
        requireText(request == null ? null : request.getOwnerName(), "负责人");
        requireText(request == null ? null : request.getMeasures(), "处置措施");
        if (!"PROCESSING".equals(order.getStatus())) {
            return rejectTransition(order, "REQUEST_REVIEW", request.getOwnerName(), "仅处置中单据可申请复核");
        }
        String owner = trim(request.getOwnerName(), 32);
        if (!owner.equals(order.getOwnerName())) {
            return rejectTransition(order, "REQUEST_REVIEW", owner, "申请复核人必须是当前负责人 " + order.getOwnerName());
        }
        order.setMeasures(trim(request.getMeasures(), 1024));
        order.setReviewRequestNote(trim(request.getNote(), 512));
        order.setRequestedAt(LocalDateTime.now());
        order.setStatus("REVIEWING");
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "REQUEST_REVIEW", "PROCESSING", "REVIEWING", owner,
                nullToEmpty(request.getNote()), true, null);
        return toListVO(saved);
    }

    public AbnormalOrderPayload.VO submitReview(Long id, AbnormalOrderPayload.ReviewSubmitRequest request) {
        AbnormalWorkOrder order = requireVersion(id, request == null ? null : request.getExpectedVersion());
        requireText(request == null ? null : request.getReviewerName(), "复核人");
        requireText(request == null ? null : request.getChoice(), "复核选择");
        if (!"REVIEWING".equals(order.getStatus())) {
            return rejectTransition(order, "REVIEW_SUBMIT", request.getReviewerName(), "仅待复核单据可执行复核");
        }
        String reviewer = trim(request.getReviewerName(), 32);
        if (reviewer.equals(order.getOwnerName())) {
            return rejectTransition(order, "REVIEW_SUBMIT", reviewer,
                    "复核人必须不同于负责人 " + nullToEmpty(order.getOwnerName()));
        }

        RecoveryCheck check = recoveryCheck(order, true);
        String choice = trim(request.getChoice(), 32);
        return switch (choice) {
            case "RECOVER" -> doRecover(order, reviewer, request.getReviewNote(), check);
            case "WAIT" -> doWait(order, reviewer, request.getReviewNote(), check);
            case "CLOSE_KEEP_FROZEN" -> doCloseKeepFrozen(order, reviewer, request.getReviewNote(),
                    request.getMissingReason(), check);
            default -> rejectTransition(order, "REVIEW_SUBMIT", reviewer, "复核选择只能是 RECOVER/WAIT/CLOSE_KEEP_FROZEN");
        };
    }

    public AbnormalOrderPayload.VO close(Long id, AbnormalOrderPayload.CloseRequest request) {
        AbnormalWorkOrder order = requireVersion(id, request == null ? null : request.getExpectedVersion());
        requireText(request == null ? null : request.getOperatorName(), "关闭操作人");
        if (!"RECOVERED".equals(order.getStatus())) {
            return rejectTransition(order, "CLOSE", request.getOperatorName(), "仅已恢复单据可正常关闭");
        }
        String from = order.getStatus();
        order.setStatus("CLOSED");
        order.setFeedingLocked(false);
        order.setFreezeReason("复核恢复后关闭，投料联锁已解除");
        order.setCloseNote(trim(request.getCloseNote(), 512));
        order.setClosedBy(trim(request.getOperatorName(), 32));
        order.setClosedAt(LocalDateTime.now());
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "CLOSE", from, "CLOSED", request.getOperatorName(),
                nullToEmpty(request.getCloseNote()), true, null);
        return toListVO(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> recoveryReadiness(Long id) {
        AbnormalWorkOrder order = requireOrder(id);
        RecoveryCheck check = recoveryCheck(order, false);
        return check.toMap();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("statuses", Dict.ABNORMAL_ORDER_STATUSES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.abnormalOrderStatusName(code))).toList());
        dict.put("choices", List.of(
                Map.of("code", "WAIT", "name", "继续冻结等待补齐",
                        "impact", "单据保持待复核，投料继续冻结；补齐后由另一复核人重新复核恢复"),
                Map.of("code", "CLOSE_KEEP_FROZEN", "name", "记录理由后关闭处置但保持投料冻结",
                        "impact", "单据变为已关闭但投料仍冻结；需另建处置单并复核达标后才能恢复投料")));
        dict.put("furnaceCodes", incineratorRepository.findAll().stream().map(Incinerator::getFurnaceCode).toList());
        return dict;
    }

    /**
     * 验收/值班人员标记某条关联告警的后续监测数据暂不可用；补齐后可取消标记重新取数。
     */
    public AbnormalOrderPayload.VO markAlarmDataMissing(Long orderId, Long alarmId, boolean missing, String operator) {
        AbnormalWorkOrder order = requireOrder(orderId);
        AbnormalOrderAlarm link = orderAlarmRepository.findByOrderIdAndAlarmId(orderId, alarmId)
                .orElseThrow(() -> new BizException("处置单未关联该告警：alarmId=" + alarmId));
        boolean changed = !Boolean.valueOf(missing).equals(Boolean.TRUE.equals(link.getForceMissingData()));
        link.setForceMissingData(missing);
        orderAlarmRepository.save(link);
        if (changed) {
            eventRecorder.record(orderId, missing ? "MARK_DATA_MISSING" : "CLEAR_DATA_MISSING",
                    order.getStatus(), order.getStatus(), operator,
                    missing ? "标记告警后续监测数据暂不可用" : "取消数据缺失标记，重新检查告警数据", true, null);
        }
        return detail(orderId);
    }

    // ---------------- 复核核心 ----------------

    private AbnormalOrderPayload.VO doRecover(AbnormalWorkOrder order, String reviewer, String note, RecoveryCheck check) {
        if (!check.recoverable) {
            String reason = "不能恢复：" + String.join("；", check.blockingReasons);
            eventRecorder.record(order.getId(), "RECOVER_REJECTED", order.getStatus(), order.getStatus(),
                    reviewer, nullToEmpty(note), false, reason);
            throw new BizException(reason);
        }
        String from = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("RECOVERED");
        order.setFeedingLocked(false);
        order.setFreezeReason("燃烧重新达标且严重告警解除，投料联锁已解除");
        order.setReviewerName(reviewer);
        order.setReviewNote(trim(note, 512));
        order.setReviewedAt(now);
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "RECOVERED", from, "RECOVERED", reviewer,
                nullToEmpty(note), true, null);
        return toListVO(saved);
    }

    private AbnormalOrderPayload.VO doWait(AbnormalWorkOrder order, String reviewer, String note, RecoveryCheck check) {
        if (!check.dataMissing) {
            return rejectTransition(order, "WAIT_DATA", reviewer, "只有告警数据暂不可用时才能选择继续冻结等待补齐");
        }
        order.setDataMissingChoice("WAIT");
        order.setDataMissingReason(trim(note, 512));
        order.setReviewerName(reviewer);
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "WAIT_MISSING_DATA", "REVIEWING", "REVIEWING", reviewer,
                "继续冻结等待告警数据补齐：" + nullToEmpty(note), true, null);
        return toListVO(saved);
    }

    private AbnormalOrderPayload.VO doCloseKeepFrozen(AbnormalWorkOrder order, String reviewer, String note,
            String missingReason, RecoveryCheck check) {
        if (!check.dataMissing) {
            return rejectTransition(order, "CLOSE_KEEP_FROZEN", reviewer, "只有告警数据暂不可用时才能选择关闭但保持冻结");
        }
        requireText(missingReason, "数据缺失关闭理由");
        String from = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("CLOSED");
        order.setFeedingLocked(true);
        order.setFreezeReason("告警数据暂不可用，关闭处置单但保持投料冻结");
        order.setReviewerName(reviewer);
        order.setReviewNote(trim(note, 512));
        order.setReviewedAt(now);
        order.setDataMissingChoice("CLOSE_KEEP_FROZEN");
        order.setDataMissingReason(trim(missingReason, 512));
        order.setCloseNote(order.getDataMissingReason());
        order.setClosedBy(reviewer);
        order.setClosedAt(now);
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        eventRecorder.record(saved.getId(), "CLOSE_KEEP_FROZEN", from, "CLOSED", reviewer,
                order.getDataMissingReason(), true, null);
        return toListVO(saved);
    }

    private static class RecoveryCheck {
        boolean combustionQualified;
        boolean dataMissing;
        boolean allAttachedAlarmsCleared;
        List<String> blockingReasons = new ArrayList<>();
        List<AbnormalOrderPayload.AlarmVO> alarms = new ArrayList<>();
        String impact;

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("recoverable", combustionQualified && !dataMissing && allAttachedAlarmsCleared);
            map.put("combustionQualified", combustionQualified);
            map.put("dataMissing", dataMissing);
            map.put("allAttachedAlarmsCleared", allAttachedAlarmsCleared);
            map.put("blockingReasons", blockingReasons);
            map.put("alarms", alarms);
            map.put("impact", impact);
            map.put("feedingLockAfterRecover", "恢复后解除投料冻结");
            map.put("feedingLockAfterWait", "继续冻结投料");
            map.put("feedingLockAfterCloseKeepFrozen", "单据关闭但投料继续冻结，需另建有效处置单复核恢复");
            return map;
        }
    }

    private RecoveryCheck recoveryCheck(AbnormalWorkOrder order, boolean lockFurnace) {
        Incinerator furnace = lockFurnace ? lockFurnace(order.getFurnaceCode())
                : incineratorRepository.findByFurnaceCode(order.getFurnaceCode())
                        .orElseThrow(() -> new BizException("焚烧炉不存在：" + order.getFurnaceCode()));
        RecoveryCheck check = new RecoveryCheck();
        List<String> items = unqualifiedItems(furnace);
        check.combustionQualified = items.isEmpty();
        if (!check.combustionQualified) {
            check.blockingReasons.add("当前燃烧未重新达标：" + String.join("；", items));
        }

        List<AbnormalOrderPayload.AlarmVO> alarmVOs = orderAlarmRepository
                .findByOrderIdOrderByAlarmTimeAsc(order.getId()).stream()
                .map(AbnormalOrderPayload.AlarmVO::from).toList();
        for (AbnormalOrderPayload.AlarmVO alarm : alarmVOs) {
            verifyAlarm(alarm, order.getFurnaceCode());
            if (Boolean.FALSE.equals(alarm.getDataAvailable())) {
                check.dataMissing = true;
                check.blockingReasons.add(alarm.getIndicatorName() + " 告警后的最新监测数据暂不可用");
            } else if (!Boolean.TRUE.equals(alarm.getCleared())) {
                check.allAttachedAlarmsCleared = false;
                check.blockingReasons.add(alarm.getIndicatorName() + " 严重告警尚未解除（最新 "
                        + alarm.getLatestValue() + "，限值 " + alarm.getLimitValue() + "）");
            }
        }
        check.allAttachedAlarmsCleared = !check.dataMissing && !alarmVOs.isEmpty()
                && alarmVOs.stream().allMatch(alarm -> Boolean.TRUE.equals(alarm.getCleared()));
        if (alarmVOs.isEmpty()) {
            check.allAttachedAlarmsCleared = true;
        }
        check.alarms = alarmVOs;
        boolean recoverable = check.combustionQualified && !check.dataMissing && check.allAttachedAlarmsCleared;
        check.impact = check.dataMissing
                ? "可选择「继续冻结等待补齐」或「记录理由后关闭处置但保持投料冻结」，两者均不会恢复投料"
                : (recoverable
                        ? "燃烧达标且严重告警解除，可由另一复核人恢复并解除投料冻结"
                        : "未满足恢复条件，投料必须继续冻结");
        return check;
    }

    // ---------------- 取证 / 快照 ----------------

    private AbnormalWorkOrder createOrder(Incinerator furnace, String source, String reason, String operator,
            Map<String, Object> detail, List<EnvAlarm> initialAlarms) {
        LocalDateTime now = LocalDateTime.now();
        AbnormalWorkOrder order = new AbnormalWorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setFurnaceCode(furnace.getFurnaceCode());
        order.setStatus("PENDING");
        order.setTriggerSource(source);
        order.setTriggerReason(trim(reason, 512));
        order.setTriggerDetailJson(toJson(detail));
        order.setFeedingLocked(true);
        order.setFreezeReason("异常工况处置中，冻结后续投料");
        order.setOwnerName("AUTO_COMBUSTION".equals(source) || "AUTO_EMISSION".equals(source)
                ? "系统自动" : trim(operator, 32));
        order.setVersion(0L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        AbnormalWorkOrder saved = orderRepository.save(order);
        saveFurnaceSnapshot(saved, furnace);
        saveGrabSnapshots(saved, furnace, now);
        for (EnvAlarm alarm : initialAlarms) {
            attachAlarm(saved, alarm, "INITIAL", now);
        }
        eventRecorder.record(saved.getId(), "ORDER_CREATED", null, "PENDING", order.getOwnerName(),
                reason + "；已保存炉况、告警和供料抓斗快照，冻结后续投料", true, null);
        return saved;
    }

    private AbnormalWorkOrder mergeLateEvidence(AbnormalWorkOrder order, String reason, String operator,
            List<EnvAlarm> alarms, String eventType, String attachSource) {
        LocalDateTime now = LocalDateTime.now();
        String from = order.getStatus();
        boolean rebound = false;
        if ("REVIEWING".equals(order.getStatus()) || "RECOVERED".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            order.setFeedingLocked(true);
            order.setFreezeReason("恢复期间再次出现严重告警，退回处置并重新冻结投料");
            order.setReviewerName(null);
            order.setReviewNote(null);
            order.setReviewedAt(null);
            order.setDataMissingChoice(null);
            order.setDataMissingReason(null);
            rebound = true;
        }
        if ("PENDING".equals(order.getStatus()) && operator != null && !"系统自动".equals(operator)
                && !"投料联锁".equals(operator)) {
            order.setOwnerName(trim(operator, 32));
        }
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        for (EnvAlarm alarm : alarms) {
            attachAlarmIfAbsent(saved, alarm, attachSource, now);
        }
        eventRecorder.record(saved.getId(), rebound ? "REBOUND_ALARM" : eventType, from, saved.getStatus(),
                operator, reason, true, null);
        return saved;
    }

    private AbnormalWorkOrder mergeManual(AbnormalWorkOrder order, String reason, String operator,
            List<EnvAlarm> alarms) {
        LocalDateTime now = LocalDateTime.now();
        String from = order.getStatus();
        boolean rebound = false;
        if ("REVIEWING".equals(order.getStatus()) || "RECOVERED".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            order.setFeedingLocked(true);
            order.setFreezeReason("人工补充异常证据，退回处置并保持投料冻结");
            rebound = true;
        }
        if (order.getOwnerName() == null || "系统自动".equals(order.getOwnerName())) {
            order.setOwnerName(trim(operator, 32));
        }
        touch(order);
        AbnormalWorkOrder saved = orderRepository.save(order);
        for (EnvAlarm alarm : alarms) {
            attachAlarmIfAbsent(saved, alarm, "MERGED", now);
        }
        eventRecorder.record(saved.getId(), rebound ? "MANUAL_REBOUND" : "MANUAL_MERGED", from,
                saved.getStatus(), operator, "人工发起与既有有效处置单并发到达，已合并证据：" + reason, true, null);
        return saved;
    }

    private void saveFurnaceSnapshot(AbnormalWorkOrder order, Incinerator furnace) {
        List<String> items = unqualifiedItems(furnace);
        AbnormalFurnaceSnapshot snapshot = new AbnormalFurnaceSnapshot();
        snapshot.setOrderId(order.getId());
        snapshot.setFurnaceCode(furnace.getFurnaceCode());
        snapshot.setFurnaceName(furnace.getFurnaceName());
        snapshot.setFurnaceTypeCode(furnace.getFurnaceTypeCode());
        snapshot.setFurnaceTempC(furnace.getFurnaceTempC());
        snapshot.setFlueGasResidenceS(furnace.getFlueGasResidenceS());
        snapshot.setCoPpm(furnace.getCoPpm());
        snapshot.setCombustionStatus(furnace.getCombustionStatus());
        snapshot.setDailyProcessTon(furnace.getDailyProcessTon());
        snapshot.setGrabNo(furnace.getGrabNo());
        snapshot.setQualified(items.isEmpty());
        snapshot.setUnqualifiedItemsJson(toJson(items));
        snapshot.setSnapshotTime(LocalDateTime.now());
        furnaceSnapshotRepository.save(snapshot);
    }

    private void saveGrabSnapshots(AbnormalWorkOrder order, Incinerator furnace, LocalDateTime now) {
        List<StoragePit> pits = pitRepository.findAll().stream()
                .filter(pit -> furnace.getGrabNo() != null && furnace.getGrabNo().equals(pit.getGrabNo()))
                .toList();
        if (pits.isEmpty()) {
            AbnormalGrabSnapshot snapshot = new AbnormalGrabSnapshot();
            snapshot.setOrderId(order.getId());
            snapshot.setGrabNo(nullToEmpty(furnace.getGrabNo()));
            snapshot.setSnapshotTime(now);
            grabSnapshotRepository.save(snapshot);
            return;
        }
        for (StoragePit pit : pits) {
            AbnormalGrabSnapshot snapshot = new AbnormalGrabSnapshot();
            snapshot.setOrderId(order.getId());
            snapshot.setGrabNo(pit.getGrabNo());
            snapshot.setPitNo(pit.getPitNo());
            snapshot.setPitName(pit.getPitName());
            snapshot.setCurrentStockTon(pit.getCurrentStockTon());
            snapshot.setPitStatus(pit.getStatus());
            snapshot.setSnapshotTime(now);
            grabSnapshotRepository.save(snapshot);
        }
    }

    private void attachAlarm(AbnormalWorkOrder order, EnvAlarm alarm, String source, LocalDateTime now) {
        AbnormalOrderAlarm link = new AbnormalOrderAlarm();
        link.setOrderId(order.getId());
        link.setAlarmId(alarm.getId());
        link.setAlarmNo(alarm.getAlarmNo());
        link.setIndicatorCode(alarm.getIndicatorCode());
        link.setIndicatorName(alarm.getIndicatorName());
        link.setMeasuredValue(alarm.getMeasuredValue());
        link.setLimitValue(alarm.getLimitValue());
        link.setExceedRatio(alarm.getExceedRatio());
        link.setAlarmLevel(alarm.getAlarmLevel());
        link.setPeriodLabel(alarm.getPeriodLabel());
        link.setStackCode(alarm.getStackCode());
        link.setAlarmTime(alarm.getAlarmTime());
        link.setAttachedSource(source);
        link.setAttachedAt(now);
        orderAlarmRepository.save(link);
    }

    private void attachAlarmIfAbsent(AbnormalWorkOrder order, EnvAlarm alarm, String source, LocalDateTime now) {
        if (orderAlarmRepository.findByOrderIdAndAlarmId(order.getId(), alarm.getId()).isEmpty()) {
            attachAlarm(order, alarm, source, now);
        }
    }

    private void verifyAlarm(AbnormalOrderPayload.AlarmVO vo, String furnaceCode) {
        if (Boolean.TRUE.equals(vo.getForceMissingData())) {
            vo.setDataAvailable(false);
            vo.setCleared(false);
            vo.setVerifyMessage("已人工标记该告警数据暂不可用，需补齐后复核");
            return;
        }
        LocalDateTime after = vo.getAlarmTime() == null ? LocalDateTime.now().minusDays(1) : vo.getAlarmTime();
        String stack = vo.getStackCode() == null ? "STACK-01" : vo.getStackCode();
        Optional<EmissionRecord> latest = emissionRepository.findLatestForRecovery(
                vo.getIndicatorCode(), stack, furnaceCode, after);
        if (latest.isEmpty()) {
            vo.setDataAvailable(false);
            vo.setCleared(false);
            vo.setVerifyMessage("告警发生后暂无该指标监测数据，不能判定解除");
            return;
        }
        EmissionRecord record = latest.get();
        vo.setDataAvailable(true);
        vo.setLatestValue(record.getMeasuredValue());
        vo.setLatestMonitorTime(record.getMonitorTime());
        vo.setCleared(!Boolean.TRUE.equals(record.getExceeded())
                && record.getMeasuredValue().compareTo(record.getLimitValue()) <= 0);
        vo.setVerifyMessage(Boolean.TRUE.equals(vo.getCleared())
                ? "最新监测值已回落至限值以内"
                : "最新监测值仍超过限值，严重告警未解除");
    }

    // ---------------- 组装 / 基础 ----------------

    private AbnormalOrderPayload.VO toListVO(AbnormalWorkOrder order) {
        AbnormalOrderPayload.VO vo = AbnormalOrderPayload.VO.from(order);
        AbnormalFurnaceSnapshot snapshot = furnaceSnapshotRepository.findByOrderId(order.getId()).orElse(null);
        if (snapshot != null) {
            vo.setFurnaceSnapshot(AbnormalOrderPayload.FurnaceSnapshotVO.from(snapshot, readItems(snapshot)));
        }
        vo.setAlarms(orderAlarmRepository.findByOrderIdOrderByAlarmTimeAsc(order.getId()).stream()
                .map(AbnormalOrderPayload.AlarmVO::from).toList());
        vo.setGrabSnapshots(grabSnapshotRepository.findByOrderIdOrderByPitNoAsc(order.getId()).stream()
                .map(AbnormalOrderPayload.GrabVO::from).toList());
        return vo;
    }

    private AbnormalOrderPayload.VO toDetailVO(AbnormalWorkOrder order) {
        AbnormalOrderPayload.VO vo = toListVO(order);
        vo.setEvents(eventRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId()).stream()
                .map(AbnormalOrderPayload.EventVO::from).toList());
        return vo;
    }

    private void enrichAlarmVerification(AbnormalOrderPayload.VO vo, String furnaceCode) {
        if (vo.getAlarms() != null) {
            vo.getAlarms().forEach(alarm -> verifyAlarm(alarm, furnaceCode));
        }
    }

    private List<String> readItems(AbnormalFurnaceSnapshot snapshot) {
        String json = snapshot.getUnqualifiedItemsJson();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private AbnormalWorkOrder findExistingForUpdate(String furnaceCode) {
        List<AbnormalWorkOrder> active = orderRepository.findActiveByFurnaceCodeForUpdate(furnaceCode);
        return active.isEmpty() ? null : active.get(0);
    }

    private Incinerator lockFurnace(String furnaceCode) {
        return incineratorRepository.findByFurnaceCodeForUpdate(furnaceCode)
                .orElseThrow(() -> new BizException("焚烧炉不存在：" + furnaceCode));
    }

    private AbnormalWorkOrder requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BizException("异常工况处置单不存在：id=" + id));
    }

    private AbnormalWorkOrder requireVersion(Long id, Long expectedVersion) {
        AbnormalWorkOrder order = requireOrder(id);
        if (expectedVersion != null && !expectedVersion.equals(order.getVersion())) {
            String reason = "提交版本 " + expectedVersion + " 已过期，当前版本为 " + order.getVersion();
            eventRecorder.record(order.getId(), "STALE_VERSION_REJECTED", order.getStatus(), order.getStatus(),
                    null, "过期版本提交被拒绝", false, reason);
            throw new ConflictException(reason + "，请刷新后重试");
        }
        return order;
    }

    private AbnormalOrderPayload.VO rejectTransition(AbnormalWorkOrder order, String eventType, String operator,
            String reason) {
        eventRecorder.record(order.getId(), eventType, order.getStatus(), order.getStatus(), operator,
                "状态操作被拒绝", false, reason);
        throw new BizException(reason);
    }

    private List<EnvAlarm> activeSevereAlarms(String furnaceCode) {
        return alarmRepository.findByAlarmLevelOrderByAlarmTimeDesc("SEVERE").stream()
                .filter(alarm -> furnaceCode.equals(furnaceCodeFromStack(alarm.getStackCode())))
                .filter(alarm -> {
                    LocalDateTime after = alarm.getAlarmTime() == null ? LocalDateTime.now().minusDays(1) : alarm.getAlarmTime();
                    Optional<EmissionRecord> latest = emissionRepository.findLatestForRecovery(
                            alarm.getIndicatorCode(), nullToEmpty(alarm.getStackCode()), furnaceCode, after);
                    return latest.isEmpty() || Boolean.TRUE.equals(latest.get().getExceeded())
                            || latest.get().getMeasuredValue().compareTo(latest.get().getLimitValue()) > 0;
                })
                .collect(Collectors.toList());
    }

    private String decideAttachSource(AbnormalWorkOrder order) {
        return ("REVIEWING".equals(order.getStatus()) || "RECOVERED".equals(order.getStatus()))
                ? "REBOUND" : "MERGED";
    }

    private void copyFurnace(Incinerator source, Incinerator target) {
        target.setId(source.getId());
        target.setFurnaceCode(source.getFurnaceCode());
        target.setFurnaceName(source.getFurnaceName());
        target.setFurnaceTypeCode(source.getFurnaceTypeCode());
        target.setRatedCapacityTpd(source.getRatedCapacityTpd());
        target.setFurnaceTempC(source.getFurnaceTempC());
        target.setFlueGasResidenceS(source.getFlueGasResidenceS());
        target.setCoPpm(source.getCoPpm());
        target.setCombustionStatus(source.getCombustionStatus());
        target.setDailyProcessTon(source.getDailyProcessTon());
        target.setRunningHours(source.getRunningHours());
        target.setLastStartTime(source.getLastStartTime());
        target.setGrabNo(source.getGrabNo());
        target.setRemark(source.getRemark());
    }

    private List<String> unqualifiedItems(Incinerator furnace) {
        List<String> items = new ArrayList<>();
        BigDecimal temp = nvl(furnace.getFurnaceTempC());
        BigDecimal residence = nvl(furnace.getFlueGasResidenceS());
        if (temp.compareTo(Dict.MIN_FURNACE_TEMP_C) < 0) {
            items.add("炉膛温度 " + temp + "℃，低于 " + Dict.MIN_FURNACE_TEMP_C + "℃ 达标线");
        }
        if (residence.compareTo(Dict.MIN_RESIDENCE_S) < 0) {
            items.add("烟气停留时间 " + residence + " s，不足 " + Dict.MIN_RESIDENCE_S + " s");
        }
        return items;
    }

    private String furnaceCodeFromStack(String stackCode) {
        if (stackCode == null || stackCode.isBlank()) {
            return null;
        }
        String code = stackCode.trim().toUpperCase();
        if (code.matches("F-\\d+")) {
            return code;
        }
        if (code.startsWith("STACK-")) {
            try {
                int number = Integer.parseInt(code.substring("STACK-".length()));
                return String.format("F-%02d", number);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String generateOrderNo() {
        return "AWO" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private void touch(AbnormalWorkOrder order) {
        order.setUpdatedAt(LocalDateTime.now());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String normalizeStatus(String status) {
        if (!Dict.isValidAbnormalOrderStatus(status)) {
            throw new BizException("处置单状态不合法：" + status);
        }
        return status;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BizException(label + "不能为空");
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
