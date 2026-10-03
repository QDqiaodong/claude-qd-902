package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.waste.power.domain.AbnormalWorkOrder;

import jakarta.persistence.LockModeType;

/**
 * 异常工况处置单数据访问。
 */
public interface AbnormalWorkOrderRepository extends JpaRepository<AbnormalWorkOrder, Long> {

    /** 同炉有效单：PENDING/PROCESSING/REVIEWING/RECOVERED。调用方需先持有焚烧炉行锁。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AbnormalWorkOrder o where o.furnaceCode = :furnaceCode "
            + "and o.status in ('PENDING','PROCESSING','REVIEWING','RECOVERED')")
    List<AbnormalWorkOrder> findActiveByFurnaceCodeForUpdate(@Param("furnaceCode") String furnaceCode);

    @Query("select o from AbnormalWorkOrder o where o.furnaceCode = :furnaceCode "
            + "and o.status in ('PENDING','PROCESSING','REVIEWING','RECOVERED') "
            + "order by o.createdAt desc")
    List<AbnormalWorkOrder> findActiveByFurnaceCode(@Param("furnaceCode") String furnaceCode);

    @Query("select o from AbnormalWorkOrder o where o.furnaceCode = :furnaceCode and o.status = 'CLOSED' "
            + "and o.feedingLocked = true order by o.updatedAt desc")
    List<AbnormalWorkOrder> findClosedFrozenByFurnaceCode(@Param("furnaceCode") String furnaceCode);

    Optional<AbnormalWorkOrder> findByOrderNo(String orderNo);

    List<AbnormalWorkOrder> findTop100ByOrderByCreatedAtDesc();

    List<AbnormalWorkOrder> findTop100ByStatusOrderByCreatedAtDesc(String status);
}
