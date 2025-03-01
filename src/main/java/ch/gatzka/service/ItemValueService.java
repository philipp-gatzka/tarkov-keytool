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

  /**
   *
   CREATE OR REPLACE FUNCTION item_value(item RECORD, game_mode TEXT) RETURNS INTEGER AS $$
   DECLARE
       DOLLAR TEXT := '5696686a4bdc2da3298b456a';
       EURO TEXT := '569668774bdc2da2298b4568';
       flea_price INTEGER;
       trader_price INTEGER;
       trader_currency TEXT;
       banned_on_flea BOOLEAN;
       trader_value INTEGER;
       dollar_price INTEGER;
       euro_price INTEGER;
   BEGIN
       IF game_mode = 'PVP' THEN
           flea_price := item.pvp_flea_price;
           banned_on_flea := item.pvp_banned_on_flea;
       ELSE
           flea_price := item.pve_flea_price;
           banned_on_flea := item.pve_banned_on_flea;
       END IF;

       trader_price := item.trader_price;
       trader_currency := item.trader_currency;

       IF banned_on_flea THEN
           IF trader_currency = '$' THEN
               SELECT trader_price INTO dollar_price FROM items WHERE tarkov_id = DOLLAR;
               RETURN trader_price * dollar_price;
           ELSIF trader_currency = '€' THEN
               SELECT trader_price INTO euro_price FROM items WHERE tarkov_id = EURO;
               RETURN trader_price * euro_price;
           ELSE
               RETURN trader_price;
           END IF;
       ELSE
           trader_value := trader_price;
           IF trader_currency = '$' THEN
               SELECT trader_price INTO dollar_price FROM items WHERE tarkov_id = DOLLAR;
               trader_value := trader_value * dollar_price;
           ELSIF trader_currency = '€' THEN
               SELECT trader_price INTO euro_price FROM items WHERE tarkov_id = EURO;
               trader_value := trader_value * euro_price;
           END IF;

           IF flea_price IS NOT NULL AND flea_price > trader_value THEN
               RETURN flea_price;
           ELSE
               RETURN trader_value;
           END IF;
       END IF;
   END;
   $$ LANGUAGE plpgsql;
   */
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
