package com.qaflow.bom.application;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.auth.application.ActorSummaryResolver;
import com.qaflow.bom.api.BomItemResponse;
import com.qaflow.bom.api.BomProductSummaryResponse;
import com.qaflow.bom.api.BomResponse;
import com.qaflow.bom.api.BomSummaryResponse;
import com.qaflow.bom.domain.Bom;
import com.qaflow.bom.domain.BomItem;
import org.springframework.stereotype.Component;

@Component
public class BomMapper {

    private final ActorSummaryResolver actorSummaryResolver;

    public BomMapper(ActorSummaryResolver actorSummaryResolver) {
        this.actorSummaryResolver = actorSummaryResolver;
    }

    public BomResponse toResponse(Bom bom) {
        HashSet<Long> actorIds = new HashSet<>();
        actorIds.add(bom.createdBy());
        if (bom.supersededBy() != null) {
            actorIds.add(bom.supersededBy());
        }
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        return new BomResponse(
                Long.toString(bom.id()),
                new BomProductSummaryResponse(
                        Long.toString(bom.product().id()),
                        bom.product().sku(),
                        bom.product().name()),
                bom.revisionNumber(),
                bom.current(),
                bom.items().stream().map(this::toItemResponse).toList(),
                bom.createdAt(),
                actors.get(bom.createdBy()),
                bom.supersededAt(),
                bom.supersededBy() == null ? null : actors.get(bom.supersededBy()));
    }

    public List<BomSummaryResponse> toSummaryResponses(List<Bom> boms, Map<Long, Long> itemCounts) {
        HashSet<Long> actorIds = new HashSet<>();
        boms.forEach(bom -> actorIds.add(bom.createdBy()));
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        return boms.stream()
                .map(bom -> new BomSummaryResponse(
                        Long.toString(bom.id()),
                        bom.revisionNumber(),
                        bom.current(),
                        itemCounts.getOrDefault(bom.id(), 0L),
                        bom.createdAt(),
                        actors.get(bom.createdBy())))
                .toList();
    }

    private BomItemResponse toItemResponse(BomItem item) {
        return new BomItemResponse(
                Long.toString(item.material().id()),
                item.material().code(),
                item.material().name(),
                item.material().unitOfMeasure(),
                item.quantityPerUnit().toPlainString());
    }
}
