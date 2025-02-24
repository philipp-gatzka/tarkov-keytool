package ch.gatzka.service;

import ch.gatzka.enums.GameMode;
import ch.gatzka.tables.records.ItemRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class DataProviderService {


    private final String PVE_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/pve/items/all?x-api-key={API_KEY}";

    private final String PVP_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/items/all?x-api-key={API_KEY}";

    private final ItemService itemService;

    @Value("${tarkov-market.api.key}")
    private String tarkovMarketApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                    log.debug("Progress: {}/{} ({}%), Estimated time remaining: {} ms",
                            counter, itemData.length, roundedProgress, estimatedRemaining);
                }
            }

            Instant instant = data.updated.isEmpty() ? Instant.now() : Instant.parse(data.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

            int itemId;
            String iconLink = data.icon != null && !data.icon.trim().isEmpty() ? data.icon : data.imgBig;
            if (itemService.existsByTarkovId(data.bsgId)) {
                ItemRecord item = itemService.getByTarkovId(data.bsgId);
                if (item.getLastUpdate().isBefore(updated)) {
                    item.setName(data.name);
                    item.setIconLink(iconLink);
                    item.setWikiLink(data.wikiLink);
                    item.setMarketLink(data.link);
                    item.setSlots(data.slots);
                    item.setTags(data.tags);
                    item.setLastUpdate(updated);
                    item.setTraderPrice(data.traderPrice);
                    item.setTraderCurrency(data.traderPriceCur);
                    item.setTraderName(data.traderName);

                    item.update();

                    totalUpdate++;
                }
                itemId = item.getId();
            } else {
                itemId = itemService.createItem(
                        data.uid,
                        data.bsgId,
                        data.name,
                        iconLink,
                        data.link,
                        data.wikiLink,
                        data.slots,
                        data.tags,
                        data.traderPrice,
                        data.traderName,
                        data.traderPriceCur,
                        updated
                );

                totalInsert++;
            }

            totalPriceInsert += insertItemPrice(data, itemId, gameMode, updated);
        }

        log.info("Inserted {} items, Updated {} items, Inserted {} item prices", totalInsert, totalUpdate, totalPriceInsert);
    }

    private int insertItemPrice(ItemData data, int itemId, GameMode gameMode, LocalDateTime timestamp) {
        if (!itemService.doesPriceExist(itemId, gameMode, timestamp)) {
            itemService.insertItemPrice(itemId, gameMode, data.avg24hPrice, data.bannedOnFlea, timestamp);
            return 1;
        }
        return 0;
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

    private record ItemData(String uid, String name, Boolean bannedOnFlea, Boolean haveMarketData, String shortName,
                            Integer price, Integer basePrice, Integer avg24hPrice, Integer avg7daysPrice,
                            String traderName, Integer traderPrice,
                            String traderPriceCur, Integer traderPriceRub, String updated, Integer slots, String icon,
                            String link, String wikiLink, String img, String imgBig, String bsgId, String[] tags,
                            Double diff24h, Double diff7days,
                            Boolean isFunctional, String reference) {
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 3)
    private void performUpdate() {
        log.info("Scheduled task started: updateData");
        updateData();
        log.info("Scheduled task completed: updateData. Next execution at {}", LocalDateTime.now().plusMinutes(10));
    }

}
