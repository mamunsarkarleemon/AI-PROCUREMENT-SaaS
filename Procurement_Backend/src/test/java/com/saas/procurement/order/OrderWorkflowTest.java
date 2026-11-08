package com.saas.procurement.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.saas.procurement.user.Role;

class OrderWorkflowTest {

	@Test
	void supplierConfirmsShipsOrCancels() {
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.PENDING, Role.SUPPLIER))
				.containsExactly(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.CONFIRMED, Role.SUPPLIER))
				.containsExactly(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.SHIPPED, Role.SUPPLIER)).isEmpty();
	}

	@Test
	void buyerCanCancelPendingAndConfirmDelivery() {
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.PENDING, Role.BUYER)).containsExactly(OrderStatus.CANCELLED);
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.SHIPPED, Role.BUYER)).containsExactly(OrderStatus.DELIVERED);
		assertThat(OrderWorkflow.canMove(OrderStatus.PENDING, OrderStatus.CONFIRMED, Role.BUYER)).isFalse();
		assertThat(OrderWorkflow.canMove(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, Role.BUYER)).isFalse();
	}

	@Test
	void adminCanSetAnyOtherStatus() {
		assertThat(OrderWorkflow.nextStatuses(OrderStatus.DELIVERED, Role.ADMIN))
				.hasSize(OrderStatus.values().length - 1)
				.doesNotContain(OrderStatus.DELIVERED);
	}

	@Test
	void onlyConfirmedShippedDeliveredHoldStock() {
		assertThat(OrderStatus.PENDING.holdsStock()).isFalse();
		assertThat(OrderStatus.CONFIRMED.holdsStock()).isTrue();
		assertThat(OrderStatus.SHIPPED.holdsStock()).isTrue();
		assertThat(OrderStatus.DELIVERED.holdsStock()).isTrue();
		assertThat(OrderStatus.CANCELLED.holdsStock()).isFalse();
	}

}
