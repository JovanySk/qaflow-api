package com.qaflow.product.application;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qaflow.shared.error.BadRequestException;
import org.springframework.data.domain.Sort;

final class ProductSort {

    private static final Pattern FORMAT = Pattern.compile("^([A-Za-z][A-Za-z0-9]*),(asc|desc)$");
    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "sku", "sku",
            "name", "name",
            "createdAt", "createdAt");

    private ProductSort() {
    }

    static Sort parse(String value) {
        Matcher matcher = FORMAT.matcher(value);
        if (!matcher.matches() || !ALLOWED_FIELDS.containsKey(matcher.group(1))) {
            throw new BadRequestException(
                    "INVALID_QUERY_PARAMETER",
                    "sort debe usar sku, name o createdAt y dirección asc o desc.");
        }
        Sort.Direction direction = "asc".equals(matcher.group(2))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, ALLOWED_FIELDS.get(matcher.group(1)));
    }
}
