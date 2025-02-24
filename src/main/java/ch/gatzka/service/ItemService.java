package ch.gatzka.service;

import ch.gatzka.enums.GameMode;
import ch.gatzka.tables.records.ItemRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static ch.gatzka.Tables.ITEM;
import static ch.gatzka.Tables.ITEM_PRICE;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final DSLContext dslContext;

    public boolean existsByTarkovId(String tarkovId) {
        return dslContext.fetchExists(ITEM, ITEM.TARKOV_ID.eq(tarkovId));
    }

    public int createItem(String marketId, String tarkovId, String name, String iconLink, String marketLink, String wikiLink, int slots, String[] tags, int traderPrice, String traderName, String traderCurrency, LocalDateTime lastUpdate) {
        return dslContext.insertInto(ITEM)
                .set(dslContext.newRecord(ITEM)
                        .setMarketId(marketId)
                        .setTarkovId(tarkovId)
                        .setName(name)
                        .setIconLink(iconLink)
                        .setWikiLink(wikiLink)
                        .setMarketLink(marketLink)
                        .setSlots(slots).setTags(tags)
                        .setLastUpdate(lastUpdate)
                        .setTraderPrice(traderPrice)
                        .setTraderCurrency(traderCurrency)
                        .setTraderName(traderName))
                .returningResult(ITEM.ID).fetchSingleInto(Integer.class);
    }

    public ItemRecord getByTarkovId(String tarkovId) {
        return dslContext.fetchSingle(ITEM, ITEM.TARKOV_ID.eq(tarkovId));
    }

    public boolean doesPriceExist(int itemId, GameMode gameMode, LocalDateTime timestamp) {
        return dslContext.fetchExists(ITEM_PRICE, ITEM_PRICE.ITEM_ID.eq(itemId), ITEM_PRICE.MODE.eq(gameMode), ITEM_PRICE.TIMESTAMP.eq(timestamp));
    }

    public void insertItemPrice(int itemId, GameMode gameMode, int fleaPrice, boolean bannedOnFlea, LocalDateTime timestamp) {
        dslContext.insertInto(ITEM_PRICE)
                .set(dslContext.newRecord(ITEM_PRICE)
                        .setItemId(itemId)
                        .setFleaPrice(fleaPrice)
                        .setMode(gameMode)
                        .setTimestamp(timestamp)
                        .setBannedOnFlea(bannedOnFlea))
                .execute();
    }
}
