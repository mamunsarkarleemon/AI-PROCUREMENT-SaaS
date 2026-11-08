package com.saas.procurement.request;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.saas.procurement.order.OrderDto;
import com.saas.procurement.request.RequestDtos.BuyRequestDto;
import com.saas.procurement.request.RequestDtos.BuyRequestForm;
import com.saas.procurement.request.RequestDtos.QuoteDto;
import com.saas.procurement.request.RequestDtos.QuoteForm;
import com.saas.procurement.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BuyRequestController {

	private final BuyRequestService requestService;
	private final QuoteService quoteService;

	// ---------- Buying requests ----------

	/** Open requests (marketplace tab). Optional ?q=&categoryId= */
	@PreAuthorize("hasAuthority('MENU_marketplace') or hasRole('ADMIN')")
	@GetMapping("/requests")
	public List<BuyRequestDto> open(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId) {
		return requestService.open(me, q, categoryId);
	}

	@PreAuthorize("hasRole('BUYER') and hasAuthority('MENU_my_requests')")
	@GetMapping("/requests/mine")
	public List<BuyRequestDto> mine(@AuthenticationPrincipal AuthUser me) {
		return requestService.mine(me);
	}

	@GetMapping("/requests/{id}")
	public BuyRequestDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return requestService.get(me, id);
	}

	@PreAuthorize("hasRole('BUYER') and hasAuthority('MENU_my_requests')")
	@PostMapping("/requests")
	@ResponseStatus(HttpStatus.CREATED)
	public BuyRequestDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BuyRequestForm form) {
		return requestService.create(me, form);
	}

	@PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
	@PutMapping("/requests/{id}")
	public BuyRequestDto update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody BuyRequestForm form) {
		return requestService.update(me, id, form);
	}

	@PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
	@PostMapping("/requests/{id}/close")
	public BuyRequestDto close(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return requestService.close(me, id);
	}

	@PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
	@DeleteMapping("/requests/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		requestService.delete(me, id);
	}

	@PreAuthorize("hasRole('BUYER')")
	@PostMapping(path = "/requests/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public BuyRequestDto upload(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestPart("files") List<MultipartFile> files) {
		return requestService.addMedia(me, id, files);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/requests")
	public List<BuyRequestDto> all(@AuthenticationPrincipal AuthUser me) {
		return requestService.all(me);
	}

	// ---------- Quotes ----------

	/** Supplier sends (or updates) a quote for a request. */
	@PreAuthorize("hasRole('SUPPLIER')")
	@PostMapping("/requests/{id}/quotes")
	@ResponseStatus(HttpStatus.CREATED)
	public QuoteDto submitQuote(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody QuoteForm form) {
		return quoteService.submit(me, id, form);
	}

	@PreAuthorize("hasRole('SUPPLIER') and hasAuthority('MENU_my_quotes')")
	@GetMapping("/quotes/mine")
	public List<QuoteDto> myQuotes(@AuthenticationPrincipal AuthUser me) {
		return quoteService.mine(me);
	}

	@PreAuthorize("hasRole('SUPPLIER')")
	@PostMapping(path = "/quotes/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public QuoteDto uploadQuoteMedia(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestPart("files") List<MultipartFile> files) {
		return quoteService.addMedia(me, id, files);
	}

	/** Buyer accepts a quote -> an order is created. */
	@PreAuthorize("hasRole('BUYER')")
	@PostMapping("/quotes/{id}/accept")
	public OrderDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return quoteService.accept(me, id);
	}

	@PreAuthorize("hasRole('BUYER')")
	@PostMapping("/quotes/{id}/reject")
	public QuoteDto reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return quoteService.reject(me, id);
	}

	@PreAuthorize("hasRole('SUPPLIER')")
	@PostMapping("/quotes/{id}/withdraw")
	public QuoteDto withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return quoteService.withdraw(me, id);
	}

}
