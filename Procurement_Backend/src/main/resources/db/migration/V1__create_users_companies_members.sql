-- V1: identity and tenancy foundation (blueprint section 5 + Appendix B).
-- users            = login identity (platform role only: USER or PLATFORM_ADMIN)
-- companies        = buyer or supplier organization (the tenant boundary)
-- company_members  = which user belongs to which company, with which company role

CREATE TABLE users (
    id                UUID         PRIMARY KEY,
    email             VARCHAR(320) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    full_name         VARCHAR(150) NOT NULL,
    platform_role     VARCHAR(30)  NOT NULL DEFAULT 'USER',
    status            VARCHAR(30)  NOT NULL,
    email_verified_at TIMESTAMPTZ,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    -- The application lower-cases emails; the database enforces it so uniqueness is case-insensitive.
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_platform_role CHECK (platform_role IN ('USER', 'PLATFORM_ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'DISABLED'))
);

CREATE TABLE companies (
    id              UUID         PRIMARY KEY,
    legal_name      VARCHAR(200) NOT NULL,
    display_name    VARCHAR(200),
    company_type    VARCHAR(20)  NOT NULL,
    country_code    VARCHAR(2)   NOT NULL,
    registration_no VARCHAR(100),
    status          VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_companies_type CHECK (company_type IN ('BUYER', 'SUPPLIER')),
    CONSTRAINT ck_companies_country CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_companies_status CHECK (status IN ('ACTIVE', 'PENDING_REVIEW', 'SUSPENDED'))
);

-- Registration numbers are unique per country (country-aware uniqueness, blueprint 5.1).
CREATE UNIQUE INDEX uq_companies_country_registration
    ON companies (country_code, registration_no)
    WHERE registration_no IS NOT NULL;

CREATE TABLE company_members (
    id         UUID        PRIMARY KEY,
    company_id UUID        NOT NULL REFERENCES companies (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    role       VARCHAR(30) NOT NULL,
    status     VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_company_member UNIQUE (company_id, user_id),
    CONSTRAINT ck_company_members_role CHECK (role IN ('BUYER_OWNER', 'BUYER_MEMBER', 'SUPPLIER_OWNER', 'SUPPLIER_MEMBER')),
    CONSTRAINT ck_company_members_status CHECK (status IN ('ACTIVE', 'INVITED', 'REMOVED'))
);

CREATE INDEX ix_company_members_user_id ON company_members (user_id);
CREATE INDEX ix_companies_status ON companies (status);
