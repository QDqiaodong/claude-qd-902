package com.waste.power.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalFurnaceSnapshot;

public interface AbnormalFurnaceSnapshotRepository extends JpaRepository<AbnormalFurnaceSnapshot, Long> {

    Optional<AbnormalFurnaceSnapshot> findByOrderId(Long orderId);
}
