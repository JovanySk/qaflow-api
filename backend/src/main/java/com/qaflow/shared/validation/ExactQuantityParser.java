package com.qaflow.shared.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

import com.qaflow.shared.error.BadRequestException;

public final class ExactQuantityParser {

    public static final String POSITIVE_PATTERN =
            "^(?!0+(?:\\.0+)?$)[0-9]{1,13}(?:\\.[0-9]{1,6})?$";
    private static final Pattern POSITIVE_QUANTITY = Pattern.compile(POSITIVE_PATTERN);

    private ExactQuantityParser() {
    }

    public static BigDecimal parsePositive(String value, String field) {
        if (value == null || !POSITIVE_QUANTITY.matcher(value.trim()).matches()) {
            throw new BadRequestException(
                    "VALIDATION_ERROR",
                    field + " debe ser una cantidad positiva con hasta 13 enteros y 6 decimales.");
        }
        return new BigDecimal(value.trim()).setScale(6, RoundingMode.UNNECESSARY);
    }
}
