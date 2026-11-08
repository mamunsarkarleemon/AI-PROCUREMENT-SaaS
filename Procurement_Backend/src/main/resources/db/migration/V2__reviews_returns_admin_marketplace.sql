-- =====================================================================
-- V2: product reviews and reactions, partial receiving with returns,
--     marketplace ratings/sales numbers, marketplace menu for admins.
-- Only adds to V1. Existing rows keep working.
-- =====================================================================

-- ---------- Numbers shown on product cards (kept up to date by the services) ----------
ALTER TABLE products
    ADD COLUMN rating_avg   NUMERIC(3,2) NOT NULL DEFAULT 0,  -- average star rating, 0 = no reviews yet
    ADD COLUMN rating_count INTEGER      NOT NULL DEFAULT 0,  -- number of reviews
    ADD COLUMN sold_qty     INTEGER      NOT NULL DEFAULT 0;  -- units actually received by buyers

-- ---------- Partial receiving: buyer accepts some units, the rest go back ----------
ALTER TABLE orders
    ADD COLUMN received_qty        INTEGER,                                 -- good units accepted (set when delivered)
    ADD COLUMN returned_qty        INTEGER     NOT NULL DEFAULT 0,          -- damaged units sent back
    ADD COLUMN return_reason       TEXT,
    ADD COLUMN return_status       VARCHAR(20) NOT NULL DEFAULT 'NONE',     -- NONE, PENDING, CONFIRMED
    ADD COLUMN return_confirmed_at TIMESTAMP;

-- Orders delivered before this version were received in full.
UPDATE orders SET received_qty = quantity WHERE status = 'DELIVERED';

UPDATE products p
SET sold_qty = s.qty
FROM (SELECT product_id, SUM(received_qty) AS qty
      FROM orders
      WHERE status = 'DELIVERED' AND product_id IS NOT NULL
      GROUP BY product_id) s
WHERE s.product_id = p.id;

-- ---------- A buyer's rating and review of a product they received ----------
CREATE TABLE product_reviews (
    id         BIGSERIAL  PRIMARY KEY,
    product_id BIGINT     NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    buyer_id   BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_id   BIGINT     REFERENCES orders (id) ON DELETE SET NULL,  -- the delivered order behind this review
    rating     INTEGER    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP  NOT NULL DEFAULT now(),
    updated_at TIMESTAMP  NOT NULL DEFAULT now(),
    UNIQUE (product_id, buyer_id)                                     -- one review per buyer per product
);

-- ---------- Reactions from other users on a review ----------
CREATE TABLE review_reactions (
    id         BIGSERIAL   PRIMARY KEY,
    review_id  BIGINT      NOT NULL REFERENCES product_reviews (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reaction   VARCHAR(20) NOT NULL,                                  -- LIKE, LOVE, HELPFUL, DISLIKE
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (review_id, user_id)                                       -- one reaction per user per review
);

CREATE INDEX ix_reviews_product    ON product_reviews (product_id);
CREATE INDEX ix_reactions_review   ON review_reactions (review_id);
CREATE INDEX ix_orders_product     ON orders (product_id);
CREATE INDEX ix_products_rating    ON products (rating_avg DESC, rating_count DESC);
CREATE INDEX ix_products_sold      ON products (sold_qty DESC);

-- ---------- Admins can open the marketplace too ----------
UPDATE menus SET for_admin = TRUE WHERE menu_key = 'marketplace';
