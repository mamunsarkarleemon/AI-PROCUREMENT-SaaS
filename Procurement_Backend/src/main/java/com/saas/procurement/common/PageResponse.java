package com.saas.procurement.common;

import java.util.List;

import org.springframework.data.domain.Page;

/** One page of a list. page starts at 0. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> p) {
		return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
	}

}
