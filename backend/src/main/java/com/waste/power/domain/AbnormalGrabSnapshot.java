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
 * 处置触发时供料抓斗及所在储坑快照。
 */
@Entity
@Table(name = "abnormal_grab_snapshot")
public class AbnormalGrabSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "grab_no", nullable = false, length = 16)
    private String grabNo;

    @Column(name = "pit_no", length = 16)
    private String pitNo;

    @Column(name = "pit_name", length = 64)
    private String pitName;

    @Column(name = "current_stock_ton", precision = 10, scale = 2)
    private BigDecimal currentStockTon;

    @Column(name = "pit_status", length = 16)
    private String pitStatus;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

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

    public String getGrabNo() {
        return grabNo;
    }

    public void setGrabNo(String grabNo) {
        this.grabNo = grabNo;
    }

    public String getPitNo() {
        return pitNo;
    }

    public void setPitNo(String pitNo) {
        this.pitNo = pitNo;
    }

    public String getPitName() {
        return pitName;
    }

    public void setPitName(String pitName) {
        this.pitName = pitName;
    }

    public BigDecimal getCurrentStockTon() {
        return currentStockTon;
    }

    public void setCurrentStockTon(BigDecimal currentStockTon) {
        this.currentStockTon = currentStockTon;
    }

    public String getPitStatus() {
        return pitStatus;
    }

    public void setPitStatus(String pitStatus) {
        this.pitStatus = pitStatus;
    }

    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }
}
