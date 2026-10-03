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
 * 处置单创建时固化的炉况快照；后续炉况变化不会改写该证据。
 */
@Entity
@Table(name = "abnormal_furnace_snapshot")
public class AbnormalFurnaceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "furnace_code", nullable = false, length = 32)
    private String furnaceCode;

    @Column(name = "furnace_name", length = 64)
    private String furnaceName;

    @Column(name = "furnace_type_code", length = 16)
    private String furnaceTypeCode;

    @Column(name = "furnace_temp_c", precision = 8, scale = 1)
    private BigDecimal furnaceTempC;

    @Column(name = "flue_gas_residence_s", precision = 6, scale = 2)
    private BigDecimal flueGasResidenceS;

    @Column(name = "co_ppm", precision = 8, scale = 2)
    private BigDecimal coPpm;

    @Column(name = "combustion_status", length = 16)
    private String combustionStatus;

    @Column(name = "daily_process_ton", precision = 10, scale = 2)
    private BigDecimal dailyProcessTon;

    @Column(name = "grab_no", length = 16)
    private String grabNo;

    @Column(name = "qualified", nullable = false)
    private Boolean qualified;

    @Column(name = "unqualified_items_json", length = 512)
    private String unqualifiedItemsJson;

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

    public String getFurnaceCode() {
        return furnaceCode;
    }

    public void setFurnaceCode(String furnaceCode) {
        this.furnaceCode = furnaceCode;
    }

    public String getFurnaceName() {
        return furnaceName;
    }

    public void setFurnaceName(String furnaceName) {
        this.furnaceName = furnaceName;
    }

    public String getFurnaceTypeCode() {
        return furnaceTypeCode;
    }

    public void setFurnaceTypeCode(String furnaceTypeCode) {
        this.furnaceTypeCode = furnaceTypeCode;
    }

    public BigDecimal getFurnaceTempC() {
        return furnaceTempC;
    }

    public void setFurnaceTempC(BigDecimal furnaceTempC) {
        this.furnaceTempC = furnaceTempC;
    }

    public BigDecimal getFlueGasResidenceS() {
        return flueGasResidenceS;
    }

    public void setFlueGasResidenceS(BigDecimal flueGasResidenceS) {
        this.flueGasResidenceS = flueGasResidenceS;
    }

    public BigDecimal getCoPpm() {
        return coPpm;
    }

    public void setCoPpm(BigDecimal coPpm) {
        this.coPpm = coPpm;
    }

    public String getCombustionStatus() {
        return combustionStatus;
    }

    public void setCombustionStatus(String combustionStatus) {
        this.combustionStatus = combustionStatus;
    }

    public BigDecimal getDailyProcessTon() {
        return dailyProcessTon;
    }

    public void setDailyProcessTon(BigDecimal dailyProcessTon) {
        this.dailyProcessTon = dailyProcessTon;
    }

    public String getGrabNo() {
        return grabNo;
    }

    public void setGrabNo(String grabNo) {
        this.grabNo = grabNo;
    }

    public Boolean getQualified() {
        return qualified;
    }

    public void setQualified(Boolean qualified) {
        this.qualified = qualified;
    }

    public String getUnqualifiedItemsJson() {
        return unqualifiedItemsJson;
    }

    public void setUnqualifiedItemsJson(String unqualifiedItemsJson) {
        this.unqualifiedItemsJson = unqualifiedItemsJson;
    }

    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }
}
