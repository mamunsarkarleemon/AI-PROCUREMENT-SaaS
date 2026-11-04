package com.saas.procurement.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder role-protected endpoints (blueprint Day 11: "wrong role returns 403").
 * They will grow into the real buyer and supplier dashboard APIs.
 */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {

	public record DashboardResponse(String area, String message) {
	}

	@PreAuthorize("hasRole('BUYER')")
	@GetMapping("/buyer/dashboard")
	public DashboardResponse buyer() {
		return new DashboardResponse("BUYER", "Welcome to the buyer workspace.");
	}

	@PreAuthorize("hasRole('SUPPLIER')")
	@GetMapping("/supplier/dashboard")
	public DashboardResponse supplier() {
		return new DashboardResponse("SUPPLIER", "Welcome to the supplier workspace.");
	}

}
