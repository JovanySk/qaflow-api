package com.qaflow.bom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import com.qaflow.material.domain.Material;
import com.qaflow.product.domain.Product;
import org.junit.jupiter.api.Test;

class BomTest {

    private static final Instant NOW = Instant.parse("2026-07-20T05:00:00Z");

    @Test
    void createsExactImmutableRevisionAndSupersedesItOnce() {
        Product product = mock(Product.class);
        Material material = mock(Material.class);
        when(material.id()).thenReturn(9L);
        Bom bom = Bom.create(product, 1, NOW, 1L);

        bom.addItem(material, new BigDecimal("2.500000"), NOW);
        bom.supersede(NOW.plusSeconds(60), 2L);

        assertThat(bom.revisionNumber()).isEqualTo(1);
        assertThat(bom.current()).isFalse();
        assertThat(bom.items()).singleElement()
                .extracting(BomItem::quantityPerUnit)
                .isEqualTo(new BigDecimal("2.500000"));
        assertThat(bom.supersededBy()).isEqualTo(2L);
        assertThatThrownBy(() -> bom.supersede(NOW.plusSeconds(120), 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsDuplicateMaterialAndNonDatabaseScale() {
        Product product = mock(Product.class);
        Material material = mock(Material.class);
        when(material.id()).thenReturn(9L);
        Bom bom = Bom.create(product, 1, NOW, 1L);
        bom.addItem(material, new BigDecimal("1.000000"), NOW);

        assertThatThrownBy(() -> bom.addItem(material, new BigDecimal("2.000000"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        Material secondMaterial = mock(Material.class);
        when(secondMaterial.id()).thenReturn(10L);
        assertThatThrownBy(() -> bom.addItem(secondMaterial, new BigDecimal("1.1"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
