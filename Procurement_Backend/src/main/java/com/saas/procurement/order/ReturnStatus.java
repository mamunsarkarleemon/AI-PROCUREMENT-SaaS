package com.saas.procurement.order;

/** Units the buyer refused on delivery and sent back to the supplier. */
public enum ReturnStatus {
	/** Nothing was sent back. */
	NONE,
	/** Waiting for the supplier to confirm the units came back. */
	PENDING,
	/** Supplier confirmed; the units are back in stock. */
	CONFIRMED
}
