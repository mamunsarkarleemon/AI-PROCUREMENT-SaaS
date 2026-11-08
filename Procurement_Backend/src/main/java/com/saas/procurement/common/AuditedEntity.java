package com.saas.procurement.common;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/** Tables that also have an updated_at column. */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditedEntity extends BaseEntity {

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreateSetUpdated() {
		if (updatedAt == null) {
			updatedAt = getCreatedAt() != null ? getCreatedAt() : LocalDateTime.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
