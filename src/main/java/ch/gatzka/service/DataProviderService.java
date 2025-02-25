package ch.gatzka.service;

import static ch.gatzka.Tables.KEY;

import ch.gatzka.FetchKeysQuery;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemPriceRepository;
import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.tables.records.ItemRecord;
import com.apollographql.java.client.ApolloClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProviderService {

  private static final String PVE_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/pve/items/all?x-api-key={API_KEY}";

  private static final String PVP_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/items/all?x-api-key={API_KEY}";

  private final ItemRepository itemRepository;

  private final KeyRepository keyRepository;

  private final ItemPriceRepository itemPriceRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Value("${tarkov-market.api.key}")
  private String tarkovMarketApiKey;

  private void updateData() {
    updateData(getPVEData(), GameMode.PVE);
    updateData(getPVPData(), GameMode.PVP);
  }

  private void updateData(ItemData[] itemData, GameMode gameMode) {
    log.info("Starting to update {} items for game mode: {}", itemData.length, gameMode);
    long startTime = System.currentTimeMillis();
    int counter = 0;
    int lastLoggedProgress = -1;

    int totalInsert = 0;
    int totalUpdate = 0;
    int totalPriceInsert = 0;

    for (ItemData data : itemData) {
      counter++;
      if (counter % 10 == 0) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        double progress = (double) counter / itemData.length;
        int roundedProgress = (int) Math.round(progress * 100);

        if (roundedProgress != lastLoggedProgress) {
          lastLoggedProgress = roundedProgress;
          long estimatedRemaining = (long) ((elapsedTime / progress) - elapsedTime);
          log.info("Progress: {}/{} ({}%), Estimated time remaining: {} ms", counter, itemData.length, roundedProgress, estimatedRemaining);
        }
      }

      Instant instant = data.updated.isEmpty() ? Instant.now() : Instant.parse(data.updated);
      LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

      int itemId;
      String iconLink = data.icon != null && !data.icon.trim().isEmpty() ? data.icon : data.imgBig;
      if (itemRepository.existsByTarkovId(data.bsgId)) {

        ItemRecord item = itemRepository.findByTarkovId(data.bsgId);
        if (item.getLastUpdate().isBefore(updated)) {

          itemRepository.update(entity -> entity.setName(data.name)
                                                .setIconLink(iconLink)
                                                .setWikiLink(data.wikiLink)
                                                .setMarketLink(data.link)
                                                .setSlots(data.slots)
                                                .setTags(data.tags)
                                                .setLastUpdate(updated)
                                                .setTraderPrice(data.traderPrice)
                                                .setTraderCurrency(data.traderPriceCur)
                                                .setTraderName(data.traderName), item.getId());

          log.debug("Updated item {}", item.getId());

          totalUpdate++;
        }
        itemId = item.getId();
      } else {
        itemId = itemRepository.insert(entity -> entity.setTarkovId(data.bsgId)
                                                       .setMarketId(data.uid)
                                                       .setName(data.name)
                                                       .setIconLink(iconLink)
                                                       .setWikiLink(data.wikiLink)
                                                       .setMarketLink(data.link)
                                                       .setSlots(data.slots)
                                                       .setTags(data.tags)
                                                       .setLastUpdate(updated)
                                                       .setTraderPrice(data.traderPrice)
                                                       .setTraderCurrency(data.traderPriceCur)
                                                       .setTraderName(data.traderName));
        log.debug("Inserted item {}", itemId);

        totalInsert++;
      }

      totalPriceInsert += insertItemPrice(data, itemId, gameMode, updated);
    }

    log.info("Inserted {} items, Updated {} items, Inserted {} item prices", totalInsert, totalUpdate, totalPriceInsert);
  }

  private int insertItemPrice(ItemData data, int itemId, GameMode gameMode, LocalDateTime timestamp) {
    if (!itemPriceRepository.existsByItemIdAndGameModeAndTimestamp(itemId, gameMode, timestamp)) {
      return itemPriceRepository.insert(entity -> entity.setItemId(itemId)
                                                        .setMode(gameMode)
                                                        .setTimestamp(timestamp)
                                                        .setFleaPrice(data.avg7daysPrice)
                                                        .setBannedOnFlea(data.bannedOnFlea));
    }
    return 0;
  }

  private void fetchKeys(int retries) {
    log.info("Fetching keys from Tarkov API");
    try (ApolloClient service = new ApolloClient.Builder().serverUrl("https://api.tarkov.dev/graphql").build()) {
      service.query(new FetchKeysQuery()).enqueue(response -> {
        if (response.exception != null || response.data == null) {
          if (retries > 0) {
            fetchKeys(retries - 1);
          } else {
            log.error("Error updating key data", response.exception);
          }
          return;
        }

        List<FetchKeysQuery.Item> data = response.data.items.stream().filter(Objects::nonNull).toList();
        log.info("Fetched {} keys", data.size());

        int updated = 0;
        int inserted = 0;
        for (FetchKeysQuery.Item itemData : data) {
          if (itemRepository.existsByTarkovId(itemData.id)) {
            ItemRecord item = itemRepository.findByTarkovId(itemData.id);
            if (keyRepository.existsByItemId(item.getId())) {
              keyRepository.update(entity -> entity.setUses(itemData.properties.onItemPropertiesKey.uses), KEY.ITEM_ID.eq(item.getId()));
              log.debug("Updated uses for key {} to {}", item.getId(), itemData.properties.onItemPropertiesKey.uses);
              updated++;
            } else {
              keyRepository.insert(entity -> entity.setItemId(item.getId())
                                                   .setUses(itemData.properties.onItemPropertiesKey.uses));
              log.debug("Inserted key {} with uses {}", item.getId(), itemData.properties.onItemPropertiesKey.uses);
              inserted++;
            }
          } else {
            log.warn("Item not found in database: {}", itemData.id);
          }
        }
        log.info("Updated {} keys, Inserted {} keys", updated, inserted);
      });
    }
  }

  @SneakyThrows
  private ItemData[] getPVPData() {
    log.info("Fetching PVP data from {}", PVP_ALL_ITEMS);
    String jsonString = restTemplate.getForObject(PVP_ALL_ITEMS, String.class, tarkovMarketApiKey);
    ItemData[] data = objectMapper.readValue(jsonString, ItemData[].class);
    log.info("Fetched {} PVP items.", data.length);
    return data;
  }

  @SneakyThrows
  private ItemData[] getPVEData() {
    log.info("Fetching PVE data from {}", PVE_ALL_ITEMS);
    String jsonString = restTemplate.getForObject(PVE_ALL_ITEMS, String.class, tarkovMarketApiKey);
    ItemData[] data = objectMapper.readValue(jsonString, ItemData[].class);
    log.info("Fetched {} PVE items.", data.length);
    return data;
  }

  @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
  private void performUpdate() {
    log.info("Scheduled task started: updateData");
    updateData();
    fetchKeys(1);
    log.info("Scheduled task completed: updateData. Next execution at {}", LocalDateTime.now().plusMinutes(10));
  }

  private record ItemData(String uid, String name, Boolean bannedOnFlea, Boolean haveMarketData, String shortName,
                          Integer price, Integer basePrice, Integer avg24hPrice, Integer avg7daysPrice,
                          String traderName, Integer traderPrice, String traderPriceCur, Integer traderPriceRub,
                          String updated, Integer slots, String icon, String link, String wikiLink, String img,
                          String imgBig, String bsgId, String[] tags, Double diff24h, Double diff7days,
                          Boolean isFunctional, String reference) {

  }

}
