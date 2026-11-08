package com.saas.procurement.order;

/** How the order was created. */
public enum OrderSource {
	/** "Buy now" at the listed price. */
	DIRECT,
	/** Agreed price after negotiation. */
	NEGOTIATION,
	/** Buyer accepted a supplier's quote on a buying request. */
	QUOTE
}
