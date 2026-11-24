package com.qaflow.product.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.auth.application.ActorSummaryResolver;
import com.qaflow.product.api.ProductResponse;
import com.qaflow.product.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private final ActorSummaryResolver actorSummaryResolver;

    public ProductMapper(ActorSummaryResolver actorSummaryResolver) {
        this.actorSummaryResolver = actorSummaryResolver;
    }

    public ProductResponse toResponse(Product product) {
        return toResponses(List.of(product)).get(0);
    }

    public List<ProductResponse> toResponses(List<Product> products) {
        HashSet<Long> actorIds = new HashSet<>();
        products.forEach(product -> {
            actorIds.add(product.createdBy());
            actorIds.add(product.updatedBy());
        });
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        List<ProductResponse> responses = new ArrayList<>(products.size());
        for (Product product : products) {
            responses.add(new ProductResponse(
                    Long.toString(product.id()),
                    product.sku(),
                    product.name(),
                    product.description(),
                    product.active(),
                    Long.toString(product.version()),
                    product.createdAt(),
                    actors.get(product.createdBy()),
                    product.updatedAt(),
                    actors.get(product.updatedBy())));
        }
        return List.copyOf(responses);
    }
}
