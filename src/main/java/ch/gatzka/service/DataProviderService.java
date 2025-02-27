package ch.gatzka.service;

import static ch.gatzka.ApplicationUtils.executeProgressive;
import static ch.gatzka.Tables.ITEM;
import static ch.gatzka.Tables.KEY;

import ch.gatzka.FetchItemSlotsQuery;
import ch.gatzka.FetchKeysQuery;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemPriceRepository;
import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.tables.records.ItemRecord;
import com.apollographql.apollo.api.Query;
import com.apollographql.java.client.ApolloClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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

    executeProgressive(itemData, data -> {
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
      }

      insertItemPrice(data, itemId, gameMode, updated);
    });
  }

  private void insertItemPrice(ItemData data, int itemId, GameMode gameMode, LocalDateTime timestamp) {
    if (!itemPriceRepository.existsByItemIdAndGameModeAndTimestamp(itemId, gameMode, timestamp)) {
      itemPriceRepository.insert(entity -> entity.setItemId(itemId)
          .setMode(gameMode)
          .setTimestamp(timestamp)
          .setFleaPrice(data.avg7daysPrice)
          .setBannedOnFlea(data.bannedOnFlea));
    }
  }

  private void fetchSlots() {
    List<FetchItemSlotsQuery.Item> data = executeQuery(5, new FetchItemSlotsQuery()).items.stream()
        .filter(Objects::nonNull)
        .toList();
    log.info("Updating item slot sizes");
    executeProgressive(data, itemData -> {
      itemRepository.update(entity -> entity.setVerticalSlots(itemData.height)
          .setHorizontalSlots(itemData.width), ITEM.TARKOV_ID.eq(itemData.id));
    });
  }

  private void fetchKeys() {
    List<FetchKeysQuery.Item> data = executeQuery(5, new FetchKeysQuery()).items.stream()
        .filter(Objects::nonNull)
        .toList();

    log.info("Updating key uses");
    executeProgressive(data, itemData -> {
      if (itemRepository.existsByTarkovId(itemData.id)) {
        ItemRecord item = itemRepository.findByTarkovId(itemData.id);
        if (keyRepository.existsByItemId(item.getId())) {
          keyRepository.update(entity -> entity.setUses(itemData.properties.onItemPropertiesKey.uses), KEY.ITEM_ID.eq(item.getId()));
        } else {
          keyRepository.insert(entity -> entity.setItemId(item.getId())
              .setUses(itemData.properties.onItemPropertiesKey.uses));
        }
      } else {
        log.debug("Item not found in database: {}", itemData.id);
      }
    });
  }

  private <D extends Query.Data> D executeQuery(int retries, Query<D> operation) {
    try (ApolloClient service = new ApolloClient.Builder().serverUrl("https://api.tarkov.dev/graphql").build()) {
      CompletableFuture<D> future = new CompletableFuture<>();
      log.debug("Executing query {}", operation.name());
      service.query(operation).enqueue(response -> {
        if (response.exception != null || response.data == null) {
          if (retries > 0) {
            log.warn("Error updating data, retrying {} times", retries);
            future.complete(executeQuery(retries - 1, operation));
          } else {
            log.error("Error updating data", response.exception);
            future.completeExceptionally(response.exception);
          }
          return;
        }
        log.debug("Query {} executed successfully", operation.name());
        future.complete(response.data);
      });
      return future.join();
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

  @Async
  @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
  public void performUpdate() {
    log.info("Scheduled task started: updateData");
    // updateData();
    // fetchKeys();
    // fetchSlots();
    log.info("Scheduled task completed: updateData. Next execution at {}", LocalDateTime.now().plusMinutes(10));
  }

  private record ItemData /* NOSONAR */(String uid, String name, Boolean bannedOnFlea, Boolean haveMarketData,
      String shortName, Integer price, Integer basePrice, Integer avg24hPrice, Integer avg7daysPrice, String traderName,
      Integer traderPrice, String traderPriceCur, Integer traderPriceRub, String updated, Integer slots, String icon,
      String link, String wikiLink, String img, String imgBig, String bsgId, String[] tags, Double diff24h,
      Double diff7days, Boolean isFunctional, String reference) {

  }

}
