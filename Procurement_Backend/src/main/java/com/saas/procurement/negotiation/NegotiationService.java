package com.saas.procurement.negotiation;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.common.Money;
import com.saas.procurement.negotiation.NegotiationDtos.CounterForm;
import com.saas.procurement.negotiation.NegotiationDtos.NegotiationDto;
import com.saas.procurement.negotiation.NegotiationDtos.StartForm;
import com.saas.procurement.negotiation.NegotiationMessage.Action;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.order.OrderService;
import com.saas.procurement.order.OrderSource;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.product.ProductStatus;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Flow:
 * 1. Buyer offers a price and quantity for a product.
 * 2. Supplier accepts, counters (new price) or rejects.
 * 3. Buyer answers a counter the same way ... until someone accepts or rejects.
 * 4. On accept an order is created at the agreed price (status PENDING, supplier confirms it).
 */
@Service
@RequiredArgsConstructor
public class NegotiationService {

	private final NegotiationRepository repository;
	private final NegotiationMessageRepository messageRepository;
	private final ProductService productService;
	private final UserRepository userRepository;
	private final OrderService orderService;
	private final OrderRepository orderRepository;
	private final NotificationService notifications;

	@Transactional
	public NegotiationDto start(AuthUser me, StartForm form) {
		Product p = productService.find(form.productId());
		if (p.getStatus() != ProductStatus.ACTIVE) {
			throw ApiException.badRequest("This product is not available.");
		}
		if (form.quantity() < p.getMinOrderQty()) {
			throw ApiException.badRequest("Minimum order for this product is " + p.getMinOrderQty() + " " + p.getUnit() + ".");
		}
		repository.findFirstByProductIdAndBuyerIdAndStatus(p.getId(), me.id(), Negotiation.Status.OPEN).ifPresent(n -> {
			throw ApiException.conflict("You already have an open negotiation for this product (#" + n.getId() + ").");
		});

		User buyer = userRepository.getReferenceById(me.id());
		Negotiation n = new Negotiation();
		n.setProduct(p);
		n.setBuyer(buyer);
		n.setSupplier(p.getSupplier());
		n.setCurrentPrice(Money.scale(form.pricePerUnit()));
		n.setCurrentQty(form.quantity());
		n.setLastOfferBy(Role.BUYER);
		repository.save(n);
		addMessage(n, buyer, Action.OFFER, n.getCurrentPrice(), n.getCurrentQty(), form.message());

		notifications.notify(p.getSupplier(), buyer.displayName() + " offers " + n.getCurrentPrice() + " per "
				+ p.getUnit() + " for " + n.getCurrentQty() + " " + p.getUnit() + " of " + p.getName() + ".",
				link(n));
		return toDto(n, me, true);
	}

	@Transactional(readOnly = true)
	public List<NegotiationDto> list(AuthUser me) {
		List<Negotiation> list = switch (me.role()) {
			case ADMIN -> repository.findAllByOrderByUpdatedAtDesc();
			case BUYER -> repository.findByBuyerIdOrderByUpdatedAtDesc(me.id());
			case SUPPLIER -> repository.findBySupplierIdOrderByUpdatedAtDesc(me.id());
		};
		return list.stream().map(n -> toDto(n, me, false)).toList();
	}

	@Transactional(readOnly = true)
	public NegotiationDto get(AuthUser me, Long id) {
		return toDto(findVisible(me, id), me, true);
	}

	@Transactional
	public NegotiationDto counter(AuthUser me, Long id, CounterForm form) {
		Negotiation n = findMyTurn(me, id);
		n.setCurrentPrice(Money.scale(form.pricePerUnit()));
		n.setCurrentQty(form.quantity());
		n.setLastOfferBy(me.role());
		User sender = userRepository.getReferenceById(me.id());
		addMessage(n, sender, Action.COUNTER, n.getCurrentPrice(), n.getCurrentQty(), form.message());
		notifications.notify(otherSide(n, me), sender.displayName() + " sent a counter-offer: " + n.getCurrentPrice()
				+ " per " + n.getProduct().getUnit() + " for " + n.getProduct().getName() + ".", link(n));
		return toDto(n, me, true);
	}

	/** Accepting the other side's latest offer creates the order. */
	@Transactional
	public NegotiationDto accept(AuthUser me, Long id, String message) {
		Negotiation n = findMyTurn(me, id);
		n.setStatus(Negotiation.Status.ACCEPTED);
		User sender = userRepository.getReferenceById(me.id());
		addMessage(n, sender, Action.ACCEPT, n.getCurrentPrice(), n.getCurrentQty(), message);

		Product p = n.getProduct();
		PurchaseOrder order = orderService.create(n.getBuyer(), n.getSupplier(), p, p.getName(), p.getUnit(),
				n.getCurrentQty(), n.getCurrentPrice(), OrderSource.NEGOTIATION, null);
		order.setNegotiation(n);
		notifications.notify(otherSide(n, me), sender.displayName() + " accepted the deal for " + p.getName()
				+ ". Order " + order.orderNo() + " was created.", "/app/orders/" + order.getId());
		return toDto(n, me, true);
	}

	/** Either side can end the negotiation at any time. */
	@Transactional
	public NegotiationDto reject(AuthUser me, Long id, String message) {
		Negotiation n = findVisible(me, id);
		if (me.isAdmin()) {
			throw ApiException.forbidden();
		}
		if (n.getStatus() != Negotiation.Status.OPEN) {
			throw ApiException.badRequest("This negotiation is already closed.");
		}
		n.setStatus(Negotiation.Status.REJECTED);
		User sender = userRepository.getReferenceById(me.id());
		addMessage(n, sender, Action.REJECT, null, null, message);
		notifications.notify(otherSide(n, me), sender.displayName() + " ended the negotiation for "
				+ n.getProduct().getName() + ".", link(n));
		return toDto(n, me, true);
	}

	private void addMessage(Negotiation n, User sender, Action action, BigDecimal price, Integer qty, String text) {
		NegotiationMessage m = new NegotiationMessage();
		m.setNegotiation(n);
		m.setSender(sender);
		m.setAction(action);
		m.setPricePerUnit(price);
		m.setQuantity(qty);
		m.setMessage(text == null || text.isBlank() ? null : text.trim());
		messageRepository.save(m);
		n.getMessages().add(m);
	}

	private Negotiation findVisible(AuthUser me, Long id) {
		Negotiation n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Negotiation"));
		boolean party = n.getBuyer().getId().equals(me.id()) || n.getSupplier().getId().equals(me.id());
		if (!me.isAdmin() && !party) {
			throw ApiException.notFound("Negotiation");
		}
		return n;
	}

	private Negotiation findMyTurn(AuthUser me, Long id) {
		Negotiation n = findVisible(me, id);
		if (me.isAdmin()) {
			throw ApiException.forbidden();
		}
		if (n.getStatus() != Negotiation.Status.OPEN) {
			throw ApiException.badRequest("This negotiation is already closed.");
		}
		if (n.getLastOfferBy() == me.role()) {
			throw ApiException.badRequest("Please wait for the other side to reply to your offer.");
		}
		return n;
	}

	private static User otherSide(Negotiation n, AuthUser me) {
		return me.role() == Role.BUYER ? n.getSupplier() : n.getBuyer();
	}

	private static String link(Negotiation n) {
		return "/app/negotiations/" + n.getId();
	}

	private NegotiationDto toDto(Negotiation n, AuthUser me, boolean withMessages) {
		Long orderId = n.getStatus() == Negotiation.Status.ACCEPTED
				? orderRepository.findFirstByNegotiationId(n.getId()).map(PurchaseOrder::getId).orElse(null)
				: null;
		return NegotiationDto.from(n, me, orderId, withMessages);
	}

}
