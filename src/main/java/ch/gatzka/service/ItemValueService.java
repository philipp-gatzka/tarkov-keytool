package ch.gatzka.service;

import static ch.gatzka.Tables.ITEM;

import ch.gatzka.enums.Currency;
import ch.gatzka.enums.GameMode;
import ch.gatzka.table.ItemRepository;
import ch.gatzka.tables.records.ItemRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemValueService {

  private final ItemRepository itemRepository;

  public int itemValue(ItemRecord item, GameMode gameMode) {
    final String DOLLAR = "5696686a4bdc2da3298b456a";
    final String EURO = "569668774bdc2da2298b4568";

    final Integer fleaPrice = gameMode == GameMode.PVP ? item.getPvpFleaPrice() : item.getPveFleaPrice();
    final Integer traderPrice = item.getTraderPrice();
    final Currency currency = item.getTraderCurrency();
    final boolean bannedOnFlea = gameMode == GameMode.PVP ? item.getPvpBannedOnFlea() : item.getPveBannedOnFlea();

    if (bannedOnFlea) {
      switch (currency) {
        case Currency.$ -> {
          ItemRecord dollar = itemRepository.get(ITEM.TARKOV_ID.eq(DOLLAR));
          return traderPrice * dollar.getTraderPrice();
        }
        case Currency.€ -> {
          ItemRecord euro = itemRepository.get(ITEM.TARKOV_ID.eq(EURO));
          return traderPrice * euro.getTraderPrice();
        }
        default -> {
          return traderPrice;
        }
      }
    } else {
      int traderValue = traderPrice;
      if (currency == Currency.$) {
        ItemRecord dollar = itemRepository.get(ITEM.TARKOV_ID.eq(DOLLAR));
        traderValue *= dollar.getTraderPrice();
      } else if (currency == Currency.€) {
        ItemRecord euro = itemRepository.get(ITEM.TARKOV_ID.eq(EURO));
        traderValue *= euro.getTraderPrice();
      }

      if (fleaPrice != null && fleaPrice > traderValue) {
        return fleaPrice;
      } else {
        return traderValue;
      }
    }
  }

}
