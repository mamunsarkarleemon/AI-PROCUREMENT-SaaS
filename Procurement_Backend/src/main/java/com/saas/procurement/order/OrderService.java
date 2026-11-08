package com.saas.procurement.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.common.Money;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.product.ProductStatus;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository repository;
	private final ProductService productService;
	private final UserRepository userRepository;
	private final NotificationService notifications;

	public record DirectOrderRequest(@NotNull Long productId,
			@NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
			@Size(max = 2000) String note) {
	}

	/** "Buy now": buyer orders a product at its listed price. */
	@Transactional
	public OrderDto createDirect(AuthUser me, DirectOrderRequest req) {
		Product p = productService.find(req.productId());
		if (p.getStatus() != ProductStatus.ACTIVE) {
			throw ApiException.badRequest("This product is not available.");
		}
		checkQuantity(p, req.quantity());
		PurchaseOrder order = create(userRepository.getReferenceById(me.id()), p.getSupplier(), p, p.getName(),
				p.getUnit(), req.quantity(), p.getPrice(), OrderSource.DIRECT, req.note());
		return OrderDto.from(order, me.role());
	}

	/** Used by "buy now", accepted negotiations and accepted quotes. */
	@Transactional
	public PurchaseOrder create(User buyer, User supplier, Product product, String productName, String unit,
			int quantity, BigDecimal unitPrice, OrderSource source, String note) {
		PurchaseOrder o = new PurchaseOrder();
		o.setBuyer(buyer);
		o.setSupplier(supplier);
		o.setProduct(product);
		o.setProductName(productName);
		o.setUnit(unit);
		o.setQuantity(quantity);
		o.setUnitPrice(Money.scale(unitPrice));
		o.setTotalAmount(Money.total(unitPrice, quantity));
		o.setSource(source);
		o.setNote(note);
		o.setStatus(OrderStatus.PENDING);
		repository.save(o);
		notifications.notify(supplier, "New order " + o.orderNo() + " from " + buyer.displayName() + ": " + quantity
				+ " " + unit + " of " + productName + ".", "/app/orders/" + o.getId());
		return o;
	}

	@Transactional(readOnly = true)
	public List<OrderDto> list(AuthUser me, OrderStatus status) {
		List<PurchaseOrder> orders = switch (me.role()) {
			case ADMIN -> repository.findAllByOrderByCreatedAtDesc();
			case BUYER -> repository.findByBuyerIdOrderByCreatedAtDesc(me.id());
			case SUPPLIER -> repository.findBySupplierIdOrderByCreatedAtDesc(me.id());
		};
		return orders.stream()
				.filter(o -> status == null || o.getStatus() == status)
				.map(o -> OrderDto.from(o, me.role()))
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderDto get(AuthUser me, Long id) {
		return OrderDto.from(findVisible(me, id), me.role());
	}

	@Transactional
	public OrderDto changeStatus(AuthUser me, Long id, OrderStatus newStatus) {
		PurchaseOrder o = findVisible(me, id);
		OrderStatus old = o.getStatus();
		if (!OrderWorkflow.canMove(old, newStatus, me.role())) {
			throw ApiException.badRequest("You cannot change this order from " + old + " to " + newStatus + ".");
		}

		// Keep product stock in sync: take stock when confirmed, give it back when cancelled.
		Product p = o.getProduct();
		if (p != null) {
			if (!old.holdsStock() && newStatus.holdsStock()) {
				if (p.getStockQty() < o.getQuantity()) {
					throw ApiException.badRequest("Not enough stock. Only " + p.getStockQty() + " " + p.getUnit()
							+ " left. Update the stock first.");
				}
				p.setStockQty(p.getStockQty() - o.getQuantity());
			}
			else if (old.holdsStock() && !newStatus.holdsStock()) {
				p.setStockQty(p.getStockQty() + o.getQuantity());
			}
		}
		o.setStatus(newStatus);

		String text = "Order " + o.orderNo() + " (" + o.getProductName() + ") is now " + newStatus + ".";
		String link = "/app/orders/" + o.getId();
		if (me.role() != Role.BUYER) {
			notifications.notify(o.getBuyer(), text, link);
		}
		if (me.role() != Role.SUPPLIER) {
			notifications.notify(o.getSupplier(), text, link);
		}
		return OrderDto.from(o, me.role());
	}

	private PurchaseOrder findVisible(AuthUser me, Long id) {
		PurchaseOrder o = repository.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
		boolean party = o.getBuyer().getId().equals(me.id()) || o.getSupplier().getId().equals(me.id());
		if (!me.isAdmin() && !party) {
			throw ApiException.notFound("Order");
		}
		return o;
	}

	static void checkQuantity(Product p, int quantity) {
		if (quantity < p.getMinOrderQty()) {
			throw ApiException.badRequest("Minimum order for this product is " + p.getMinOrderQty() + " " + p.getUnit() + ".");
		}
		if (quantity > p.getStockQty()) {
			throw ApiException.badRequest("Only " + p.getStockQty() + " " + p.getUnit() + " in stock.");
		}
	}

}
