package com.saas.procurement.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.negotiation.Negotiation;
import com.saas.procurement.negotiation.NegotiationRepository;
import com.saas.procurement.order.OrderDto;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.order.OrderStatus;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.ProductDto;
import com.saas.procurement.product.ProductRepository;
import com.saas.procurement.product.ProductStatus;
import com.saas.procurement.report.ReportService.DayPoint;
import com.saas.procurement.request.BuyRequestRepository;
import com.saas.procurement.request.QuoteRepository;
import com.saas.procurement.request.QuoteStatus;
import com.saas.procurement.request.RequestStatus;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserDto;

import lombok.RequiredArgsConstructor;

/** The numbers and lists on each role's home page. */
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final BuyRequestRepository requestRepository;
	private final QuoteRepository quoteRepository;
	private final NegotiationRepository negotiationRepository;
	private final UserRepository userRepository;

	/** format: "money" or "count". tone picks the card colour in the UI. */
	public record StatCard(String label, BigDecimal value, String format, String hint, String icon, String tone) {
		static StatCard money(String label, BigDecimal v, String hint, String icon, String tone) {
			return new StatCard(label, v, "money", hint, icon, tone);
		}

		static StatCard count(String label, long v, String hint, String icon, String tone) {
			return new StatCard(label, BigDecimal.valueOf(v), "count", hint, icon, tone);
		}
	}

	public record Dashboard(List<StatCard> cards, String chartTitle, List<DayPoint> chart, List<OrderDto> recentOrders,
			String productsTitle, List<ProductDto> products, List<UserDto> recentUsers) {
	}

	@Transactional(readOnly = true)
	public Dashboard forUser(AuthUser me) {
		return switch (me.role()) {
			case BUYER -> buyer(me);
			case SUPPLIER -> supplier(me);
			case ADMIN -> admin(me);
		};
	}

	private Dashboard buyer(AuthUser me) {
		List<PurchaseOrder> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(me.id());
		List<PurchaseOrder> valid = valid(orders);
		List<StatCard> cards = List.of(
				StatCard.money("Total spent", ReportService.sum(valid), "All orders except cancelled", "Wallet", "blue"),
				StatCard.count("Orders", orders.size(), count(orders, OrderStatus.PENDING) + " waiting for supplier", "ShoppingCart", "purple"),
				StatCard.count("Open negotiations", negotiationRepository.countByBuyerIdAndStatus(me.id(), Negotiation.Status.OPEN), "Price talks in progress", "Handshake", "green"),
				StatCard.count("Open buying requests", requestRepository.countByBuyerIdAndStatus(me.id(), RequestStatus.OPEN), "Waiting for quotes", "ClipboardList", "orange"));
		return new Dashboard(cards, "Spending - last 30 days", last30Days(valid), recent(orders, me),
				"New in the marketplace", productRepository.findByStatusAndSupplierStatusOrderByCreatedAtDesc(
						ProductStatus.ACTIVE, UserStatus.ACTIVE, Limit.of(4)).stream().map(ProductDto::from).toList(),
				List.of());
	}

	private Dashboard supplier(AuthUser me) {
		List<PurchaseOrder> orders = orderRepository.findBySupplierIdOrderByCreatedAtDesc(me.id());
		List<PurchaseOrder> valid = valid(orders);
		List<StatCard> cards = List.of(
				StatCard.money("Total sales", ReportService.sum(valid), "All orders except cancelled", "Wallet", "blue"),
				StatCard.count("Orders received", orders.size(), count(orders, OrderStatus.PENDING) + " need your confirmation", "ShoppingCart", "purple"),
				StatCard.count("My products", productRepository.countBySupplierId(me.id()), "In your catalog", "Package", "green"),
				StatCard.count("Open buying requests", requestRepository.countByStatus(RequestStatus.OPEN), quoteRepository.countBySupplierIdAndStatus(me.id(), QuoteStatus.PENDING) + " of your quotes pending", "ClipboardList", "orange"));
		return new Dashboard(cards, "Sales - last 30 days", last30Days(valid), recent(orders, me), "Low stock",
				productRepository.findTop5BySupplierIdAndStatusOrderByStockQtyAsc(me.id(), ProductStatus.ACTIVE).stream()
						.map(ProductDto::from).toList(),
				List.of());
	}

	private Dashboard admin(AuthUser me) {
		List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
		List<PurchaseOrder> valid = valid(orders);
		List<StatCard> cards = List.of(
				StatCard.money("Order value", ReportService.sum(valid), "All orders except cancelled", "Wallet", "blue"),
				StatCard.count("Orders", orders.size(), count(orders, OrderStatus.PENDING) + " pending", "ShoppingCart", "purple"),
				StatCard.count("Suppliers", userRepository.countByRole(Role.SUPPLIER), userRepository.countByRole(Role.BUYER) + " buyers", "Factory", "green"),
				StatCard.count("Active products", productRepository.countByStatus(ProductStatus.ACTIVE), requestRepository.countByStatus(RequestStatus.OPEN) + " open buying requests", "Boxes", "orange"));
		return new Dashboard(cards, "Order value - last 30 days", last30Days(valid), recent(orders, me), null, List.of(),
				userRepository.findTop5ByOrderByCreatedAtDesc().stream().map(UserDto::from).toList());
	}

	private static List<PurchaseOrder> valid(List<PurchaseOrder> orders) {
		return orders.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
	}

	private static long count(List<PurchaseOrder> orders, OrderStatus s) {
		return orders.stream().filter(o -> o.getStatus() == s).count();
	}

	private static List<DayPoint> last30Days(List<PurchaseOrder> valid) {
		LocalDate today = LocalDate.now();
		return ReportService.daily(valid, today.minusDays(29), today);
	}

	private static List<OrderDto> recent(List<PurchaseOrder> orders, AuthUser me) {
		return orders.stream().limit(6).map(o -> OrderDto.from(o, me.role())).toList();
	}

}
