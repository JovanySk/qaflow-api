package com.qaflow.inventory.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qaflow.shared.error.BadRequestException;
import org.springframework.data.domain.Sort;

final class InventoryMovementSort {

    private static final Pattern FORMAT = Pattern.compile("^(occurredAt),(asc|desc)$");

    private InventoryMovementSort() {
    }

    static Sort parse(String value) {
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new BadRequestException(
                    "INVALID_QUERY_PARAMETER",
                    "sort debe usar occurredAt y dirección asc o desc.");
        }
        Sort.Direction direction = "asc".equals(matcher.group(2))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, "occurredAt").and(Sort.by(direction, "id"));
    }
}
