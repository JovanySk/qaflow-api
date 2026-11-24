package com.qaflow.material.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.auth.application.ActorSummaryResolver;
import com.qaflow.material.api.MaterialResponse;
import com.qaflow.material.domain.Material;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    private final ActorSummaryResolver actorSummaryResolver;

    public MaterialMapper(ActorSummaryResolver actorSummaryResolver) {
        this.actorSummaryResolver = actorSummaryResolver;
    }

    public MaterialResponse toResponse(Material material) {
        return toResponses(List.of(material)).get(0);
    }

    public List<MaterialResponse> toResponses(List<Material> materials) {
        HashSet<Long> actorIds = new HashSet<>();
        materials.forEach(material -> {
            actorIds.add(material.createdBy());
            actorIds.add(material.updatedBy());
        });
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        List<MaterialResponse> responses = new ArrayList<>(materials.size());
        for (Material material : materials) {
            responses.add(new MaterialResponse(
                    Long.toString(material.id()),
                    material.code(),
                    material.name(),
                    material.description(),
                    material.unitOfMeasure(),
                    material.active(),
                    Long.toString(material.version()),
                    material.createdAt(),
                    actors.get(material.createdBy()),
                    material.updatedAt(),
                    actors.get(material.updatedBy())));
        }
        return List.copyOf(responses);
    }
}
