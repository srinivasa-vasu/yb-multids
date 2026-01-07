CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS kvinfo
(
    key           uuid PRIMARY KEY,
    value         text
) SPLIT INTO 1 TABLETS;