package com.saas.procurement.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.order.OrderDto;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.order.OrderStatus;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.User;

import lombok.RequiredArgsConstructor;

/**
 * Order reports for a date range.
 * Buyer: what I bought. Supplier: what I sold. Admin: everything, optionally for one supplier and/or buyer.
 * Amounts never include cancelled orders.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

	private final OrderRepository orderRepository;

	public record Summary(long orderCount, BigDecimal totalAmount, BigDecimal deliveredAmount, long pendingCount,
			long deliveredCount, long cancelledCount, BigDecimal averageOrder) {
	}

	public record DayPoint(LocalDate date, long orders, BigDecimal amount) {
	}

	public record StatusRow(OrderStatus status, long orders, BigDecimal amount) {
	}

	public record PartyRow(Long id, String name, String email, long orders, BigDecimal amount) {
	}

	public record ProductRow(String productName, String unit, long quantity, long orders, BigDecimal amount) {
	}

	public record Report(LocalDate from, LocalDate to, Summary summary, List<DayPoint> daily, List<StatusRow> byStatus,
			List<PartyRow> bySupplier, List<PartyRow> byBuyer, List<ProductRow> byProduct, List<OrderDto> orders) {
	}

	@Transactional(readOnly = true)
	public Report report(AuthUser me, LocalDate from, LocalDate to, Long supplierId, Long buyerId) {
		if (to == null) {
			to = LocalDate.now();
		}
		if (from == null) {
			from = to.minusDays(29);
		}
		if (from.isAfter(to)) {
			throw ApiException.badRequest("'From' date must be before 'To' date.");
		}
		if (from.plusDays(366).isBefore(to)) {
			throw ApiException.badRequest("Please choose a range of one year or less.");
		}

		// Buyers and suppliers only ever see their own orders.
		Long buyerFilter = me.isBuyer() ? me.id() : buyerId;
		Long supplierFilter = me.isSupplier() ? me.id() : supplierId;

		List<PurchaseOrder> orders = orderRepository
				.findByCreatedAtBetweenOrderByCreatedAtAsc(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
				.filter(o -> buyerFilter == null || o.getBuyer().getId().equals(buyerFilter))
				.filter(o -> supplierFilter == null || o.getSupplier().getId().equals(supplierFilter))
				.toList();
		List<PurchaseOrder> valid = orders.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();

		BigDecimal total = sum(valid);
		BigDecimal delivered = sum(valid.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).toList());
		Summary summary = new Summary(orders.size(), total, delivered,
				count(orders, OrderStatus.PENDING), count(orders, OrderStatus.DELIVERED),
				count(orders, OrderStatus.CANCELLED),
				valid.isEmpty() ? BigDecimal.ZERO.setScale(2)
						: total.divide(BigDecimal.valueOf(valid.size()), 2, RoundingMode.HALF_UP));

		return new Report(from, to, summary, daily(valid, from, to), byStatus(orders),
				byParty(valid, PurchaseOrder::getSupplier), byParty(valid, PurchaseOrder::getBuyer), byProduct(valid),
				orders.stream().sorted(Comparator.comparing(PurchaseOrder::getCreatedAt).reversed())
						.map(o -> OrderDto.from(o, me.role())).toList());
	}

	/** One point per day, including days with no orders, so the chart has no gaps. */
	public static List<DayPoint> daily(List<PurchaseOrder> validOrders, LocalDate from, LocalDate to) {
		Map<LocalDate, List<PurchaseOrder>> byDay = validOrders.stream()
				.collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));
		List<DayPoint> points = new ArrayList<>();
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			List<PurchaseOrder> day = byDay.getOrDefault(d, List.of());
			points.add(new DayPoint(d, day.size(), sum(day)));
		}
		return points;
	}

	private static List<StatusRow> byStatus(List<PurchaseOrder> orders) {
		List<StatusRow> rows = new ArrayList<>();
		for (OrderStatus s : OrderStatus.values()) {
			List<PurchaseOrder> list = orders.stream().filter(o -> o.getStatus() == s).toList();
			rows.add(new StatusRow(s, list.size(), sum(list)));
		}
		return rows;
	}

	private static List<PartyRow> byParty(List<PurchaseOrder> orders, Function<PurchaseOrder, User> party) {
		Map<Long, List<PurchaseOrder>> grouped = orders.stream()
				.collect(Collectors.groupingBy(o -> party.apply(o).getId(), LinkedHashMap::new, Collectors.toList()));
		return grouped.values().stream().map(list -> {
			User u = party.apply(list.get(0));
			return new PartyRow(u.getId(), u.displayName(), u.getEmail(), list.size(), sum(list));
		}).sorted(Comparator.comparing(PartyRow::amount).reversed()).toList();
	}

	private static List<ProductRow> byProduct(List<PurchaseOrder> orders) {
		Map<String, List<PurchaseOrder>> grouped = orders.stream()
				.collect(Collectors.groupingBy(o -> o.getProductName() + "|" + o.getUnit()));
		return grouped.values().stream().map(list -> new ProductRow(list.get(0).getProductName(),
				list.get(0).getUnit(), list.stream().mapToLong(PurchaseOrder::effectiveQty).sum(), list.size(), sum(list)))
				.sorted(Comparator.comparing(ProductRow::amount).reversed()).toList();
	}

	public static BigDecimal sum(List<PurchaseOrder> orders) {
		return orders.stream().map(PurchaseOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static long count(List<PurchaseOrder> orders, OrderStatus status) {
		return orders.stream().filter(o -> o.getStatus() == status).count();
	}

}
