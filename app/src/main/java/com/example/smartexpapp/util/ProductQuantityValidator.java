package com.example.smartexpapp.util;

import java.math.BigDecimal;

public final class ProductQuantityValidator {
    private static final String DECIMAL_PATTERN = "(?:\\d+(?:\\.\\d+)?|\\.\\d+)";

    private ProductQuantityValidator() {
    }

    public static String normalize(String input) {
        String value = input == null ? "" : input.trim();
        if (!value.matches(DECIMAL_PATTERN)) {
            return null;
        }

        BigDecimal quantity;
        try {
            quantity = new BigDecimal(value);
        } catch (NumberFormatException error) {
            return null;
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return quantity.stripTrailingZeros().toPlainString();
    }
}
