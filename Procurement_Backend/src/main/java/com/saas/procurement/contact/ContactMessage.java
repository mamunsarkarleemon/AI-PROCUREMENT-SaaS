package com.saas.procurement.contact;

import com.saas.procurement.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sent from the "Contact us" form on the landing page. */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
public class ContactMessage extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

}
