package com.saas.procurement.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.admin.AdminUserService.UserForm;
import com.saas.procurement.contact.ContactMessage;
import com.saas.procurement.contact.ContactMessageRepository;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.UserStatus;
import com.saas.procurement.user.dto.UserDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminUserService userService;
	private final ContactMessageRepository contactRepository;

	// ---------- Users ----------

	/** Optional filters: ?role=SUPPLIER&status=ACTIVE&q=doha */
	@GetMapping("/users")
	public List<UserDto> users(@RequestParam(required = false) Role role,
			@RequestParam(required = false) UserStatus status, @RequestParam(required = false) String q) {
		return userService.list(role, status, q);
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto create(@Valid @RequestBody UserForm form) {
		return userService.create(form);
	}

	@PutMapping("/users/{id}")
	public UserDto update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody UserForm form) {
		return userService.update(me, id, form);
	}

	public record StatusChange(@NotNull UserStatus status) {
	}

	@PutMapping("/users/{id}/status")
	public UserDto setStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
			@Valid @RequestBody StatusChange body) {
		return userService.setStatus(me, id, body.status());
	}

	@DeleteMapping("/users/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		userService.delete(me, id);
	}

	// ---------- Contact messages from the landing page ----------

	@GetMapping("/messages")
	public List<ContactMessage> messages() {
		return contactRepository.findAllByOrderByCreatedAtDesc();
	}

	@DeleteMapping("/messages/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMessage(@PathVariable Long id) {
		contactRepository.deleteById(id);
	}

}
