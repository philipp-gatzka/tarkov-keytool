package ch.gatzka.repository;

import static ch.gatzka.Tables.LATEST_ITEM_PRICE_VIEW;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.base.ViewRepository;
import ch.gatzka.tables.records.LatestItemPriceViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class LatestItemPriceViewRepository extends ViewRepository<LatestItemPriceViewRecord> {

  public LatestItemPriceViewRepository(DSLContext dslContext) {
    super(dslContext, LATEST_ITEM_PRICE_VIEW);
  }

  public LatestItemPriceViewRecord findByItemIdAndMode(int itemId, GameMode mode) {
    return find(LATEST_ITEM_PRICE_VIEW.ITEM_ID.eq(itemId).and(LATEST_ITEM_PRICE_VIEW.MODE.eq(mode)));
  }

}
