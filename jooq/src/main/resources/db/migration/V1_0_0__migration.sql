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
    reported_by INTEGER   NOT NULL REFERENCES account
);

CREATE TABLE loot_report
(
    id            SERIAL  NOT NULL PRIMARY KEY,
    item_id       INTEGER NOT NULL REFERENCES item,
    key_report_id INTEGER NOT NULL REFERENCES key_report,
    count         INTEGER NOT NULL
);