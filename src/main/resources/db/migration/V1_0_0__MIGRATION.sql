DO
$do$
    BEGIN
        IF EXISTS(SELECT FROM pg_catalog.pg_roles WHERE rolname = 'tarkov_keytool') THEN
            RAISE NOTICE 'Role "tarkov_keytool" already exists. Skipping';
        ELSE CREATE ROLE tarkov_keytool;
        END IF;
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
    id               SERIAL         NOT NULL PRIMARY KEY,
    market_id        VARCHAR(255)   NOT NULL UNIQUE,
    tarkov_id        VARCHAR(255)   NOT NULL UNIQUE,
    name             VARCHAR(255)   NOT NULL,
    icon_link        VARCHAR(255)   NOT NULL,
    market_link      VARCHAR(255)   NOT NULL,
    wiki_link        VARCHAR(255)   NOT NULL,
    slots            INTEGER        NOT NULL,
    vertical_slots   INTEGER        not null default 1,
    horizontal_slots INTEGER        not null default 1,
    tags             VARCHAR(255)[] NOT NULL,
    last_update      TIMESTAMP      NOT NULL,
    trader_name      VARCHAR(255)   NOT NULL,
    trader_price     INTEGER        NOT NULL,
    trader_currency  CHAR           NOT NULL
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
SELECT item_price.item_id,
       item_price.mode,
       item_price.flea_price,
       item_price.timestamp,
       item_price.banned_on_flea
FROM item_price
WHERE timestamp = (SELECT MAX(inner_item_prices.timestamp)
                   FROM item_price inner_item_prices
                   WHERE item_price.item_id = inner_item_prices.item_id
                     AND item_price.mode = inner_item_prices.mode);

CREATE OR REPLACE VIEW item_grid_view AS
SELECT item.id                         AS item_id,
       item.name,
       item.icon_link,
       item.market_link,
       item.wiki_link,
       item.vertical_slots,
       item.horizontal_slots,
       item.tags,
       item.trader_name,
       item.trader_price,
       item.trader_currency,
       latest_pvp_price.flea_price     AS pvp_price,
       latest_pve_price.flea_price     AS pve_price,
       latest_pvp_price.banned_on_flea AS pvp_flea_banned,
       latest_pve_price.banned_on_flea AS pve_flea_banned
FROM item
         JOIN latest_item_price_view latest_pvp_price
              ON item.id = latest_pvp_price.item_id AND latest_pvp_price.mode = 'PVP'
         JOIN latest_item_price_view latest_pve_price
              ON item.id = latest_pve_price.item_id AND latest_pve_price.mode = 'PVE';

create or replace view key_grid_view as
SELECT item.id                                AS item_id,
       item.name,
       item.icon_link,
       item.trader_price,
       item.trader_price / key.uses           as trader_price_per_use,
       item.trader_currency,
       key.uses,
       latest_pvp_price.flea_price            AS pvp_price,
       latest_pvp_price.flea_price / key.uses AS pvp_price_per_use,
       latest_pve_price.flea_price            AS pve_price,
       latest_pve_price.flea_price / key.uses AS pve_price_per_use,
       latest_pvp_price.banned_on_flea        AS pvp_flea_banned,
       latest_pve_price.banned_on_flea        AS pve_flea_banned
FROM key
         join item on key.item_id = item.id
         JOIN latest_item_price_view latest_pvp_price
              ON item.id = latest_pvp_price.item_id AND latest_pvp_price.mode = 'PVP'::GAME_MODE
         JOIN latest_item_price_view latest_pve_price
              ON item.id = latest_pve_price.item_id AND latest_pve_price.mode = 'PVE'::GAME_MODE;

CREATE OR REPLACE VIEW tag AS
SELECT tag,
       REPLACE(tag, '_', ' ') AS clean
FROM item,
     UNNEST(item.tags) AS all_tags(tag)
GROUP BY tag;

create view key_view as
select item.id,
       item.market_id,
       item.tarkov_id,
       item.name,
       item.icon_link,
       item.market_link,
       item.wiki_link,
       item.slots,
       item.vertical_slots,
       item.horizontal_slots,
       item.tags,
       item.last_update,
       item.trader_name,
       item.trader_price,
       item.trader_currency,
       key.uses
from key
         join item on key.item_id = item.id;

CREATE OR REPLACE FUNCTION get_item_value(input_item_id integer, game_mode game_mode)
    RETURNS NUMERIC AS
$$
DECLARE
    dollar_price         numeric;
    item_trader_price    NUMERIC;
    item_trader_currency varchar;
    item_flea_price      numeric;
    item_banned_on_flea  bool;
    result               NUMERIC;
BEGIN
    select item.trader_currency into item_trader_currency from item where item.id = get_item_value.input_item_id;
    select item.trader_price into item_trader_price from item where item.id = get_item_value.input_item_id;

    IF item_trader_currency = '$' THEN
        select item.trader_price into dollar_price from item where item.tarkov_id = '5696686a4bdc2da3298b456a';
        item_trader_price := item_trader_price * dollar_price;
    end if;

    select latest_item_price_view.banned_on_flea
    into item_banned_on_flea
    from item
             join latest_item_price_view on item.id = latest_item_price_view.item_id
    where item.id = get_item_value.input_item_id;

    select latest_item_price_view.flea_price
    into item_flea_price
    from item
             join latest_item_price_view on item.id = latest_item_price_view.item_id
    where item.id = get_item_value.input_item_id
      and latest_item_price_view.mode = get_item_value.game_mode;

    IF item_banned_on_flea or item_flea_price < item_trader_price then
        result := item_trader_price;
    else
        result := item_flea_price;
    end if;

    RETURN result;
END;
$$ LANGUAGE plpgsql;

create table key_report
(
    id          serial    not null primary key,
    key_id      integer   not null references key,
    reported_at timestamp not null default current_timestamp,
    account_id  integer   not null references account
);

create table loot_report
(
    key_report_id integer not null references key_report,
    item_id       integer not null references item,
    count         integer not null
);

GRANT DELETE , INSERT, SELECT, UPDATE ON ALL TABLES IN SCHEMA public TO tarkov_keytool;

GRANT SELECT, USAGE ON ALL SEQUENCES IN SCHEMA public TO tarkov_keytool;
