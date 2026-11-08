package com.saas.procurement.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

import lombok.RequiredArgsConstructor;

/** Creates the messages shown under the bell icon. */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository repository;
	private final UserRepository userRepository;

	public record NotificationDto(Long id, String message, String link, boolean read, LocalDateTime createdAt) {
		static NotificationDto from(Notification n) {
			return new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
		}
	}

	public record NotificationList(long unread, List<NotificationDto> items) {
	}

	@Transactional
	public void notify(User user, String message, String link) {
		Notification n = new Notification();
		n.setUser(user);
		n.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
		n.setLink(link);
		repository.save(n);
	}

	/** Sends the same message to every active user of a role (e.g. all suppliers). */
	@Transactional
	public void notifyRole(Role role, String message, String link) {
		userRepository.findByRoleAndStatus(role, UserStatus.ACTIVE).forEach(u -> notify(u, message, link));
	}

	@Transactional(readOnly = true)
	public NotificationList list(Long userId) {
		return new NotificationList(repository.countByUserIdAndReadFalse(userId),
				repository.findTop30ByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationDto::from).toList());
	}

	@Transactional
	public void markRead(Long userId, Long id) {
		Notification n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notification"));
		if (!n.getUser().getId().equals(userId)) {
			throw ApiException.forbidden();
		}
		n.setRead(true);
	}

	@Transactional
	public void markAllRead(Long userId) {
		repository.markAllRead(userId);
	}

}
