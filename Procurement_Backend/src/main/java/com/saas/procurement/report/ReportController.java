package com.saas.procurement.report;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.report.DashboardService.Dashboard;
import com.saas.procurement.report.ReportService.Report;
import com.saas.procurement.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;
	private final DashboardService dashboardService;

	@GetMapping("/dashboard")
	public Dashboard dashboard(@AuthenticationPrincipal AuthUser me) {
		return dashboardService.forUser(me);
	}

	/**
	 * Example: /api/v1/reports?from=2026-10-01&to=2026-10-31
	 * Admin can add &supplierId=3 and/or &buyerId=5
	 */
	@PreAuthorize("hasAuthority('MENU_reports')")
	@GetMapping("/reports")
	public Report report(@AuthenticationPrincipal AuthUser me,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long supplierId, @RequestParam(required = false) Long buyerId) {
		return reportService.report(me, from, to, supplierId, buyerId);
	}

}
