package com.saas.procurement.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.order.OrderService.DirectOrderRequest;
import com.saas.procurement.order.OrderService.ReceiveRequest;
import com.saas.procurement.security.AuthUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService service;

	/** Buyer: my orders. Supplier: orders I received. Admin: all orders. Optional ?status=PENDING */
	@PreAuthorize("hasAuthority('MENU_orders')")
	@GetMapping
	public List<OrderDto> list(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) OrderStatus status) {
		return service.list(me, status);
	}

	@GetMapping("/{id}")
	public OrderDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return service.get(me, id);
	}

	/** Buyer: "Buy now" at the listed price. */
	@PreAuthorize("hasRole('BUYER')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderDto buyNow(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody DirectOrderRequest req) {
		return service.createDirect(me, req);
	}

	/** Buyer: receive a shipped order, in full or partly. */
	@PreAuthorize("hasRole('BUYER')")
	@PostMapping("/{id}/receive")
	public OrderDto receive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody ReceiveRequest body) {
		return service.receive(me, id, body);
	}

	/** Supplier or admin: the returned units came back, add them to stock. */
	@PreAuthorize("hasRole('SUPPLIER') or hasRole('ADMIN')")
	@PostMapping("/{id}/return/confirm")
	public OrderDto confirmReturn(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return service.confirmReturn(me, id);
	}

	public record StatusChange(@NotNull OrderStatus status) {
	}

	@PutMapping("/{id}/status")
	public OrderDto changeStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody StatusChange body) {
		return service.changeStatus(me, id, body.status());
	}

}
