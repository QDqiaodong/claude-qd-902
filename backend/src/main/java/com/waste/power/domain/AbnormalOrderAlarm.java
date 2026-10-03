package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 处置单关联的严重排放告警证据。不依赖 env_alarm.handled，恢复时以最新实测数据复核。
 */
@Entity
@Table(name = "abnormal_order_alarm")
public class AbnormalOrderAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "alarm_id", nullable = false)
    private Long alarmId;

    @Column(name = "alarm_no", nullable = false, length = 32)
    private String alarmNo;

    @Column(name = "indicator_code", nullable = false, length = 16)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 32)
    private String indicatorName;

    @Column(name = "measured_value", precision = 12, scale = 3)
    private BigDecimal measuredValue;

    @Column(name = "limit_value", precision = 12, scale = 3)
    private BigDecimal limitValue;

    @Column(name = "exceed_ratio", precision = 8, scale = 2)
    private BigDecimal exceedRatio;

    @Column(name = "alarm_level", length = 16)
    private String alarmLevel;

    @Column(name = "period_label", length = 64)
    private String periodLabel;

    @Column(name = "stack_code", length = 32)
    private String stackCode;

    @Column(name = "alarm_time")
    private LocalDateTime alarmTime;

    /** INITIAL / MERGED / REBOUND */
    @Column(name = "attached_source", nullable = false, length = 32)
    private String attachedSource;

    @Column(name = "force_missing_data", nullable = false)
    private Boolean forceMissingData = false;

    @Column(name = "attached_at", nullable = false)
    private LocalDateTime attachedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
    }

    public String getAlarmNo() {
        return alarmNo;
    }

    public void setAlarmNo(String alarmNo) {
        this.alarmNo = alarmNo;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public BigDecimal getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(BigDecimal measuredValue) {
        this.measuredValue = measuredValue;
    }

    public BigDecimal getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(BigDecimal limitValue) {
        this.limitValue = limitValue;
    }

    public BigDecimal getExceedRatio() {
        return exceedRatio;
    }

    public void setExceedRatio(BigDecimal exceedRatio) {
        this.exceedRatio = exceedRatio;
    }

    public String getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(String alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public String getStackCode() {
        return stackCode;
    }

    public void setStackCode(String stackCode) {
        this.stackCode = stackCode;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getAttachedSource() {
        return attachedSource;
    }

    public void setAttachedSource(String attachedSource) {
        this.attachedSource = attachedSource;
    }

    public LocalDateTime getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(LocalDateTime attachedAt) {
        this.attachedAt = attachedAt;
    }

    public Boolean getForceMissingData() {
        return forceMissingData;
    }

    public void setForceMissingData(Boolean forceMissingData) {
        this.forceMissingData = forceMissingData;
    }
}
