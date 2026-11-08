package com.saas.procurement.contact;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.notification.NotificationService;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.product.ProductDto;
import com.saas.procurement.product.ProductRepository;
import com.saas.procurement.product.ProductService;
import com.saas.procurement.product.ProductStatus;
import com.saas.procurement.request.BuyRequestRepository;
import com.saas.procurement.request.RequestStatus;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/** Data for the public landing page (no login needed). */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final BuyRequestRepository requestRepository;
	private final OrderRepository orderRepository;
	private final ProductService productService;
	private final ContactMessageRepository contactRepository;
	private final NotificationService notifications;

	public record PublicStats(long suppliers, long buyers, long products, long openRequests, long orders) {
	}

	@GetMapping("/stats")
	public PublicStats stats() {
		return new PublicStats(userRepository.countByRole(Role.SUPPLIER), userRepository.countByRole(Role.BUYER),
				productRepository.countByStatus(ProductStatus.ACTIVE), requestRepository.countByStatus(RequestStatus.OPEN),
				orderRepository.count());
	}

	/** Latest products for the "Fresh in the marketplace" section. */
	@GetMapping("/products")
	public List<ProductDto> latestProducts(@RequestParam(defaultValue = "8") int limit) {
		return productService.latest(Math.min(Math.max(limit, 1), 24));
	}

	public record ContactForm(
			@NotBlank(message = "Name is required") @Size(max = 150) String name,
			@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 200) String email,
			@Size(max = 200) String subject,
			@NotBlank(message = "Message is required") @Size(max = 5000) String message) {
	}

	@PostMapping("/contact")
	@ResponseStatus(HttpStatus.CREATED)
	public void contact(@Valid @RequestBody ContactForm form) {
		ContactMessage m = new ContactMessage();
		m.setName(form.name().trim());
		m.setEmail(form.email().trim());
		m.setSubject(form.subject());
		m.setMessage(form.message());
		contactRepository.save(m);
		notifications.notifyRole(Role.ADMIN, "New contact message from " + m.getName() + ": "
				+ (m.getSubject() == null || m.getSubject().isBlank() ? "(no subject)" : m.getSubject()),
				"/app/admin/messages");
	}

}
