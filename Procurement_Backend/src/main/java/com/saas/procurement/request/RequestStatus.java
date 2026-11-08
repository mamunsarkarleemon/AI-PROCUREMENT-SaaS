package com.saas.procurement.request;

public enum RequestStatus {
	/** Suppliers can send quotes. */
	OPEN,
	/** A quote was accepted or the buyer closed it. */
	CLOSED
}
