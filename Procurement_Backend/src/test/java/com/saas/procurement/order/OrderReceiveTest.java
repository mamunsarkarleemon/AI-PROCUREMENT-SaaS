package com.saas.procurement.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.order.OrderService.ReceiveRequest;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;

/** 100 ordered, 80 good, 20 damaged: pay for 80; 20 go back to stock only after the supplier confirms. */
class OrderReceiveTest {

	private static final AuthUser BUYER = new AuthUser(1L, "b@x.com", "B", Role.BUYER);
	private static final AuthUser SUPPLIER = new AuthUser(2L, "s@x.com", "S", Role.SUPPLIER);
	private static final AuthUser OTHER_BUYER = new AuthUser(9L, "o@x.com", "O", Role.BUYER);

	private final OrderRepository repository = mock(OrderRepository.class);
	private OrderService service;
	private PurchaseOrder order;
	private Product product;

	@BeforeEach
	void setUp() {
		service = new OrderService(repository, mock(ProductService.class), mock(UserRepository.class), mock(NotificationService.class));
		product = new Product();
		product.setId(5L);
		product.setStockQty(50); // 100 were already taken when the order was confirmed
		order = new PurchaseOrder();
		order.setId(7L);
		order.setBuyer(user(1L));
		order.setSupplier(user(2L));
		order.setProduct(product);
		order.setProductName("Box");
		order.setUnit("box");
		order.setQuantity(100);
		order.setUnitPrice(new BigDecimal("10.00"));
		order.setTotalAmount(new BigDecimal("1000.00"));
		order.setSource(OrderSource.DIRECT);
		order.setStatus(OrderStatus.SHIPPED);
		when(repository.findById(7L)).thenReturn(Optional.of(order));
		when(repository.sumReceivedQty(any())).thenAnswer(i -> (long) order.effectiveQty());
	}

	private static User user(Long id) {
		User u = new User();
		u.setId(id);
		return u;
	}

	@Test
	void partialReceiptChargesOnlyGoodUnitsAndLeavesStockUntilConfirmed() {
		OrderDto dto = service.receive(BUYER, 7L, new ReceiveRequest(80, "Crushed in transit"));

		assertThat(dto.status()).isEqualTo(OrderStatus.DELIVERED);
		assertThat(dto.receivedQty()).isEqualTo(80);
		assertThat(dto.returnedQty()).isEqualTo(20);
		assertThat(dto.returnStatus()).isEqualTo(ReturnStatus.PENDING);
		assertThat(dto.totalAmount()).isEqualByComparingTo("800.00");
		assertThat(dto.orderedAmount()).isEqualByComparingTo("1000.00");
		assertThat(product.getStockQty()).isEqualTo(50);
		assertThat(product.getSoldQty()).isEqualTo(80);
	}

	@Test
	void supplierConfirmationAddsReturnedUnitsToStockOnce() {
		service.receive(BUYER, 7L, new ReceiveRequest(80, "Crushed"));

		OrderDto dto = service.confirmReturn(SUPPLIER, 7L);

		assertThat(dto.returnStatus()).isEqualTo(ReturnStatus.CONFIRMED);
		assertThat(product.getStockQty()).isEqualTo(70);
		assertThatThrownBy(() -> service.confirmReturn(SUPPLIER, 7L)).isInstanceOf(ApiException.class);
		assertThat(product.getStockQty()).isEqualTo(70);
	}

	@Test
	void fullReceiptHasNoReturn() {
		OrderDto dto = service.receive(BUYER, 7L, new ReceiveRequest(100, null));

		assertThat(dto.returnStatus()).isEqualTo(ReturnStatus.NONE);
		assertThat(dto.totalAmount()).isEqualByComparingTo("1000.00");
		assertThat(product.getSoldQty()).isEqualTo(100);
	}

	@Test
	void rejectsBadInput() {
		assertThatThrownBy(() -> service.receive(BUYER, 7L, new ReceiveRequest(101, null))).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.receive(BUYER, 7L, new ReceiveRequest(80, "  "))).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> service.receive(OTHER_BUYER, 7L, new ReceiveRequest(100, null))).isInstanceOf(ApiException.class);
		order.setStatus(OrderStatus.CONFIRMED);
		assertThatThrownBy(() -> service.receive(BUYER, 7L, new ReceiveRequest(100, null))).isInstanceOf(ApiException.class);
		assertThat(order.getReceivedQty()).isNull();
	}

	@Test
	void buyerCannotConfirmAReturn() {
		service.receive(BUYER, 7L, new ReceiveRequest(80, "Crushed"));

		assertThatThrownBy(() -> service.confirmReturn(BUYER, 7L)).isInstanceOf(ApiException.class);
		assertThat(product.getStockQty()).isEqualTo(50);
	}

	@Test
	void orderWithReturnCannotChangeStatusEvenByAdmin() {
		service.receive(BUYER, 7L, new ReceiveRequest(80, "Crushed"));
		AuthUser admin = new AuthUser(3L, "a@x.com", "A", Role.ADMIN);

		assertThatThrownBy(() -> service.changeStatus(admin, 7L, OrderStatus.CANCELLED)).isInstanceOf(ApiException.class);
		assertThat(OrderDto.from(order, Role.ADMIN).nextStatuses()).isEmpty();
	}

	@Test
	void oldMarkAsReceivedStillMeansFullReceipt() {
		OrderDto dto = service.changeStatus(BUYER, 7L, OrderStatus.DELIVERED);

		assertThat(dto.receivedQty()).isEqualTo(100);
		assertThat(product.getSoldQty()).isEqualTo(100);
	}

}
