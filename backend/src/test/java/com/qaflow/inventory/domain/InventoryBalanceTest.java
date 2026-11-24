package com.qaflow.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InventoryBalanceTest {

    private InventoryBalance balance;

    @BeforeEach
    void setUp() {
        balance = new InventoryBalance();
        ReflectionTestUtils.setField(balance, "onHandQuantity", new BigDecimal("10.000000"));
        ReflectionTestUtils.setField(balance, "reservedQuantity", new BigDecimal("2.000000"));
        ReflectionTestUtils.setField(balance, "updatedAt", Instant.parse("2026-07-20T08:00:00Z"));
        ReflectionTestUtils.setField(balance, "updatedBy", 1L);
    }

    @Test
    void incomingMovementIncreasesPhysicalAndAvailableQuantities() {
        balance.apply(
                InventoryMovementType.ENTRY,
                new BigDecimal("3.500000"),
                Instant.parse("2026-07-20T09:00:00Z"),
                2L);

        assertThat(balance.onHandQuantity()).isEqualByComparingTo("13.500000");
        assertThat(balance.reservedQuantity()).isEqualByComparingTo("2.000000");
        assertThat(balance.availableQuantity()).isEqualByComparingTo("11.500000");
        assertThat(balance.updatedBy()).isEqualTo(2L);
    }

    @Test
    void outgoingMovementCanConsumeExactlyAllAvailableStock() {
        balance.apply(
                InventoryMovementType.MANUAL_OUT,
                new BigDecimal("8.000000"),
                Instant.parse("2026-07-20T09:00:00Z"),
                2L);

        assertThat(balance.onHandQuantity()).isEqualByComparingTo("2.000000");
        assertThat(balance.availableQuantity()).isZero();
    }

    @Test
    void outgoingMovementCannotConsumeReservedOrLeaveNegativeAvailableStock() {
        assertThatThrownBy(() -> balance.apply(
                InventoryMovementType.ADJUSTMENT_OUT,
                new BigDecimal("8.000001"),
                Instant.parse("2026-07-20T09:00:00Z"),
                2L))
                .isInstanceOf(InventoryBalance.InsufficientStockException.class);

        assertThat(balance.onHandQuantity()).isEqualByComparingTo("10.000000");
        assertThat(balance.availableQuantity()).isEqualByComparingTo("8.000000");
    }

    @Test
    void incomingMovementCannotOverflowDecimalCapacity() {
        ReflectionTestUtils.setField(balance, "onHandQuantity", InventoryBalance.MAX_QUANTITY);
        ReflectionTestUtils.setField(balance, "reservedQuantity", BigDecimal.ZERO.setScale(6));

        assertThatThrownBy(() -> balance.apply(
                InventoryMovementType.ADJUSTMENT_IN,
                new BigDecimal("0.000001"),
                Instant.parse("2026-07-20T09:00:00Z"),
                2L))
                .isInstanceOf(InventoryBalance.QuantityOverflowException.class);
        assertThat(balance.onHandQuantity()).isEqualByComparingTo(InventoryBalance.MAX_QUANTITY);
    }
}
