package ch.gatzka.service;

import ch.gatzka.FetchItemSlotsQuery;
import ch.gatzka.FetchKeysQuery;
import ch.gatzka.pojo.Item;
import com.apollographql.apollo.api.Query;
import com.apollographql.java.client.ApolloClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {

  private static final String PVE_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/pve/items/all?x-api-key={API_KEY}";

  private static final String PVP_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/items/all?x-api-key={API_KEY}";

  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Value("${tarkov-market.api.key}")
  private String apiKey;

  @SneakyThrows
  private <V> V executeGet(String url, Class<V> clazz) {
    log.info("Executing GET request to {}", url);
    String json = restTemplate.getForObject(url, String.class, apiKey);
    return objectMapper.readValue(json, clazz);
  }

  public Item[] readPVEItems() {
    return executeGet(PVE_ALL_ITEMS, Item[].class);
  }

  public Item[] readPVPItems() {
    return executeGet(PVP_ALL_ITEMS, Item[].class);
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

  public List<FetchItemSlotsQuery.Item> readItemSlots() {
    return executeQuery(5, new FetchItemSlotsQuery()).items.stream().filter(Objects::nonNull).toList();
  }

  public List<FetchKeysQuery.Item> readKeys() {
    return executeQuery(5, new FetchKeysQuery()).items.stream().filter(Objects::nonNull).toList();
  }

}
