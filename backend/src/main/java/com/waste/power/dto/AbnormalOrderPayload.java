package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.waste.power.domain.AbnormalFurnaceSnapshot;
import com.waste.power.domain.AbnormalGrabSnapshot;
import com.waste.power.domain.AbnormalOrderAlarm;
import com.waste.power.domain.AbnormalOrderEvent;
import com.waste.power.domain.AbnormalWorkOrder;

/**
 * 异常工况处置单请求与视图对象。
 */
public class AbnormalOrderPayload {

    public static class ManualCreateRequest {
        private String furnaceCode;
        private String ownerName;
        private String reason;

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class StartRequest {
        private Long expectedVersion;
        private String ownerName;

        public Long getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Long expectedVersion) {
            this.expectedVersion = expectedVersion;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }
    }

    public static class MeasureRequest {
        private Long expectedVersion;
        private String ownerName;
        private String measures;

        public Long getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Long expectedVersion) {
            this.expectedVersion = expectedVersion;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getMeasures() {
            return measures;
        }

        public void setMeasures(String measures) {
            this.measures = measures;
        }
    }

    public static class ReviewRequest {
        private Long expectedVersion;
        private String ownerName;
        private String note;

        public Long getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Long expectedVersion) {
            this.expectedVersion = expectedVersion;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class ReviewSubmitRequest {
        private Long expectedVersion;
        private String reviewerName;
        /** RECOVER / WAIT / CLOSE_KEEP_FROZEN */
        private String choice;
        private String reviewNote;
        private String missingReason;

        public Long getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Long expectedVersion) {
            this.expectedVersion = expectedVersion;
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public void setReviewerName(String reviewerName) {
            this.reviewerName = reviewerName;
        }

        public String getChoice() {
            return choice;
        }

        public void setChoice(String choice) {
            this.choice = choice;
        }

        public String getReviewNote() {
            return reviewNote;
        }

        public void setReviewNote(String reviewNote) {
            this.reviewNote = reviewNote;
        }

        public String getMissingReason() {
            return missingReason;
        }

        public void setMissingReason(String missingReason) {
            this.missingReason = missingReason;
        }
    }

    public static class CloseRequest {
        private Long expectedVersion;
        private String operatorName;
        private String closeNote;

        public Long getExpectedVersion() {
            return expectedVersion;
        }

        public void setExpectedVersion(Long expectedVersion) {
            this.expectedVersion = expectedVersion;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getCloseNote() {
            return closeNote;
        }

        public void setCloseNote(String closeNote) {
            this.closeNote = closeNote;
        }
    }

    public static class FurnaceSnapshotVO {
        private BigDecimal furnaceTempC;
        private BigDecimal flueGasResidenceS;
        private BigDecimal coPpm;
        private String combustionStatus;
        private BigDecimal dailyProcessTon;
        private String grabNo;
        private Boolean qualified;
        private List<String> unqualifiedItems;
        private LocalDateTime snapshotTime;

        public static FurnaceSnapshotVO from(AbnormalFurnaceSnapshot snapshot, List<String> items) {
            FurnaceSnapshotVO vo = new FurnaceSnapshotVO();
            vo.furnaceTempC = snapshot.getFurnaceTempC();
            vo.flueGasResidenceS = snapshot.getFlueGasResidenceS();
            vo.coPpm = snapshot.getCoPpm();
            vo.combustionStatus = snapshot.getCombustionStatus();
            vo.dailyProcessTon = snapshot.getDailyProcessTon();
            vo.grabNo = snapshot.getGrabNo();
            vo.qualified = snapshot.getQualified();
            vo.unqualifiedItems = items;
            vo.snapshotTime = snapshot.getSnapshotTime();
            return vo;
        }

        public BigDecimal getFurnaceTempC() { return furnaceTempC; }
        public void setFurnaceTempC(BigDecimal furnaceTempC) { this.furnaceTempC = furnaceTempC; }
        public BigDecimal getFlueGasResidenceS() { return flueGasResidenceS; }
        public void setFlueGasResidenceS(BigDecimal flueGasResidenceS) { this.flueGasResidenceS = flueGasResidenceS; }
        public BigDecimal getCoPpm() { return coPpm; }
        public void setCoPpm(BigDecimal coPpm) { this.coPpm = coPpm; }
        public String getCombustionStatus() { return combustionStatus; }
        public void setCombustionStatus(String combustionStatus) { this.combustionStatus = combustionStatus; }
        public BigDecimal getDailyProcessTon() { return dailyProcessTon; }
        public void setDailyProcessTon(BigDecimal dailyProcessTon) { this.dailyProcessTon = dailyProcessTon; }
        public String getGrabNo() { return grabNo; }
        public void setGrabNo(String grabNo) { this.grabNo = grabNo; }
        public Boolean getQualified() { return qualified; }
        public void setQualified(Boolean qualified) { this.qualified = qualified; }
        public List<String> getUnqualifiedItems() { return unqualifiedItems; }
        public void setUnqualifiedItems(List<String> unqualifiedItems) { this.unqualifiedItems = unqualifiedItems; }
        public LocalDateTime getSnapshotTime() { return snapshotTime; }
        public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    }

    public static class AlarmVO {
        private Long alarmId;
        private String alarmNo;
        private String indicatorCode;
        private String indicatorName;
        private BigDecimal measuredValue;
        private BigDecimal limitValue;
        private BigDecimal exceedRatio;
        private String alarmLevel;
        private String periodLabel;
        private String stackCode;
        private LocalDateTime alarmTime;
        private String attachedSource;
        private LocalDateTime attachedAt;
        private Boolean forceMissingData;
        private Boolean cleared;
        private Boolean dataAvailable;
        private BigDecimal latestValue;
        private LocalDateTime latestMonitorTime;
        private String verifyMessage;

        public static AlarmVO from(AbnormalOrderAlarm alarm) {
            AlarmVO vo = new AlarmVO();
            vo.alarmId = alarm.getAlarmId();
            vo.alarmNo = alarm.getAlarmNo();
            vo.indicatorCode = alarm.getIndicatorCode();
            vo.indicatorName = alarm.getIndicatorName();
            vo.measuredValue = alarm.getMeasuredValue();
            vo.limitValue = alarm.getLimitValue();
            vo.exceedRatio = alarm.getExceedRatio();
            vo.alarmLevel = alarm.getAlarmLevel();
            vo.periodLabel = alarm.getPeriodLabel();
            vo.stackCode = alarm.getStackCode();
            vo.alarmTime = alarm.getAlarmTime();
            vo.attachedSource = alarm.getAttachedSource();
            vo.attachedAt = alarm.getAttachedAt();
            vo.forceMissingData = Boolean.TRUE.equals(alarm.getForceMissingData());
            return vo;
        }

        public Long getAlarmId() { return alarmId; }
        public void setAlarmId(Long alarmId) { this.alarmId = alarmId; }
        public String getAlarmNo() { return alarmNo; }
        public void setAlarmNo(String alarmNo) { this.alarmNo = alarmNo; }
        public String getIndicatorCode() { return indicatorCode; }
        public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
        public String getIndicatorName() { return indicatorName; }
        public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
        public BigDecimal getMeasuredValue() { return measuredValue; }
        public void setMeasuredValue(BigDecimal measuredValue) { this.measuredValue = measuredValue; }
        public BigDecimal getLimitValue() { return limitValue; }
        public void setLimitValue(BigDecimal limitValue) { this.limitValue = limitValue; }
        public BigDecimal getExceedRatio() { return exceedRatio; }
        public void setExceedRatio(BigDecimal exceedRatio) { this.exceedRatio = exceedRatio; }
        public String getAlarmLevel() { return alarmLevel; }
        public void setAlarmLevel(String alarmLevel) { this.alarmLevel = alarmLevel; }
        public String getPeriodLabel() { return periodLabel; }
        public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
        public String getStackCode() { return stackCode; }
        public void setStackCode(String stackCode) { this.stackCode = stackCode; }
        public LocalDateTime getAlarmTime() { return alarmTime; }
        public void setAlarmTime(LocalDateTime alarmTime) { this.alarmTime = alarmTime; }
        public String getAttachedSource() { return attachedSource; }
        public void setAttachedSource(String attachedSource) { this.attachedSource = attachedSource; }
        public LocalDateTime getAttachedAt() { return attachedAt; }
        public void setAttachedAt(LocalDateTime attachedAt) { this.attachedAt = attachedAt; }
        public Boolean getForceMissingData() { return forceMissingData; }
        public void setForceMissingData(Boolean forceMissingData) { this.forceMissingData = forceMissingData; }
        public Boolean getCleared() { return cleared; }
        public void setCleared(Boolean cleared) { this.cleared = cleared; }
        public Boolean getDataAvailable() { return dataAvailable; }
        public void setDataAvailable(Boolean dataAvailable) { this.dataAvailable = dataAvailable; }
        public BigDecimal getLatestValue() { return latestValue; }
        public void setLatestValue(BigDecimal latestValue) { this.latestValue = latestValue; }
        public LocalDateTime getLatestMonitorTime() { return latestMonitorTime; }
        public void setLatestMonitorTime(LocalDateTime latestMonitorTime) { this.latestMonitorTime = latestMonitorTime; }
        public String getVerifyMessage() { return verifyMessage; }
        public void setVerifyMessage(String verifyMessage) { this.verifyMessage = verifyMessage; }
    }

    public static class GrabVO {
        private String grabNo;
        private String pitNo;
        private String pitName;
        private BigDecimal currentStockTon;
        private String pitStatus;
        private LocalDateTime snapshotTime;

        public static GrabVO from(AbnormalGrabSnapshot snapshot) {
            GrabVO vo = new GrabVO();
            vo.grabNo = snapshot.getGrabNo();
            vo.pitNo = snapshot.getPitNo();
            vo.pitName = snapshot.getPitName();
            vo.currentStockTon = snapshot.getCurrentStockTon();
            vo.pitStatus = snapshot.getPitStatus();
            vo.snapshotTime = snapshot.getSnapshotTime();
            return vo;
        }

        public String getGrabNo() { return grabNo; }
        public void setGrabNo(String grabNo) { this.grabNo = grabNo; }
        public String getPitNo() { return pitNo; }
        public void setPitNo(String pitNo) { this.pitNo = pitNo; }
        public String getPitName() { return pitName; }
        public void setPitName(String pitName) { this.pitName = pitName; }
        public BigDecimal getCurrentStockTon() { return currentStockTon; }
        public void setCurrentStockTon(BigDecimal currentStockTon) { this.currentStockTon = currentStockTon; }
        public String getPitStatus() { return pitStatus; }
        public void setPitStatus(String pitStatus) { this.pitStatus = pitStatus; }
        public LocalDateTime getSnapshotTime() { return snapshotTime; }
        public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    }

    public static class EventVO {
        private Long id;
        private String eventType;
        private String fromStatus;
        private String toStatus;
        private String operator;
        private String detail;
        private Boolean success;
        private String failureReason;
        private LocalDateTime createdAt;

        public static EventVO from(AbnormalOrderEvent event) {
            EventVO vo = new EventVO();
            vo.id = event.getId();
            vo.eventType = event.getEventType();
            vo.fromStatus = event.getFromStatus();
            vo.toStatus = event.getToStatus();
            vo.operator = event.getOperator();
            vo.detail = event.getDetail();
            vo.success = event.getSuccess();
            vo.failureReason = event.getFailureReason();
            vo.createdAt = event.getCreatedAt();
            return vo;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class VO {
        private Long id;
        private String orderNo;
        private String furnaceCode;
        private String status;
        private String statusName;
        private String triggerSource;
        private String triggerReason;
        private Boolean feedingLocked;
        private String freezeReason;
        private String ownerName;
        private String measures;
        private String reviewRequestNote;
        private LocalDateTime requestedAt;
        private String reviewerName;
        private String reviewNote;
        private LocalDateTime reviewedAt;
        private String dataMissingChoice;
        private String dataMissingReason;
        private String closeNote;
        private LocalDateTime closedAt;
        private String closedBy;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private FurnaceSnapshotVO furnaceSnapshot;
        private List<AlarmVO> alarms;
        private List<GrabVO> grabSnapshots;
        private List<EventVO> events;

        public static VO from(AbnormalWorkOrder order) {
            VO vo = new VO();
            vo.id = order.getId();
            vo.orderNo = order.getOrderNo();
            vo.furnaceCode = order.getFurnaceCode();
            vo.status = order.getStatus();
            vo.statusName = com.waste.power.common.Dict.abnormalOrderStatusName(order.getStatus());
            vo.triggerSource = order.getTriggerSource();
            vo.triggerReason = order.getTriggerReason();
            vo.feedingLocked = order.getFeedingLocked();
            vo.freezeReason = order.getFreezeReason();
            vo.ownerName = order.getOwnerName();
            vo.measures = order.getMeasures();
            vo.reviewRequestNote = order.getReviewRequestNote();
            vo.requestedAt = order.getRequestedAt();
            vo.reviewerName = order.getReviewerName();
            vo.reviewNote = order.getReviewNote();
            vo.reviewedAt = order.getReviewedAt();
            vo.dataMissingChoice = order.getDataMissingChoice();
            vo.dataMissingReason = order.getDataMissingReason();
            vo.closeNote = order.getCloseNote();
            vo.closedAt = order.getClosedAt();
            vo.closedBy = order.getClosedBy();
            vo.version = order.getVersion();
            vo.createdAt = order.getCreatedAt();
            vo.updatedAt = order.getUpdatedAt();
            return vo;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getFurnaceCode() { return furnaceCode; }
        public void setFurnaceCode(String furnaceCode) { this.furnaceCode = furnaceCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getTriggerSource() { return triggerSource; }
        public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
        public String getTriggerReason() { return triggerReason; }
        public void setTriggerReason(String triggerReason) { this.triggerReason = triggerReason; }
        public Boolean getFeedingLocked() { return feedingLocked; }
        public void setFeedingLocked(Boolean feedingLocked) { this.feedingLocked = feedingLocked; }
        public String getFreezeReason() { return freezeReason; }
        public void setFreezeReason(String freezeReason) { this.freezeReason = freezeReason; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getMeasures() { return measures; }
        public void setMeasures(String measures) { this.measures = measures; }
        public String getReviewRequestNote() { return reviewRequestNote; }
        public void setReviewRequestNote(String reviewRequestNote) { this.reviewRequestNote = reviewRequestNote; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
        public String getReviewNote() { return reviewNote; }
        public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
        public LocalDateTime getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
        public String getDataMissingChoice() { return dataMissingChoice; }
        public void setDataMissingChoice(String dataMissingChoice) { this.dataMissingChoice = dataMissingChoice; }
        public String getDataMissingReason() { return dataMissingReason; }
        public void setDataMissingReason(String dataMissingReason) { this.dataMissingReason = dataMissingReason; }
        public String getCloseNote() { return closeNote; }
        public void setCloseNote(String closeNote) { this.closeNote = closeNote; }
        public LocalDateTime getClosedAt() { return closedAt; }
        public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
        public String getClosedBy() { return closedBy; }
        public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public FurnaceSnapshotVO getFurnaceSnapshot() { return furnaceSnapshot; }
        public void setFurnaceSnapshot(FurnaceSnapshotVO furnaceSnapshot) { this.furnaceSnapshot = furnaceSnapshot; }
        public List<AlarmVO> getAlarms() { return alarms; }
        public void setAlarms(List<AlarmVO> alarms) { this.alarms = alarms; }
        public List<GrabVO> getGrabSnapshots() { return grabSnapshots; }
        public void setGrabSnapshots(List<GrabVO> grabSnapshots) { this.grabSnapshots = grabSnapshots; }
        public List<EventVO> getEvents() { return events; }
        public void setEvents(List<EventVO> events) { this.events = events; }
    }
}
