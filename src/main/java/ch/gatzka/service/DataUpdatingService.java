package ch.gatzka.service;

import static ch.gatzka.Tables.ITEM;
import static ch.gatzka.Tables.KEY;

import ch.gatzka.FetchKeysQuery;
import ch.gatzka.ItemRepository;
import ch.gatzka.KeyRepository;
import ch.gatzka.Utils;
import ch.gatzka.enums.Currency;
import ch.gatzka.enums.GameMode;
import ch.gatzka.pojo.Item;
import ch.gatzka.tables.records.ItemRecord;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataUpdatingService {

  private final ItemRepository itemRepository;

  private final ApiService apiService;

  private final KeyRepository keyRepository;

  @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
  public void updateBaseData() {
    updatePVPData();
    updatePVEData();
    updateSlots();
    updateKeys();
  }

  private void updateKeys() {
    final int[] stats = {0, 0};
    Utils.executeProgressive("Update keys", apiService.readKeys(), key -> {
      if (itemRepository.exists(ITEM.TARKOV_ID.eq(key.id))) {
        ItemRecord item = itemRepository.get(ITEM.TARKOV_ID.eq(key.id));
        if (keyRepository.exists(KEY.ITEM_ID.eq(item.getId()))) {
          updateKey(item, key);
          stats[0]++;
        } else {
          insertKey(item, key);
          stats[1]++;
        }
      }
    });
    log.info("Updated {} keys, inserted {} keys", stats[1], stats[0]);
  }

  private void insertKey(ItemRecord item, FetchKeysQuery.Item key) {
    keyRepository.insert(entity -> entity.setItemId(item.getId()).setUses(key.properties.onItemPropertiesKey.uses));
  }

  private void updateKey(ItemRecord item, FetchKeysQuery.Item key) {
    keyRepository.update(entity -> entity.setUses(key.properties.onItemPropertiesKey.uses), KEY.ITEM_ID.eq(item.getId()));
  }

  private void updateSlots() {
    final int[] stats = {0};
    Utils.executeProgressive("Update item slots", apiService.readItemSlots()
        .stream()
        .filter(item -> item.height * item.width != 1)
        .toList(), item -> stats[0] += itemRepository.update(entity -> entity.setHorizontalSlots(item.width)
        .setVerticalSlots(item.height), ITEM.TARKOV_ID.eq(item.id)));
    log.info("Updated {} item slots", stats[0]);
  }

  private void updatePVPData() {
    updateBaseData(apiService.readPVPItems(), GameMode.PVP);
  }

  private void updatePVEData() {
    updateBaseData(apiService.readPVEItems(), GameMode.PVE);
  }

  private void updateBaseData(Item[] items, GameMode gameMode) {
    final int[] stats = {0, 0};
    Utils.executeProgressive("Update PVP Data", Arrays.stream(items).toList(), item -> {
      if (itemRepository.exists(ITEM.TARKOV_ID.eq(item.getBsgId()))) {
        updateItemBaseData(item, gameMode);
        stats[1]++;
      } else {
        insertItemBaseData(item, gameMode);
        stats[0]++;
      }
    });
    log.info("Updated {} items, inserted {} items", stats[1], stats[0]);
  }

  private void updateItemBaseData(Item item, GameMode gameMode) {
    itemRepository.update(entity -> entity.setName(item.getName())
        .setIconLink(item.getIcon().trim().isEmpty() ? item.getImgBig() : item.getIcon())
        .setWikiLink(item.getWikiLink())
        .setMarketLink(item.getLink())
        .setTraderCurrency(Currency.lookupLiteral(item.getTraderPriceCur()))
        .setTraderPrice(item.getTraderPrice())
        .setTags(item.getTags()), ITEM.TARKOV_ID.eq(item.getBsgId()));
    updateItemPrices(item, gameMode);
  }

  private void insertItemBaseData(Item item, GameMode gameMode) {
    itemRepository.insert(entity -> entity.setTarkovId(item.getBsgId())
        .setMarketId(item.getUid())
        .setName(item.getName())
        .setIconLink(item.getIcon().trim().isEmpty() ? item.getImgBig() : item.getIcon())
        .setWikiLink(item.getWikiLink())
        .setMarketLink(item.getLink())
        .setTraderCurrency(Currency.lookupLiteral(item.getTraderPriceCur()))
        .setTraderPrice(item.getTraderPrice())
        .setTags(item.getTags()));
    updateItemPrices(item, gameMode);
  }

  private void updateItemPrices(Item item, GameMode gameMode) {
    if (gameMode == GameMode.PVP) {
      itemRepository.update(entity -> entity.setPvpBannedOnFlea(item.getBannedOnFlea())
          .setPvpFleaPrice(Boolean.TRUE.equals(item.getBannedOnFlea()) ? null : item.getAvg24hPrice()), ITEM.TARKOV_ID.eq(item.getBsgId()));
    } else {
      itemRepository.update(entity -> entity.setPveBannedOnFlea(item.getBannedOnFlea())
          .setPveFleaPrice(Boolean.TRUE.equals(item.getBannedOnFlea()) ? null : item.getAvg24hPrice()), ITEM.TARKOV_ID.eq(item.getBsgId()));
    }
  }

}
