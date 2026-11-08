package com.saas.procurement.notification;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.notification.NotificationService.NotificationList;
import com.saas.procurement.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService service;

	@GetMapping
	public NotificationList list(@AuthenticationPrincipal AuthUser me) {
		return service.list(me.id());
	}

	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void read(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
		service.markRead(me.id(), id);
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void readAll(@AuthenticationPrincipal AuthUser me) {
		service.markAllRead(me.id());
	}

}
