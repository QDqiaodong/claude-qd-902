package com.waste.power.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalOrderEvent;

public interface AbnormalOrderEventRepository extends JpaRepository<AbnormalOrderEvent, Long> {

    List<AbnormalOrderEvent> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
