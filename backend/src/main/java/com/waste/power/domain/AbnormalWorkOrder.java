package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 异常工况处置单。同一炉在恢复/关闭前由 active_flag 唯一约束保证只有一张有效单。
 */
@Entity
@Table(name = "abnormal_work_order")
public class AbnormalWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(name = "furnace_code", nullable = false, length = 32)
    private String furnaceCode;

    /** PENDING / PROCESSING / REVIEWING / RECOVERED / CLOSED */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /** AUTO_COMBUSTION / AUTO_EMISSION / MANUAL */
    @Column(name = "trigger_source", nullable = false, length = 16)
    private String triggerSource;

    @Column(name = "trigger_reason", nullable = false, length = 512)
    private String triggerReason;

    @Column(name = "trigger_detail_json", columnDefinition = "TEXT")
    private String triggerDetailJson;

    @Column(name = "feeding_locked", nullable = false)
    private Boolean feedingLocked;

    @Column(name = "freeze_reason", length = 256)
    private String freezeReason;

    @Column(name = "owner_name", length = 32)
    private String ownerName;

    @Column(name = "measures", length = 1024)
    private String measures;

    @Column(name = "review_request_note", length = 512)
    private String reviewRequestNote;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "reviewer_name", length = 32)
    private String reviewerName;

    @Column(name = "review_note", length = 512)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** WAIT / CLOSE_KEEP_FROZEN */
    @Column(name = "data_missing_choice", length = 32)
    private String dataMissingChoice;

    @Column(name = "data_missing_reason", length = 512)
    private String dataMissingReason;

    @Column(name = "close_note", length = 512)
    private String closeNote;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 32)
    private String closedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getFurnaceCode() {
        return furnaceCode;
    }

    public void setFurnaceCode(String furnaceCode) {
        this.furnaceCode = furnaceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public String getTriggerDetailJson() {
        return triggerDetailJson;
    }

    public void setTriggerDetailJson(String triggerDetailJson) {
        this.triggerDetailJson = triggerDetailJson;
    }

    public Boolean getFeedingLocked() {
        return feedingLocked;
    }

    public void setFeedingLocked(Boolean feedingLocked) {
        this.feedingLocked = feedingLocked;
    }

    public String getFreezeReason() {
        return freezeReason;
    }

    public void setFreezeReason(String freezeReason) {
        this.freezeReason = freezeReason;
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

    public String getReviewRequestNote() {
        return reviewRequestNote;
    }

    public void setReviewRequestNote(String reviewRequestNote) {
        this.reviewRequestNote = reviewRequestNote;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getDataMissingChoice() {
        return dataMissingChoice;
    }

    public void setDataMissingChoice(String dataMissingChoice) {
        this.dataMissingChoice = dataMissingChoice;
    }

    public String getDataMissingReason() {
        return dataMissingReason;
    }

    public void setDataMissingReason(String dataMissingReason) {
        this.dataMissingReason = dataMissingReason;
    }

    public String getCloseNote() {
        return closeNote;
    }

    public void setCloseNote(String closeNote) {
        this.closeNote = closeNote;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
