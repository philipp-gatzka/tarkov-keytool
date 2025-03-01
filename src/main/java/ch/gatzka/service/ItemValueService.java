package ch.gatzka.service;

import static ch.gatzka.Tables.ITEM;

import ch.gatzka.enums.GameMode;
import ch.gatzka.table.ItemRepository;
import ch.gatzka.tables.records.ItemRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemValueService {

  private static final String ID_DOLLAR = "5696686a4bdc2da3298b456a";

  private static final String ID_EURO = "569668774bdc2da2298b4568";

  private final ItemRepository itemRepository;

  public int itemValue(ItemRecord item, GameMode gameMode) {
    boolean isPvp = gameMode == GameMode.PVP;

    Integer fleaPrice = isPvp ? item.getPvpFleaPrice() : item.getPveFleaPrice();

    if (Boolean.TRUE.equals(isPvp ? item.getPvpBannedOnFlea() : item.getPveBannedOnFlea())) {
      int traderPrice = item.getTraderPrice();
      ItemRecord dollarItem = itemRepository.get(ITEM.TARKOV_ID.eq(ID_DOLLAR));
      ItemRecord euroItem = itemRepository.get(ITEM.TARKOV_ID.eq(ID_EURO));

      return switch (item.getTraderCurrency()) {
        case $ -> traderPrice * dollarItem.getTraderPrice();
        case € -> traderPrice * euroItem.getTraderPrice();
        case ₽ -> traderPrice;
      };
    } else {
      return fleaPrice == null ? 0 : fleaPrice;
    }
  }

}
