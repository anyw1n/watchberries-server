CREATE TABLE db.users
(
    id TEXT PRIMARY KEY,
    key UUID NOT NULL UNIQUE,
    token TEXT NOT NULL,
    last_sync TIMESTAMP NOT NULL
);

CREATE TABLE db.skus
(
    id SERIAL PRIMARY KEY,
    "user" TEXT NOT NULL references db.users(id) ON DELETE CASCADE,
    sku INT NOT NULL,
    UNIQUE ("user", sku)
);