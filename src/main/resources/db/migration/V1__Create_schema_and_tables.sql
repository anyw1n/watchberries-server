CREATE SCHEMA db;

CREATE TABLE db.products
(
    sku INT PRIMARY KEY,
    brand TEXT NOT NULL,
    title TEXT NOT NULL
);

CREATE TABLE db.prices
(
    id SERIAL PRIMARY KEY,
    sku INT NOT NULL references db.products(sku) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL,
    price INT NOT NULL,
    UNIQUE (sku, timestamp)
);