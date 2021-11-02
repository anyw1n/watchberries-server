DROP TABLE db.users, db.skus CASCADE;

CREATE TABLE db.users
(
    id SERIAL PRIMARY KEY,
    key UUID NOT NULL UNIQUE,
    token TEXT NOT NULL,
    last_sync TIMESTAMP NOT NULL
);

CREATE TABLE db.skus
(
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL references db.users(id) ON DELETE CASCADE,
    sku INT NOT NULL,
    UNIQUE (user_id, sku)
);
