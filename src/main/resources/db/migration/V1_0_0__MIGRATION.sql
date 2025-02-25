DO
$do$
    BEGIN
        IF EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = 'tarkov_keytool') THEN RAISE NOTICE 'Role "tarkov_keytool" already exists. Skipping'; ELSE CREATE ROLE tarkov_keytool; END IF;
    END
$do$;

CREATE TYPE ACCOUNT_ROLE AS ENUM ('USER', 'ADMIN');

CREATE TYPE GAME_MODE AS ENUM ('PVE', 'PVP');

CREATE TABLE account
(
    id    SERIAL         NOT NULL PRIMARY KEY,
    email VARCHAR(255)   NOT NULL UNIQUE,
    roles ACCOUNT_ROLE[] NOT NULL,
    mode  GAME_MODE      NOT NULL
);

CREATE TABLE item
(
    id              SERIAL         NOT NULL PRIMARY KEY,
    market_id       VARCHAR(255)   NOT NULL UNIQUE,
    tarkov_id       VARCHAR(255)   NOT NULL UNIQUE,
    name            VARCHAR(255)   NOT NULL,
    icon_link       VARCHAR(255)   NOT NULL,
    market_link     VARCHAR(255)   NOT NULL,
    wiki_link       VARCHAR(255)   NOT NULL,
    slots           INTEGER        NOT NULL,
    tags            VARCHAR(255)[] NOT NULL,
    last_update     TIMESTAMP      NOT NULL,
    trader_name     VARCHAR(255)   NOT NULL,
    trader_price    INTEGER        NOT NULL,
    trader_currency CHAR           NOT NULL
);

CREATE TABLE item_price
(
    item_id        INTEGER   NOT NULL REFERENCES item,
    banned_on_flea BOOL      NOT NULL,
    flea_price     INTEGER   NOT NULL,
    mode           GAME_MODE NOT NULL,
    timestamp      TIMESTAMP NOT NULL,
    PRIMARY KEY (item_id, timestamp, mode)
);

CREATE TABLE key
(
    item_id INTEGER NOT NULL REFERENCES item,
    uses    INTEGER NOT NULL,
    PRIMARY KEY (item_id)
);

CREATE OR REPLACE VIEW latest_item_price_view AS
SELECT
    item_price.item_id,
    item_price.mode,
    item_price.flea_price,
    item_price.timestamp,
    item_price.banned_on_flea
FROM
    item_price
WHERE
    timestamp = (SELECT MAX(inner_item_prices.timestamp) FROM item_price inner_item_prices WHERE item_price.item_id = inner_item_prices.item_id AND item_price.mode = inner_item_prices.mode);

CREATE OR REPLACE VIEW item_grid_view AS
SELECT
    item.id                         AS item_id,
    item.name,
    item.icon_link,
    item.market_link,
    item.wiki_link,
    item.slots,
    item.tags,
    item.trader_name,
    item.trader_price,
    item.trader_currency,
    latest_pvp_price.flea_price     AS pvp_price,
    latest_pve_price.flea_price     AS pve_price,
    latest_pvp_price.banned_on_flea AS pvp_flea_banned,
    latest_pve_price.banned_on_flea AS pve_flea_banned
FROM
    item
        JOIN latest_item_price_view latest_pvp_price
             ON item.id = latest_pvp_price.item_id AND latest_pvp_price.mode = 'PVP'
        JOIN latest_item_price_view latest_pve_price
             ON item.id = latest_pve_price.item_id AND latest_pve_price.mode = 'PVE';

CREATE OR REPLACE VIEW tag AS
SELECT
    tag,
    REPLACE(tag, '_', ' ') AS clean
FROM
    item,
    UNNEST(item.tags) AS all_tags(tag)
GROUP BY
    tag;

GRANT DELETE , INSERT, SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO tarkov_keytool;

GRANT SELECT, USAGE ON ALL SEQUENCES IN SCHEMA public TO tarkov_keytool;
