package ch.gatzka.repository;

import static ch.gatzka.Tables.ITEM_PRICE;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.base.WriteRepository;
import ch.gatzka.tables.records.ItemPriceRecord;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class ItemPriceRepository extends WriteRepository<ItemPriceRecord> {

  public ItemPriceRepository(DSLContext dslContext) {
    super(dslContext, ITEM_PRICE);
  }

  public boolean existsByItemIdAndGameModeAndTimestamp(int itemId, GameMode gameMode, LocalDateTime timestamp) {
    return exists(ITEM_PRICE.ITEM_ID.eq(itemId), ITEM_PRICE.MODE.eq(gameMode), ITEM_PRICE.TIMESTAMP.eq(timestamp));
  }

}
