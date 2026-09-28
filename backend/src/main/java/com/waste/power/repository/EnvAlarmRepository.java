package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.EnvAlarm;

/**
 * 环保告警数据访问。
 */
public interface EnvAlarmRepository extends JpaRepository<EnvAlarm, Long> {

    List<EnvAlarm> findTop30ByOrderByAlarmTimeDesc();

    List<EnvAlarm> findTop20ByHandledFalseOrderByAlarmTimeDesc();

    Optional<EnvAlarm> findByAlarmNo(String alarmNo);
}
