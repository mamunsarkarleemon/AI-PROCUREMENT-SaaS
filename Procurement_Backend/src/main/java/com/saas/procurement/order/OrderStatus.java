package com.saas.procurement.order;

public enum OrderStatus {
	/** Placed by the buyer, waiting for the supplier. */
	PENDING,
	/** Supplier accepted it; stock is reserved. */
	CONFIRMED,
	SHIPPED,
	/** Buyer received the goods. */
	DELIVERED,
	CANCELLED;

	/** Stock is taken from the product while the order is in one of these states. */
	public boolean holdsStock() {
		return this == CONFIRMED || this == SHIPPED || this == DELIVERED;
	}
}
