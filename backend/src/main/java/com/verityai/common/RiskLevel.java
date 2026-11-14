package com.verityai.common;

public enum RiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    public static RiskLevel fromText(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
