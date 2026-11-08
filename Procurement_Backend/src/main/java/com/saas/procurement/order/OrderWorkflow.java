package com.saas.procurement.order;

import java.util.Arrays;
import java.util.List;

import com.saas.procurement.user.Role;

/**
 * Who can move an order to which status.
 *
 * <pre>
 *   PENDING ──supplier──> CONFIRMED ──supplier──> SHIPPED ──buyer──> DELIVERED
 *      │                     │
 *      └─buyer/supplier──> CANCELLED <──supplier──┘
 * </pre>
 * Admin can set any status.
 */
public final class OrderWorkflow {

	private OrderWorkflow() {
	}

	public static List<OrderStatus> nextStatuses(OrderStatus current, Role role) {
		return switch (role) {
			case ADMIN -> Arrays.stream(OrderStatus.values()).filter(s -> s != current).toList();
			case BUYER -> switch (current) {
				case PENDING -> List.of(OrderStatus.CANCELLED);
				case SHIPPED -> List.of(OrderStatus.DELIVERED);
				default -> List.of();
			};
			case SUPPLIER -> switch (current) {
				case PENDING -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
				case CONFIRMED -> List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
				default -> List.of();
			};
		};
	}

	public static boolean canMove(OrderStatus from, OrderStatus to, Role role) {
		return nextStatuses(from, role).contains(to);
	}

}
