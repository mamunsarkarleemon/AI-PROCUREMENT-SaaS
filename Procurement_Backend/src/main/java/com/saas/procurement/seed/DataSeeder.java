package com.saas.procurement.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.saas.procurement.category.Category;
import com.saas.procurement.category.CategoryRepository;
import com.saas.procurement.common.Money;
import com.saas.procurement.negotiation.Negotiation;
import com.saas.procurement.negotiation.NegotiationMessage;
import com.saas.procurement.negotiation.NegotiationMessageRepository;
import com.saas.procurement.negotiation.NegotiationRepository;
import com.saas.procurement.order.OrderRepository;
import com.saas.procurement.order.OrderSource;
import com.saas.procurement.order.OrderStatus;
import com.saas.procurement.order.PurchaseOrder;
import com.saas.procurement.product.Product;
import com.saas.procurement.product.ProductRepository;
import com.saas.procurement.request.BuyRequest;
import com.saas.procurement.request.BuyRequestRepository;
import com.saas.procurement.request.Quote;
import com.saas.procurement.request.QuoteRepository;
import com.saas.procurement.user.Role;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;
import com.saas.procurement.user.UserStatus;

/**
 * On startup:
 * 1. Creates the first admin account if there is no admin yet (app.admin.*).
 * 2. If app.seed-demo-data=true and there are no buyers/suppliers yet, adds DEMO data
 *    (all demo users use the password Demo@123 and an @demo.com email).
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DEMO_PASSWORD = "Demo@123";

	private final UserRepository users;
	private final CategoryRepository categories;
	private final ProductRepository products;
	private final BuyRequestRepository requests;
	private final QuoteRepository quotes;
	private final NegotiationRepository negotiations;
	private final NegotiationMessageRepository negotiationMessages;
	private final OrderRepository orders;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.admin.email}")
	private String adminEmail;
	@Value("${app.admin.password}")
	private String adminPassword;
	@Value("${app.admin.name}")
	private String adminName;
	@Value("${app.seed-demo-data:false}")
	private boolean seedDemoData;

	public DataSeeder(UserRepository users, CategoryRepository categories, ProductRepository products,
			BuyRequestRepository requests, QuoteRepository quotes, NegotiationRepository negotiations,
			NegotiationMessageRepository negotiationMessages, OrderRepository orders, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.categories = categories;
		this.products = products;
		this.requests = requests;
		this.quotes = quotes;
		this.negotiations = negotiations;
		this.negotiationMessages = negotiationMessages;
		this.orders = orders;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!users.existsByRole(Role.ADMIN)) {
			User admin = user(adminName, adminEmail, adminPassword, Role.ADMIN, "ProcureAI", "Doha", "Qatar");
			log.info("Created admin account: {}", admin.getEmail());
		}
		if (seedDemoData && !users.existsByRole(Role.BUYER) && !users.existsByRole(Role.SUPPLIER)) {
			seedDemo();
			log.info("Demo data loaded. Demo users: supplier1@demo.com, supplier2@demo.com, supplier3@demo.com, "
					+ "buyer1@demo.com, buyer2@demo.com (password {})", DEMO_PASSWORD);
		}
	}

	private void seedDemo() {
		User gulfPack = user("Rahim Uddin", "supplier1@demo.com", DEMO_PASSWORD, Role.SUPPLIER, "Gulf Pack Trading", "Doha", "Qatar");
		gulfPack.setPhone("+974 5500 1122");
		gulfPack.setAbout("Food packaging wholesaler serving restaurants and caterers across Qatar since 2015.");
		User alNoor = user("Sara Ahmed", "supplier2@demo.com", DEMO_PASSWORD, Role.SUPPLIER, "Al Noor Building Supplies", "Doha", "Qatar");
		alNoor.setPhone("+974 5500 3344");
		User officeMart = user("Chen Wei", "supplier3@demo.com", DEMO_PASSWORD, Role.SUPPLIER, "Shenzhen Office Mart", "Shenzhen", "China");
		User dohaFoods = user("Aisha Rahman", "buyer1@demo.com", DEMO_PASSWORD, Role.BUYER, "Doha Foods LLC", "Doha", "Qatar");
		dohaFoods.setPhone("+974 6600 7788");
		User pearlCafe = user("Omar Khalid", "buyer2@demo.com", DEMO_PASSWORD, Role.BUYER, "Pearl Cafe Group", "Lusail", "Qatar");

		Product boxes = product(gulfPack, "Food Packaging", "Kraft Paper Food Box 750ml",
				"Grease-resistant kraft box with lid, food-contact safe. Ideal for takeaway meals.", "carton", "85.00", 420, 10);
		Product cups = product(gulfPack, "Food Packaging", "Double Wall Paper Cup 12oz",
				"Insulated hot-drink cup, 500 cups per carton. Matching lids sold separately.", "carton", "120.00", 260, 5);
		Product foil = product(gulfPack, "Food Packaging", "Aluminium Foil Container 83A",
				"Oven-safe foil container with board lid, 1000 pcs per carton.", "carton", "210.00", 90, 3);
		Product gloves = product(gulfPack, "Cleaning & Hygiene", "Nitrile Gloves (Blue, M)",
				"Powder-free, food-grade, 100 gloves per box.", "box", "18.50", 1200, 50);
		Product cement = product(alNoor, "Construction Materials", "Portland Cement 50kg",
				"OPC 42.5N, suitable for structural concrete.", "bag", "17.00", 3000, 100);
		product(alNoor, "Construction Materials", "Steel Rebar 12mm (12m)",
				"Grade B500B deformed bars.", "piece", "32.00", 800, 50);
		Product tiles = product(alNoor, "Construction Materials", "Porcelain Floor Tile 60x60",
				"Matt finish, grey. 1.44 sqm per box.", "box", "48.00", 35, 20);
		Product paper = product(officeMart, "Office Supplies", "A4 Copy Paper 80gsm",
				"5 reams per box, 500 sheets per ream.", "box", "62.00", 600, 10);
		Product chairs = product(officeMart, "Furniture", "Ergonomic Office Chair",
				"Mesh back, adjustable arms and lumbar support.", "piece", "390.00", 45, 2);
		Product toner = product(officeMart, "Office Supplies", "Compatible Toner Cartridge 26A",
				"High-yield, 3100 pages.", "piece", "95.00", 8, 5);

		// Orders spread over the last 30 days so the dashboards and reports have something to show.
		order(dohaFoods, gulfPack, boxes, 40, "82.00", OrderStatus.DELIVERED, OrderSource.NEGOTIATION, 27);
		order(dohaFoods, gulfPack, cups, 20, "120.00", OrderStatus.DELIVERED, OrderSource.DIRECT, 24);
		order(pearlCafe, gulfPack, cups, 15, "118.00", OrderStatus.DELIVERED, OrderSource.DIRECT, 21);
		order(dohaFoods, alNoor, cement, 300, "16.50", OrderStatus.DELIVERED, OrderSource.DIRECT, 18);
		order(pearlCafe, officeMart, chairs, 6, "380.00", OrderStatus.SHIPPED, OrderSource.DIRECT, 14);
		order(dohaFoods, gulfPack, gloves, 100, "18.50", OrderStatus.CONFIRMED, OrderSource.DIRECT, 10);
		order(pearlCafe, gulfPack, foil, 4, "210.00", OrderStatus.CANCELLED, OrderSource.DIRECT, 8);
		order(dohaFoods, officeMart, paper, 25, "60.00", OrderStatus.SHIPPED, OrderSource.DIRECT, 6);
		order(pearlCafe, alNoor, tiles, 20, "47.00", OrderStatus.CONFIRMED, OrderSource.DIRECT, 3);
		order(dohaFoods, gulfPack, boxes, 30, "85.00", OrderStatus.PENDING, OrderSource.DIRECT, 1);
		order(pearlCafe, officeMart, toner, 5, "95.00", OrderStatus.PENDING, OrderSource.DIRECT, 0);

		// An open negotiation: buyer offered, supplier must answer.
		Negotiation n = new Negotiation();
		n.setProduct(boxes);
		n.setBuyer(pearlCafe);
		n.setSupplier(gulfPack);
		n.setCurrentPrice(new BigDecimal("78.00"));
		n.setCurrentQty(60);
		n.setLastOfferBy(Role.BUYER);
		n.setCreatedAt(LocalDateTime.now().minusHours(5));
		negotiations.save(n);
		negotiationMessage(n, pearlCafe, NegotiationMessage.Action.OFFER, "78.00", 60,
				"We order every month. Can you do 78 per carton for 60 cartons?");

		// Buying requests: one with two quotes waiting for the buyer, one fresh.
		BuyRequest napkins = request(dohaFoods, "Food Packaging", "Printed paper napkins with our logo",
				"2-ply white napkins, 33x33cm, one-colour logo print. Need samples first.", 200, "carton", "45.00", 21);
		quote(napkins, gulfPack, "44.00", 200, 14, "We can print your logo. Free samples in 3 days.");
		quote(napkins, officeMart, "39.50", 200, 25, "Price includes printing. Sea freight to Doha extra.");
		request(pearlCafe, "Furniture", "Outdoor cafe tables and chairs",
				"Weather-resistant set for terrace, 4 chairs per table.", 12, "set", null, 30);
	}

	private User user(String name, String email, String password, Role role, String company, String city, String country) {
		User u = new User();
		u.setFullName(name);
		u.setEmail(email.toLowerCase());
		u.setPasswordHash(passwordEncoder.encode(password));
		u.setRole(role);
		u.setStatus(UserStatus.ACTIVE);
		u.setCompanyName(company);
		u.setCity(city);
		u.setCountry(country);
		return users.save(u);
	}

	private Product product(User supplier, String category, String name, String description, String unit, String price,
			int stock, int moq) {
		Product p = new Product();
		p.setSupplier(supplier);
		p.setCategory(category(category));
		p.setName(name);
		p.setDescription(description);
		p.setUnit(unit);
		p.setPrice(new BigDecimal(price));
		p.setStockQty(stock);
		p.setMinOrderQty(moq);
		return products.save(p);
	}

	private void order(User buyer, User supplier, Product p, int qty, String price, OrderStatus status,
			OrderSource source, int daysAgo) {
		PurchaseOrder o = new PurchaseOrder();
		o.setBuyer(buyer);
		o.setSupplier(supplier);
		o.setProduct(p);
		o.setProductName(p.getName());
		o.setUnit(p.getUnit());
		o.setQuantity(qty);
		o.setUnitPrice(new BigDecimal(price));
		o.setTotalAmount(Money.total(new BigDecimal(price), qty));
		o.setStatus(status);
		o.setSource(source);
		o.setCreatedAt(LocalDateTime.now().minusDays(daysAgo).withHour(10).withMinute(30));
		if (status == OrderStatus.DELIVERED) {
			o.setReceivedQty(qty);
			p.setSoldQty(p.getSoldQty() + qty);
		}
		orders.save(o);
	}

	private BuyRequest request(User buyer, String category, String title, String description, int qty, String unit,
			String target, int neededInDays) {
		BuyRequest r = new BuyRequest();
		r.setBuyer(buyer);
		r.setCategory(category(category));
		r.setTitle(title);
		r.setDescription(description);
		r.setQuantity(qty);
		r.setUnit(unit);
		r.setTargetPrice(target == null ? null : new BigDecimal(target));
		r.setNeededBy(LocalDate.now().plusDays(neededInDays));
		r.setDeliveryCity(buyer.getCity());
		return requests.save(r);
	}

	private void quote(BuyRequest r, User supplier, String price, int qty, int days, String message) {
		Quote q = new Quote();
		q.setRequest(r);
		q.setSupplier(supplier);
		q.setPricePerUnit(new BigDecimal(price));
		q.setQuantity(qty);
		q.setDeliveryDays(days);
		q.setMessage(message);
		quotes.save(q);
	}

	private void negotiationMessage(Negotiation n, User sender, NegotiationMessage.Action action, String price, int qty,
			String text) {
		NegotiationMessage m = new NegotiationMessage();
		m.setNegotiation(n);
		m.setSender(sender);
		m.setAction(action);
		m.setPricePerUnit(new BigDecimal(price));
		m.setQuantity(qty);
		m.setMessage(text);
		negotiationMessages.save(m);
	}

	private Category category(String name) {
		return categories.findAllByOrderByNameAsc().stream().filter(c -> c.getName().equals(name)).findFirst()
				.orElse(null);
	}

}
