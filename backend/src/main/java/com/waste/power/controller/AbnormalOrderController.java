package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.AbnormalOrderPayload;
import com.waste.power.service.AbnormalOrderService;

/**
 * 异常工况处置单接口：自动/人工触发、处置、申请复核、复核恢复、数据缺失选择和操作轨迹。
 */
@RestController
@RequestMapping("/api/abnormal-orders")
public class AbnormalOrderController {

    private final AbnormalOrderService orderService;

    public AbnormalOrderController(AbnormalOrderService orderService) {
        this.orderService = order;
    }

    @GetMapping
    public ApiResponse<List<AbnormalOrderPayload.VO>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(orderService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AbnormalOrderPayload.VO> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id));
    }

    @PostMapping("/manual")
    public ApiResponse<AbnormalOrderPayload.VO> createManual(@RequestBody AbnormalOrderPayload.ManualCreateRequest request) {
        return ApiResponse.success("已发起异常工况处置单并冻结后续投料", orderService.createManual(request));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<AbnormalOrderPayload.VO> start(@PathVariable Long id,
            @RequestBody AbnormalOrderPayload.StartRequest request) {
        return ApiResponse.success("已开始处置", orderService.start(id, request));
    }

    @PostMapping("/{id}/measures")
    public ApiResponse<AbnormalOrderPayload.VO> measures(@PathVariable Long id,
            @RequestBody AbnormalOrderPayload.MeasureRequest request) {
        return ApiResponse.success("处置措施已记录", orderService.saveMeasures(id, request));
    }

    @PostMapping("/{id}/review-request")
    public ApiResponse<AbnormalOrderPayload.VO> requestReview(@PathVariable Long id,
            @RequestBody AbnormalOrderPayload.ReviewRequest request) {
        return ApiResponse.success("已提交复核申请", orderService.requestReview(id, request));
    }

    @PostMapping("/{id}/alarms/{alarmId}/data-missing")
    public ApiResponse<AbnormalOrderPayload.VO> markDataMissing(@PathVariable Long id,
            @PathVariable Long alarmId,
            @RequestParam(defaultValue = "true") boolean missing,
            @RequestParam(defaultValue = "值班员") String operator) {
        return ApiResponse.success("告警数据可用性标记已更新",
                orderService.markAlarmDataMissing(id, alarmId, missing, operator));
    }

    @GetMapping("/{id}/recovery-readiness")
    public ApiResponse<Map<String, Object>> recoveryReadiness(@PathVariable Long id) {
        return ApiResponse.success(orderService.recoveryReadiness(id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<AbnormalOrderPayload.VO> review(@PathVariable Long id,
            @RequestBody AbnormalOrderPayload.ReviewSubmitRequest request) {
        return ApiResponse.success("复核处理完成", orderService.submitReview(id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<AbnormalOrderPayload.VO> close(@PathVariable Long id,
            @RequestBody AbnormalOrderPayload.CloseRequest request) {
        return ApiResponse.success("处置单已关闭", orderService.close(id, request));
    }

    @PostMapping("/scan-severe")
    public ApiResponse<Map<String, Object>> scanSevere() {
        int count = orderService.scanActiveSevereAlarms();
        return ApiResponse.success("已同步严重告警，处理炉数 " + count, Map.of("affectedFurnaceCount", count));
    }

    @GetMapping("/furnace/{furnaceCode}/active")
    public ApiResponse<AbnormalOrderPayload.VO> active(@PathVariable String furnaceCode) {
        return ApiResponse.success(orderService.activeForFurnace(furnaceCode).orElse(null));
    }

    @GetMapping("/furnace/{furnaceCode}/evaluate")
    public ApiResponse<Map<String, Object>> evaluate(@PathVariable String furnaceCode) {
        return ApiResponse.success(orderService.evaluate(furnaceCode));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(orderService.dict());
    }
}
