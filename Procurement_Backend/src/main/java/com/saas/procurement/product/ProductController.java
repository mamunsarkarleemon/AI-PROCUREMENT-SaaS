package com.saas.procurement.product;

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

import com.saas.procurement.common.PageResponse;
import com.saas.procurement.product.ProductService.Highlights;
import com.saas.procurement.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService service;

	/** Marketplace list (buyers and suppliers). Optional ?q=box&categoryId=1 */
	@PreAuthorize("hasAuthority('MENU_marketplace') or hasRole('ADMIN')")
	@GetMapping("/products")
	public List<ProductDto> marketplace(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId) {
		return service.marketplace(q, categoryId);
	}

	/** Marketplace, one page at a time. page starts at 0. sort: newest, rating, selling, price_asc, price_desc */
	@PreAuthorize("hasAuthority('MENU_marketplace') or hasRole('ADMIN')")
	@GetMapping("/products/page")
	public PageResponse<ProductDto> marketplacePage(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String sort,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
		return service.marketplacePage(q, categoryId, sort, page, size);
	}

	/** Top rated and top selling products. */
	@PreAuthorize("hasAuthority('MENU_marketplace') or hasRole('ADMIN')")
	@GetMapping("/products/highlights")
	public Highlights highlights() {
		return service.highlights();
	}

	@GetMapping("/products/{id}")
	public ProductDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		return service.get(id, me);
	}

	// ---------- Supplier: my products ----------

	@PreAuthorize("hasRole('SUPPLIER') and hasAuthority('MENU_my_products')")
	@GetMapping("/products/mine")
	public List<ProductDto> mine(@AuthenticationPrincipal AuthUser me) {
		return service.mine(me);
	}

	@PreAuthorize("hasRole('SUPPLIER') and hasAuthority('MENU_my_products')")
	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProductRequest req) {
		return service.create(me, req);
	}

	@PreAuthorize("hasRole('SUPPLIER') or hasRole('ADMIN')")
	@PutMapping("/products/{id}")
	public ProductDto update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody ProductRequest req) {
		return service.update(me, id, req);
	}

	public record StatusChange(ProductStatus status) {
	}

	@PreAuthorize("hasRole('SUPPLIER') or hasRole('ADMIN')")
	@PutMapping("/products/{id}/status")
	public ProductDto setStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestBody StatusChange body) {
		return service.setStatus(me, id, body.status());
	}

	@PreAuthorize("hasRole('SUPPLIER') or hasRole('ADMIN')")
	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		service.delete(me, id);
	}

	/** Upload photos / short videos (form field name: files). */
	@PreAuthorize("hasRole('SUPPLIER') or hasRole('ADMIN')")
	@PostMapping(path = "/products/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductDto upload(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@RequestPart("files") List<MultipartFile> files) {
		return service.addMedia(me, id, files);
	}

	// ---------- Admin ----------

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/products")
	public List<ProductDto> all() {
		return service.all();
	}

}
