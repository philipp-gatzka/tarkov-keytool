DO
$do$
    BEGIN
        IF EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = 'tarkov_keytool') THEN
            RAISE NOTICE 'Role "tarkov_keytool" already exists. Skipping';
        ELSE CREATE ROLE tarkov_keytool;
            ALTER ROLE tarkov_keytool
                LOGIN;
        END IF;
    END
$do$;

CREATE TYPE ACCOUNT_ROLE AS ENUM ('USER', 'ADMIN');

CREATE TYPE GAME_MODE AS ENUM ('PVE', 'PVP');

CREATE TYPE CURRENCY AS ENUM ('$', '€', '₽');

CREATE TABLE account
(
    id        SERIAL         NOT NULL PRIMARY KEY,
    email     VARCHAR(255)   NOT NULL UNIQUE,
    roles     ACCOUNT_ROLE[] NOT NULL DEFAULT ARRAY ['USER']::ACCOUNT_ROLE[],
    game_mode GAME_MODE      NOT NULL DEFAULT 'PVE'::GAME_MODE
);

CREATE TABLE item
(
    id                 SERIAL       NOT NULL PRIMARY KEY,
    tarkov_id          VARCHAR(255) NOT NULL UNIQUE,
    market_id          VARCHAR(255) NOT NULL UNIQUE,
    name               VARCHAR(255) NOT NULL,
    icon_link          VARCHAR(255) NOT NULL,
    wiki_link          VARCHAR(255) NOT NULL,
    market_link        VARCHAR(255) NOT NULL,
    horizontal_slots   INTEGER DEFAULT 1,
    vertical_slots     INTEGER DEFAULT 1,
    trader_currency    CURRENCY     NOT NULL,
    trader_price       INTEGER      NOT NULL,
    pvp_banned_on_flea BOOL,
    pve_banned_on_flea BOOL,
    pvp_flea_price     INTEGER,
    pve_flea_price     INTEGER,
    tags               VARCHAR[]    NOT NULL
);

CREATE TABLE key
(
    item_id INTEGER NOT NULL PRIMARY KEY REFERENCES item,
    uses    INTEGER NOT NULL
);

CREATE VIEW item_grid_view AS
SELECT id                                                              AS item_id,
       name,
       icon_link,
       wiki_link,
       market_link,
       horizontal_slots,
       vertical_slots,
       trader_currency,
       trader_price,
       pvp_banned_on_flea,
       pve_banned_on_flea,
       CASE WHEN NOT pvp_banned_on_flea THEN pvp_flea_price ELSE 0 END AS pvp_flea_price,
       CASE WHEN NOT pve_banned_on_flea THEN pve_flea_price ELSE 0 END AS pve_flea_price,
       tags
FROM item;

CREATE VIEW tag_view AS
SELECT tag,
       REPLACE(tag, '_', ' ') AS clean
FROM item,
     UNNEST(item.tags) AS all_tags(tag)
GROUP BY tag;

CREATE VIEW key_grid_view AS
SELECT item_id,
       uses,
       name,
       icon_link,
       tags
FROM key
         JOIN item ON key.item_id = id;

CREATE VIEW location_view AS
SELECT tag                    AS location,
       REPLACE(tag, '_', ' ') AS clean
FROM key
         JOIN item ON id = key.item_id,
     UNNEST(item.tags) AS all_tags(tag)
WHERE tag != 'Keys'
GROUP BY tag;

CREATE VIEW key_view AS
SELECT item_id,
       uses,
       tarkov_id,
       market_id,
       name,
       icon_link,
       wiki_link,
       market_link,
       horizontal_slots,
       vertical_slots,
       trader_currency,
       trader_price,
       pvp_banned_on_flea,
       pve_banned_on_flea,
       pvp_flea_price,
       pve_flea_price,
       tags
FROM key
         JOIN item ON key.item_id = item.id;

CREATE TABLE key_report
(
    id          SERIAL    NOT NULL PRIMARY KEY,
    key_id      INTEGER   NOT NULL REFERENCES key,
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reported_by INTEGER   NOT NULL REFERENCES account,
    game_mode   GAME_MODE NOT NULL
);

CREATE TABLE loot_report
(
    id            SERIAL  NOT NULL PRIMARY KEY,
    item_id       INTEGER NOT NULL REFERENCES item,
    key_report_id INTEGER NOT NULL REFERENCES key_report,
    count         INTEGER NOT NULL
);

CREATE OR REPLACE FUNCTION item_value(item_id INTEGER, game_mode GAME_MODE) RETURNS INTEGER AS
$$
DECLARE
    f_dollar_tarkov_id TEXT := '5696686a4bdc2da3298b456a';
    f_euro_tarkov_id   TEXT := '569668774bdc2da2298b4568';
    f_flea_price       INTEGER;
    f_trader_price     INTEGER;
    f_trader_currency  TEXT;
    f_banned_on_flea   BOOLEAN;
    f_trader_value     INTEGER;
    f_dollar_price     INTEGER;
    f_euro_price       INTEGER;
BEGIN
    IF game_mode = 'PVP' THEN
        BEGIN
            SELECT pvp_flea_price,
                   pvp_banned_on_flea,
                   trader_price,
                   trader_currency,
                   (SELECT trader_price FROM item WHERE tarkov_id = f_dollar_tarkov_id) AS dollar_price,
                   (SELECT trader_price FROM item WHERE tarkov_id = f_euro_tarkov_id)   AS euro_price
            INTO f_flea_price, f_banned_on_flea, f_trader_price, f_trader_currency, f_dollar_price, f_euro_price
            FROM item
            WHERE id = item_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE EXCEPTION 'Item with id % does not exist.', item_id;
        END;
    ELSE
        BEGIN
            SELECT pve_flea_price,
                   pve_banned_on_flea,
                   trader_price,
                   trader_currency,
                   (SELECT trader_price FROM item WHERE tarkov_id = f_dollar_tarkov_id) AS dollar_price,
                   (SELECT trader_price FROM item WHERE tarkov_id = f_euro_tarkov_id)   AS euro_price
            INTO f_flea_price, f_banned_on_flea, f_trader_price, f_trader_currency, f_dollar_price, f_euro_price
            FROM item
            WHERE id = item_id;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE EXCEPTION 'Item with id % does not exist.', item_id;
        END;
    END IF;

    IF f_banned_on_flea THEN
        IF f_trader_currency = '$' THEN
            SELECT trader_price INTO f_dollar_price FROM item WHERE tarkov_id = f_dollar_tarkov_id;
            RETURN f_trader_price * f_dollar_price;
        ELSIF f_trader_currency = '€' THEN
            SELECT trader_price INTO f_euro_price FROM item WHERE tarkov_id = f_euro_tarkov_id;
            RETURN f_trader_price * f_euro_price;
        ELSE
            RETURN f_trader_price;
        END IF;
    ELSE
        f_trader_value := f_trader_price;
        IF f_trader_currency = '$' THEN
            SELECT trader_price INTO f_dollar_price FROM item WHERE tarkov_id = f_dollar_tarkov_id;
            f_trader_value := f_trader_value * f_dollar_price;
        ELSIF f_trader_currency = '€' THEN
            SELECT trader_price INTO f_euro_price FROM item WHERE tarkov_id = f_euro_tarkov_id;
            f_trader_value := f_trader_value * f_euro_price;
        END IF;

        IF COALESCE(f_flea_price, 0) > f_trader_value THEN
            RETURN f_flea_price;
        ELSE
            RETURN f_trader_value;
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE VIEW loot_report_view AS
SELECT loot_report.item_id,
       loot_report.count,
       loot_report.key_report_id,
       item.name,
       item.icon_link,
       item.horizontal_slots,
       item.vertical_slots,
       item_value(item_id := loot_report.item_id, game_mode := 'PVP') * count AS pvp_value,
       item_value(item_id := loot_report.item_id, game_mode := 'PVE') * count AS pve_value
FROM loot_report
         JOIN item ON loot_report.item_id = item.id;

CREATE VIEW key_report_view AS
SELECT key_report.id,
       key_report.game_mode,
       key_report.key_id,
       key_report.reported_at,
       key_report.reported_by,
       key.uses,
       item.name,
       item.icon_link,
       item.tags,
       CASE WHEN game_mode = 'PVP' THEN pvp_flea_price ELSE pve_flea_price END                              AS flea_price,
       SUM(CASE WHEN game_mode = 'PVP' THEN loot_report_view.pvp_value ELSE loot_report_view.pve_value END) AS value
FROM key_report
         JOIN key ON key_report.key_id = key.item_id
         JOIN item ON key.item_id = item.id
         JOIN loot_report_view ON key_report.id = loot_report_view.key_report_id
GROUP BY key_report.id,
         key_report.game_mode,
         key_report.key_id,
         key_report.reported_at,
         key_report.reported_by,
         key.uses,
         item.name,
         item.icon_link,
         item.tags,
         CASE WHEN game_mode = 'PVP' THEN pvp_flea_price ELSE pve_flea_price END;

CREATE TABLE wipe
(
    id        SERIAL       NOT NULL PRIMARY KEY,
    wipe_date timestamp         NOT NULL,
    version   VARCHAR(255) NOT NULL
);

GRANT INSERT, SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO tarkov_keytool;

GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO tarkov_keytool;
