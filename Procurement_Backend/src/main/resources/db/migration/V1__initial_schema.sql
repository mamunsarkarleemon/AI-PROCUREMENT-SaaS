-- =====================================================================
-- ProcureAI - initial schema
--
--   users ─┬─< products ─┬─< media_files
--          │             └─< negotiations ─< negotiation_messages
--          ├─< buy_requests ─┬─< quotes ─< media_files
--          │                 └─< media_files
--          ├─< orders  (buyer_id, supplier_id)
--          └─< notifications
--   categories ─< products, buy_requests
--   menus ─< role_menu_access          (admin decides which menu each role sees)
--   contact_messages                    (landing page contact form)
-- =====================================================================

-- ---------- Users (buyers, suppliers, admins) ----------
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,               -- BCrypt hash, never the real password
    role          VARCHAR(20)  NOT NULL,               -- ADMIN, BUYER, SUPPLIER
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    phone         VARCHAR(40),
    company_name  VARCHAR(200),
    address       VARCHAR(300),
    city          VARCHAR(100),
    country       VARCHAR(100),
    about         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Product categories ----------
CREATE TABLE categories (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ---------- Products a supplier sells ----------
CREATE TABLE products (
    id             BIGSERIAL     PRIMARY KEY,
    supplier_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id    BIGINT        REFERENCES categories (id) ON DELETE SET NULL,
    name           VARCHAR(200)  NOT NULL,
    description    TEXT,
    unit           VARCHAR(30)   NOT NULL,              -- pcs, kg, box, carton ...
    price          NUMERIC(14,2) NOT NULL,              -- price per unit
    stock_qty      INTEGER       NOT NULL DEFAULT 0,
    min_order_qty  INTEGER       NOT NULL DEFAULT 1,    -- MOQ
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ---------- What a buyer wants to buy (posted to all suppliers) ----------
CREATE TABLE buy_requests (
    id            BIGSERIAL     PRIMARY KEY,
    buyer_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id   BIGINT        REFERENCES categories (id) ON DELETE SET NULL,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    quantity      INTEGER       NOT NULL,
    unit          VARCHAR(30)   NOT NULL,
    target_price  NUMERIC(14,2),                        -- optional budget per unit
    needed_by     DATE,
    delivery_city VARCHAR(100),
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSED
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- ---------- A supplier's answer to a buy request ----------
CREATE TABLE quotes (
    id             BIGSERIAL     PRIMARY KEY,
    request_id     BIGINT        NOT NULL REFERENCES buy_requests (id) ON DELETE CASCADE,
    supplier_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id     BIGINT        REFERENCES products (id) ON DELETE SET NULL, -- optional link to a catalog product
    price_per_unit NUMERIC(14,2) NOT NULL,
    quantity       INTEGER       NOT NULL,
    delivery_days  INTEGER,
    message        TEXT,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, WITHDRAWN
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_quote_per_supplier UNIQUE (request_id, supplier_id)
);

-- ---------- Photos and short videos (for a product, a request or a quote) ----------
CREATE TABLE media_files (
    id            BIGSERIAL    PRIMARY KEY,
    file_url      VARCHAR(400) NOT NULL,
    file_type     VARCHAR(10)  NOT NULL,               -- IMAGE, VIDEO
    original_name VARCHAR(255),
    size_bytes    BIGINT,
    product_id    BIGINT       REFERENCES products (id) ON DELETE CASCADE,
    request_id    BIGINT       REFERENCES buy_requests (id) ON DELETE CASCADE,
    quote_id      BIGINT       REFERENCES quotes (id) ON DELETE CASCADE,
    uploaded_by   BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Price negotiation between a buyer and a supplier on one product ----------
CREATE TABLE negotiations (
    id             BIGSERIAL     PRIMARY KEY,
    product_id     BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    buyer_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    supplier_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    current_price  NUMERIC(14,2) NOT NULL,              -- latest proposed price per unit
    current_qty    INTEGER       NOT NULL,              -- latest proposed quantity
    last_offer_by  VARCHAR(20)   NOT NULL,              -- BUYER or SUPPLIER (the other side must answer)
    status         VARCHAR(20)   NOT NULL DEFAULT 'OPEN', -- OPEN, ACCEPTED, REJECTED
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE negotiation_messages (
    id             BIGSERIAL     PRIMARY KEY,
    negotiation_id BIGINT        NOT NULL REFERENCES negotiations (id) ON DELETE CASCADE,
    sender_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action         VARCHAR(20)   NOT NULL,              -- OFFER, COUNTER, ACCEPT, REJECT
    price_per_unit NUMERIC(14,2),
    quantity       INTEGER,
    message        TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ---------- Orders ----------
CREATE TABLE orders (
    id             BIGSERIAL     PRIMARY KEY,
    buyer_id       BIGINT        NOT NULL REFERENCES users (id),  -- no cascade: order history is kept
    supplier_id    BIGINT        NOT NULL REFERENCES users (id),
    product_id     BIGINT        REFERENCES products (id) ON DELETE SET NULL,
    request_id     BIGINT        REFERENCES buy_requests (id) ON DELETE SET NULL,
    quote_id       BIGINT        REFERENCES quotes (id) ON DELETE SET NULL,
    negotiation_id BIGINT        REFERENCES negotiations (id) ON DELETE SET NULL,
    source         VARCHAR(20)   NOT NULL,              -- DIRECT, NEGOTIATION, QUOTE
    product_name   VARCHAR(200)  NOT NULL,              -- copied so history survives product changes
    unit           VARCHAR(30)   NOT NULL,
    quantity       INTEGER       NOT NULL,
    unit_price     NUMERIC(14,2) NOT NULL,
    total_amount   NUMERIC(14,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    note           TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ---------- In-app notifications (the bell icon) ----------
CREATE TABLE notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(200),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Menus and role-based access (managed by admin) ----------
CREATE TABLE menus (
    id           BIGSERIAL    PRIMARY KEY,
    menu_key     VARCHAR(50)  NOT NULL UNIQUE,
    label        VARCHAR(100) NOT NULL,
    path         VARCHAR(100) NOT NULL,
    icon         VARCHAR(50)  NOT NULL,
    sort_order   INTEGER      NOT NULL,
    for_admin    BOOLEAN      NOT NULL DEFAULT FALSE,  -- admins always see these
    for_buyer    BOOLEAN      NOT NULL DEFAULT FALSE,  -- can be given to buyers
    for_supplier BOOLEAN      NOT NULL DEFAULT FALSE,  -- can be given to suppliers
    locked       BOOLEAN      NOT NULL DEFAULT FALSE   -- always on (cannot be switched off)
);

CREATE TABLE role_menu_access (
    id      BIGSERIAL   PRIMARY KEY,
    role    VARCHAR(20) NOT NULL,                       -- BUYER or SUPPLIER
    menu_id BIGINT      NOT NULL REFERENCES menus (id) ON DELETE CASCADE,
    CONSTRAINT uq_role_menu UNIQUE (role, menu_id)
);

-- ---------- Landing page contact form ----------
CREATE TABLE contact_messages (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(200) NOT NULL,
    subject    VARCHAR(200),
    message    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Indexes for the common lookups ----------
CREATE INDEX ix_products_supplier   ON products (supplier_id);
CREATE INDEX ix_products_category   ON products (category_id);
CREATE INDEX ix_requests_buyer      ON buy_requests (buyer_id);
CREATE INDEX ix_quotes_request      ON quotes (request_id);
CREATE INDEX ix_media_product       ON media_files (product_id);
CREATE INDEX ix_media_request       ON media_files (request_id);
CREATE INDEX ix_media_quote         ON media_files (quote_id);
CREATE INDEX ix_negotiations_buyer  ON negotiations (buyer_id);
CREATE INDEX ix_negotiations_supp   ON negotiations (supplier_id);
CREATE INDEX ix_orders_buyer        ON orders (buyer_id);
CREATE INDEX ix_orders_supplier     ON orders (supplier_id);
CREATE INDEX ix_orders_created      ON orders (created_at);
CREATE INDEX ix_notifications_user  ON notifications (user_id, is_read);

-- =====================================================================
-- Seed data
-- =====================================================================
INSERT INTO categories (name) VALUES
    ('Food Packaging'), ('Food & Beverage'), ('Construction Materials'), ('Electrical & Electronics'),
    ('Office Supplies'), ('Furniture'), ('Cleaning & Hygiene'), ('Textiles & Uniforms'),
    ('Machinery & Tools'), ('Medical Supplies'), ('Other');

INSERT INTO menus (menu_key, label, path, icon, sort_order, for_admin, for_buyer, for_supplier, locked) VALUES
    ('dashboard',      'Dashboard',          '/app/dashboard',        'LayoutDashboard', 10,  TRUE,  TRUE,  TRUE,  TRUE),
    ('marketplace',    'Marketplace',        '/app/marketplace',      'Store',           20,  FALSE, TRUE,  TRUE,  FALSE),
    ('my_products',    'My Products',        '/app/my-products',      'Package',         30,  FALSE, FALSE, TRUE,  FALSE),
    ('my_requests',    'My Buying Requests', '/app/my-requests',      'ClipboardList',   40,  FALSE, TRUE,  FALSE, FALSE),
    ('my_quotes',      'My Quotes',          '/app/my-quotes',        'FileText',        50,  FALSE, FALSE, TRUE,  FALSE),
    ('negotiations',   'Negotiations',       '/app/negotiations',     'Handshake',       60,  FALSE, TRUE,  TRUE,  FALSE),
    ('orders',         'Orders',             '/app/orders',           'ShoppingCart',    70,  TRUE,  TRUE,  TRUE,  FALSE),
    ('reports',        'Reports',            '/app/reports',          'BarChart3',       80,  TRUE,  TRUE,  TRUE,  FALSE),
    ('admin_users',    'Users',              '/app/admin/users',      'Users',           100, TRUE,  FALSE, FALSE, TRUE),
    ('admin_products', 'All Products',       '/app/admin/products',   'Boxes',           110, TRUE,  FALSE, FALSE, TRUE),
    ('admin_requests', 'All Requests',       '/app/admin/requests',   'Inbox',           120, TRUE,  FALSE, FALSE, TRUE),
    ('admin_categories','Categories',        '/app/admin/categories', 'Tags',            130, TRUE,  FALSE, FALSE, TRUE),
    ('admin_menus',    'Menu Access',        '/app/admin/menu-access','ShieldCheck',     140, TRUE,  FALSE, FALSE, TRUE),
    ('admin_messages', 'Contact Messages',   '/app/admin/messages',   'Mail',            150, TRUE,  FALSE, FALSE, TRUE),
    ('profile',        'My Profile',         '/app/profile',          'UserCircle',      200, TRUE,  TRUE,  TRUE,  TRUE);

-- By default buyers and suppliers get every menu that applies to them.
INSERT INTO role_menu_access (role, menu_id) SELECT 'BUYER', id FROM menus WHERE for_buyer;
INSERT INTO role_menu_access (role, menu_id) SELECT 'SUPPLIER', id FROM menus WHERE for_supplier;
