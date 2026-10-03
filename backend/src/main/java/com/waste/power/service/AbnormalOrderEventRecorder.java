package com.waste.power.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.domain.AbnormalOrderEvent;
import com.waste.power.repository.AbnormalOrderEventRepository;

/**
 * 独立事务记录操作轨迹，保证主业务回滚或复核失败时，失败原因仍然可追溯。
 */
@Service
public class AbnormalOrderEventRecorder {

    private final AbnormalOrderEventRepository eventRepository;

    public AbnormalOrderEventRecorder(AbnormalOrderEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long orderId, String type, String fromStatus, String toStatus, String operator,
            String detail, boolean success, String failureReason) {
        AbnormalOrderEvent event = new AbnormalOrderEvent();
        event.setOrderId(orderId);
        event.setEventType(type);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperator(trim(operator, 32));
        event.setDetail(trim(detail, 1024));
        event.setSuccess(success);
        event.setFailureReason(trim(failureReason, 1024));
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
