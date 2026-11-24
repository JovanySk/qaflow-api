package com.qaflow.bom.api;

import com.qaflow.material.domain.UnitOfMeasure;

public record BomItemResponse(
        String materialId,
        String materialCode,
        String materialName,
        UnitOfMeasure unitOfMeasure,
        String quantityPerUnit) {
}
