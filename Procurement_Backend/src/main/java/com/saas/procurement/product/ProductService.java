package com.saas.procurement.product;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saas.procurement.category.CategoryRepository;
import com.saas.procurement.common.ApiException;
import com.saas.procurement.common.Money;
import com.saas.procurement.media.MediaFile;
import com.saas.procurement.media.MediaService;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository repository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final MediaService mediaService;

	/** Marketplace: active products of active suppliers. */
	@Transactional(readOnly = true)
	public List<ProductDto> marketplace(String q, Long categoryId) {
		return repository.search(ProductStatus.ACTIVE, UserStatus.ACTIVE, categoryId, likePattern(q)).stream()
				.map(ProductDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductDto> latest(int limit) {
		return repository.findByStatusAndSupplierStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE, UserStatus.ACTIVE,
				Limit.of(limit)).stream().map(ProductDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductDto> mine(AuthUser me) {
		return repository.findBySupplierIdOrderByCreatedAtDesc(me.id()).stream().map(ProductDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductDto> all() {
		return repository.findAllByOrderByCreatedAtDesc().stream().map(ProductDto::from).toList();
	}

	@Transactional(readOnly = true)
	public ProductDto get(Long id, AuthUser me) {
		Product p = find(id);
		boolean owner = p.getSupplier().getId().equals(me.id());
		if (p.getStatus() != ProductStatus.ACTIVE && !owner && !me.isAdmin()) {
			throw ApiException.notFound("Product");
		}
		return ProductDto.from(p);
	}

	@Transactional
	public ProductDto create(AuthUser me, ProductRequest req) {
		Product p = new Product();
		p.setSupplier(userRepository.getReferenceById(me.id()));
		apply(p, req);
		return ProductDto.from(repository.save(p));
	}

	@Transactional
	public ProductDto update(AuthUser me, Long id, ProductRequest req) {
		Product p = findEditable(id, me);
		apply(p, req);
		return ProductDto.from(p);
	}

	@Transactional
	public ProductDto setStatus(AuthUser me, Long id, ProductStatus status) {
		Product p = findEditable(id, me);
		p.setStatus(status);
		return ProductDto.from(p);
	}

	@Transactional
	public void delete(AuthUser me, Long id) {
		Product p = findEditable(id, me);
		List<String> files = p.getMedia().stream().map(MediaFile::getFileUrl).toList();
		repository.delete(p);
		files.forEach(mediaService::deleteFromDisk);
	}

	@Transactional
	public ProductDto addMedia(AuthUser me, Long id, List<MultipartFile> files) {
		Product p = findEditable(id, me);
		p.getMedia().addAll(mediaService.saveAll(files, me, m -> m.setProduct(p)));
		return ProductDto.from(p);
	}

	public Product find(Long id) {
		return repository.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
	}

	/** Only the supplier who owns it, or an admin. */
	private Product findEditable(Long id, AuthUser me) {
		Product p = find(id);
		if (!me.isAdmin() && !p.getSupplier().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		return p;
	}

	private void apply(Product p, ProductRequest req) {
		p.setName(req.name().trim());
		p.setDescription(req.description());
		p.setCategory(req.categoryId() == null ? null
				: categoryRepository.findById(req.categoryId()).orElseThrow(() -> ApiException.notFound("Category")));
		p.setUnit(req.unit().trim());
		p.setPrice(Money.scale(req.price()));
		p.setStockQty(req.stockQty());
		p.setMinOrderQty(req.minOrderQty());
		if (req.status() != null) {
			p.setStatus(req.status());
		}
	}

	public static String likePattern(String q) {
		return "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
	}

}
