package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.AbnormalOrderAlarm;

public interface AbnormalOrderAlarmRepository extends JpaRepository<AbnormalOrderAlarm, Long> {

    List<AbnormalOrderAlarm> findByOrderIdOrderByAlarmTimeAsc(Long orderId);

    Optional<AbnormalOrderAlarm> findByOrderIdAndAlarmId(Long orderId, Long alarmId);
}
