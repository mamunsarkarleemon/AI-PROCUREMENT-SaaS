package com.saas.procurement.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One item in the left sidebar. The list is seeded by the V1 migration. */
@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
public class Menu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "menu_key", nullable = false, unique = true)
	private String menuKey;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private String path;

	@Column(nullable = false)
	private String icon;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "for_admin", nullable = false)
	private boolean forAdmin;

	@Column(name = "for_buyer", nullable = false)
	private boolean forBuyer;

	@Column(name = "for_supplier", nullable = false)
	private boolean forSupplier;

	/** Always on; cannot be switched off (e.g. Dashboard, Profile). */
	@Column(nullable = false)
	private boolean locked;

}
