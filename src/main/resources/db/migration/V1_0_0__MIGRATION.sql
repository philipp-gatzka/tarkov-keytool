DO
$do$
    BEGIN
        if exists(select from pg_catalog.pg_roles where rolname = 'tarkov_keytool') then
            RAISE notice 'Role "tarkov_keytool" already exists. Skipping';
        else create role tarkov_keytool;
        end if;
    END
$do$;

create type account_role as enum ('USER', 'ADMIN');

create type game_mode as enum ('PVE', 'PVP');

create table account
(
    id    serial         not null primary key,
    email varchar(255)   not null unique,
    roles account_role[] not null,
    mode  game_mode      not null
);

create table item
(
    id              serial         not null primary key,
    market_id       varchar(255)   not null unique,
    tarkov_id       varchar(255)   not null unique,
    name            varchar(255)   not null,
    icon_link       varchar(255)   not null,
    market_link     varchar(255)   not null,
    wiki_link       varchar(255)   not null,
    slots           integer        not null,
    tags            varchar(255)[] not null,
    last_update     timestamp      not null,
    trader_name     varchar(255)   not null,
    trader_price    integer        not null,
    trader_currency char           not null
);

create table item_price
(
    item_id        integer   not null references item,
    banned_on_flea bool      not null,
    flea_price     integer   not null,
    mode           game_mode not null,
    timestamp      timestamp not null,
    primary key (item_id, timestamp, mode)
);

create or replace view latest_item_price_view as
SELECT item_price.item_id, item_price.mode, item_price.flea_price, item_price.timestamp, item_price.banned_on_flea
FROM item_price
WHERE timestamp = (SELECT MAX(inner_item_prices.timestamp)
                   FROM item_price inner_item_prices
                   WHERE item_price.item_id = inner_item_prices.item_id
                     AND item_price.mode = inner_item_prices.mode);

create or replace view item_grid_view as
select item.id                         as item_id,
       item.name,
       item.icon_link,
       item.market_link,
       item.wiki_link,
       item.slots,
       item.tags,
       item.trader_name,
       item.trader_price,
       item.trader_currency,
       latest_pvp_price.flea_price     as pvp_price,
       latest_pve_price.flea_price     as pve_price,
       latest_pvp_price.banned_on_flea as pvp_flea_banned,
       latest_pve_price.banned_on_flea as pve_flea_banned
from item
         join latest_item_price_view latest_pvp_price
              on item.id = latest_pvp_price.item_id and latest_pvp_price.mode = 'PVP'
         join latest_item_price_view latest_pve_price
              on item.id = latest_pve_price.item_id and latest_pve_price.mode = 'PVE';

grant delete, insert, select, update on all tables in schema public to tarkov_keytool;

grant select, usage on all sequences in schema public to tarkov_keytool;
