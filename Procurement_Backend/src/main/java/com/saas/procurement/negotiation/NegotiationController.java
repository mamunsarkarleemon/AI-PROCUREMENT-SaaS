package com.saas.procurement.negotiation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.negotiation.NegotiationDtos.CounterForm;
import com.saas.procurement.negotiation.NegotiationDtos.NegotiationDto;
import com.saas.procurement.negotiation.NegotiationDtos.ReplyForm;
import com.saas.procurement.negotiation.NegotiationDtos.StartForm;
import com.saas.procurement.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/negotiations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MENU_negotiations') or hasRole('ADMIN')")
public class NegotiationController {

	private final NegotiationService service;

	@GetMapping
	public List<NegotiationDto> list(@AuthenticationPrincipal AuthUser me) {
		return service.list(me);
	}

	@GetMapping("/{id}")
	public NegotiationDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return service.get(me, id);
	}

	/** Buyer makes the first offer on a product. */
	@PreAuthorize("hasRole('BUYER') and hasAuthority('MENU_negotiations')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public NegotiationDto start(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StartForm form) {
		return service.start(me, form);
	}

	@PostMapping("/{id}/counter")
	public NegotiationDto counter(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody CounterForm form) {
		return service.counter(me, id, form);
	}

	@PostMapping("/{id}/accept")
	public NegotiationDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestBody(required = false) ReplyForm form) {
		return service.accept(me, id, form == null ? null : form.message());
	}

	@PostMapping("/{id}/reject")
	public NegotiationDto reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestBody(required = false) ReplyForm form) {
		return service.reject(me, id, form == null ? null : form.message());
	}

}
