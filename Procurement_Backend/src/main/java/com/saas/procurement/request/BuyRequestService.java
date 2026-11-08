package com.saas.procurement.request;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saas.procurement.category.CategoryRepository;
import com.saas.procurement.common.ApiException;
import com.saas.procurement.common.Money;
import com.saas.procurement.media.MediaService;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.request.RequestDtos.BuyRequestDto;
import com.saas.procurement.request.RequestDtos.BuyRequestForm;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuyRequestService {

	private final BuyRequestRepository repository;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final MediaService mediaService;
	private final NotificationService notifications;

	/** Open requests from active buyers - the "Buying Requests" tab of the marketplace. */
	@Transactional(readOnly = true)
	public List<BuyRequestDto> open(AuthUser me, String q, Long categoryId) {
		return repository.search(RequestStatus.OPEN, UserStatus.ACTIVE, categoryId, ProductService.likePattern(q))
				.stream().map(r -> toDto(r, me)).toList();
	}

	@Transactional(readOnly = true)
	public List<BuyRequestDto> mine(AuthUser me) {
		return repository.findByBuyerIdOrderByCreatedAtDesc(me.id()).stream().map(r -> toDto(r, me)).toList();
	}

	@Transactional(readOnly = true)
	public List<BuyRequestDto> all(AuthUser me) {
		return repository.findAllByOrderByCreatedAtDesc().stream().map(r -> toDto(r, me)).toList();
	}

	@Transactional(readOnly = true)
	public BuyRequestDto get(AuthUser me, Long id) {
		return toDto(find(id), me);
	}

	@Transactional
	public BuyRequestDto create(AuthUser me, BuyRequestForm form) {
		BuyRequest r = new BuyRequest();
		User buyer = userRepository.getReferenceById(me.id());
		r.setBuyer(buyer);
		apply(r, form);
		repository.save(r);
		notifications.notifyRole(Role.SUPPLIER, "New buying request: " + r.getQuantity() + " " + r.getUnit() + " of "
				+ r.getTitle() + ". Send your quote!", "/app/requests/" + r.getId());
		return toDto(r, me);
	}

	@Transactional
	public BuyRequestDto update(AuthUser me, Long id, BuyRequestForm form) {
		BuyRequest r = findOwned(id, me);
		if (r.getStatus() != RequestStatus.OPEN) {
			throw ApiException.badRequest("A closed request cannot be edited.");
		}
		apply(r, form);
		return toDto(r, me);
	}

	@Transactional
	public BuyRequestDto close(AuthUser me, Long id) {
		BuyRequest r = findOwned(id, me);
		r.setStatus(RequestStatus.CLOSED);
		return toDto(r, me);
	}

	@Transactional
	public void delete(AuthUser me, Long id) {
		BuyRequest r = findOwned(id, me);
		var files = new java.util.ArrayList<String>();
		r.getMedia().forEach(m -> files.add(m.getFileUrl()));
		r.getQuotes().forEach(q -> q.getMedia().forEach(m -> files.add(m.getFileUrl())));
		repository.delete(r);
		files.forEach(mediaService::deleteFromDisk);
	}

	@Transactional
	public BuyRequestDto addMedia(AuthUser me, Long id, List<MultipartFile> files) {
		BuyRequest r = findOwned(id, me);
		r.getMedia().addAll(mediaService.saveAll(files, me, m -> m.setRequest(r)));
		return toDto(r, me);
	}

	public BuyRequest find(Long id) {
		return repository.findById(id).orElseThrow(() -> ApiException.notFound("Buying request"));
	}

	/** Owner sees all quotes, admin sees all, a supplier sees only their own quote. */
	BuyRequestDto toDto(BuyRequest r, AuthUser me) {
		boolean owner = r.getBuyer().getId().equals(me.id());
		List<Quote> visible = (owner || me.isAdmin())
				? r.getQuotes().stream().filter(q -> q.getStatus() != QuoteStatus.WITHDRAWN).toList()
				: r.getQuotes().stream().filter(q -> q.getSupplier().getId().equals(me.id())).toList();
		return BuyRequestDto.from(r, visible);
	}

	private BuyRequest findOwned(Long id, AuthUser me) {
		BuyRequest r = find(id);
		if (!me.isAdmin() && !r.getBuyer().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		return r;
	}

	private void apply(BuyRequest r, BuyRequestForm f) {
		r.setTitle(f.title().trim());
		r.setDescription(f.description());
		r.setCategory(f.categoryId() == null ? null
				: categoryRepository.findById(f.categoryId()).orElseThrow(() -> ApiException.notFound("Category")));
		r.setQuantity(f.quantity());
		r.setUnit(f.unit().trim());
		r.setTargetPrice(Money.scale(f.targetPrice()));
		r.setNeededBy(f.neededBy());
		r.setDeliveryCity(f.deliveryCity());
	}

}
