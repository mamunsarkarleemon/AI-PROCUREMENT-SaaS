package com.saas.procurement.request;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.common.Money;
import com.saas.procurement.media.MediaService;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.order.OrderDto;
import com.saas.procurement.order.OrderService;
import com.saas.procurement.order.OrderSource;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.request.RequestDtos.QuoteDto;
import com.saas.procurement.request.RequestDtos.QuoteForm;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuoteService {

	private final QuoteRepository repository;
	private final BuyRequestService requestService;
	private final ProductService productService;
	private final UserRepository userRepository;
	private final OrderService orderService;
	private final MediaService mediaService;
	private final NotificationService notifications;

	/** Supplier sends a quote. Sending again updates their pending quote. */
	@Transactional
	public QuoteDto submit(AuthUser me, Long requestId, QuoteForm form) {
		BuyRequest r = requestService.find(requestId);
		if (r.getStatus() != RequestStatus.OPEN) {
			throw ApiException.badRequest("This request is closed.");
		}
		Quote q = repository.findByRequestIdAndSupplierId(requestId, me.id()).orElse(null);
		boolean isNew = q == null;
		if (isNew) {
			q = new Quote();
			q.setRequest(r);
			q.setSupplier(userRepository.getReferenceById(me.id()));
		}
		else if (q.getStatus() == QuoteStatus.ACCEPTED || q.getStatus() == QuoteStatus.REJECTED) {
			throw ApiException.badRequest("Your quote was already " + q.getStatus().name().toLowerCase() + ".");
		}
		q.setStatus(QuoteStatus.PENDING);
		q.setPricePerUnit(Money.scale(form.pricePerUnit()));
		q.setQuantity(form.quantity());
		q.setDeliveryDays(form.deliveryDays());
		q.setMessage(form.message());
		if (form.productId() != null) {
			Product p = productService.find(form.productId());
			if (!p.getSupplier().getId().equals(me.id())) {
				throw ApiException.badRequest("You can only link your own product.");
			}
			q.setProduct(p);
		}
		else {
			q.setProduct(null);
		}
		repository.save(q);

		notifications.notify(r.getBuyer(), (isNew ? "New quote" : "Updated quote") + " for \"" + r.getTitle() + "\" from "
				+ q.getSupplier().displayName() + ": " + q.getPricePerUnit() + " per " + r.getUnit() + ".",
				"/app/requests/" + r.getId());
		return QuoteDto.from(q);
	}

	@Transactional(readOnly = true)
	public List<QuoteDto> mine(AuthUser me) {
		return repository.findBySupplierIdOrderByCreatedAtDesc(me.id()).stream().map(QuoteDto::from).toList();
	}

	@Transactional
	public QuoteDto addMedia(AuthUser me, Long quoteId, List<MultipartFile> files) {
		Quote q = find(quoteId);
		if (!q.getSupplier().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		q.getMedia().addAll(mediaService.saveAll(files, me, m -> m.setQuote(q)));
		return QuoteDto.from(q);
	}

	/** Buyer accepts: the quote becomes an order, the request closes, other quotes are declined. */
	@Transactional
	public OrderDto accept(AuthUser me, Long quoteId) {
		Quote q = findForBuyer(quoteId, me);
		BuyRequest r = q.getRequest();
		if (r.getStatus() != RequestStatus.OPEN || q.getStatus() != QuoteStatus.PENDING) {
			throw ApiException.badRequest("This quote can no longer be accepted.");
		}
		q.setStatus(QuoteStatus.ACCEPTED);
		r.setStatus(RequestStatus.CLOSED);
		for (Quote other : r.getQuotes()) {
			if (!other.getId().equals(q.getId()) && other.getStatus() == QuoteStatus.PENDING) {
				other.setStatus(QuoteStatus.REJECTED);
				notifications.notify(other.getSupplier(), "Your quote for \"" + r.getTitle() + "\" was not selected.",
						"/app/my-quotes");
			}
		}
		String name = q.getProduct() != null ? q.getProduct().getName() : r.getTitle();
		PurchaseOrder order = orderService.create(r.getBuyer(), q.getSupplier(), q.getProduct(), name, r.getUnit(),
				q.getQuantity(), q.getPricePerUnit(), OrderSource.QUOTE, q.getMessage());
		order.setRequest(r);
		order.setQuote(q);
		notifications.notify(q.getSupplier(), "Your quote for \"" + r.getTitle() + "\" was ACCEPTED. Order "
				+ order.orderNo() + " is waiting for your confirmation.", "/app/orders/" + order.getId());
		return OrderDto.from(order, me.role());
	}

	@Transactional
	public QuoteDto reject(AuthUser me, Long quoteId) {
		Quote q = findForBuyer(quoteId, me);
		if (q.getStatus() != QuoteStatus.PENDING) {
			throw ApiException.badRequest("Only pending quotes can be rejected.");
		}
		q.setStatus(QuoteStatus.REJECTED);
		notifications.notify(q.getSupplier(), "Your quote for \"" + q.getRequest().getTitle() + "\" was declined.",
				"/app/my-quotes");
		return QuoteDto.from(q);
	}

	@Transactional
	public QuoteDto withdraw(AuthUser me, Long quoteId) {
		Quote q = find(quoteId);
		if (!q.getSupplier().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		if (q.getStatus() != QuoteStatus.PENDING) {
			throw ApiException.badRequest("Only pending quotes can be withdrawn.");
		}
		q.setStatus(QuoteStatus.WITHDRAWN);
		return QuoteDto.from(q);
	}

	private Quote find(Long id) {
		return repository.findById(id).orElseThrow(() -> ApiException.notFound("Quote"));
	}

	private Quote findForBuyer(Long id, AuthUser me) {
		Quote q = find(id);
		if (!q.getRequest().getBuyer().getId().equals(me.id())) {
			throw ApiException.forbidden();
		}
		return q;
	}

}
