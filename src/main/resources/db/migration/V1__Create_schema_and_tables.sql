CREATE SCHEMA db;

CREATE TABLE db.users
(
    id UUID PRIMARY KEY
);

CREATE TABLE db.skus
(
    id SERIAL PRIMARY KEY,
    "user" UUID references db.users(id) NOT NULL,
    sku INT NOT NULL,
    CONSTRAINT user_sku UNIQUE ("user", sku)
);

CREATE TABLE db.products
(
    sku INT PRIMARY KEY,
    brand TEXT NOT NULL,
    title TEXT NOT NULL
);

CREATE TABLE db.prices
(
    id SERIAL PRIMARY KEY NOT NULL,
    sku INT references db.products(sku) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    price INT NOT NULL
);