package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalGrabSnapshot;

public interface AbnormalGrabSnapshotRepository extends JpaRepository<AbnormalGrabSnapshot, Long> {

    List<AbnormalGrabSnapshot> findByOrderIdOrderByPitNoAsc(Long orderId);
}
