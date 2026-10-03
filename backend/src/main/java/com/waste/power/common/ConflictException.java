package com.waste.power.common;

/**
 * 并发冲突：版本过期、两人同时复核、恢复期间状态已被新告警改变等。
 */
public class ConflictException extends BizException {

    public ConflictException(String message) {
        super(message);
    }
}
