package com.qaflow.material.api;

import com.qaflow.material.domain.Material;
import com.qaflow.material.domain.UnitOfMeasure;

public record MaterialSummaryResponse(
        String id,
        String code,
        String name,
        UnitOfMeasure unitOfMeasure) {

    public static MaterialSummaryResponse from(Material material) {
        return new MaterialSummaryResponse(
                Long.toString(material.id()),
                material.code(),
                material.name(),
                material.unitOfMeasure());
    }
}
