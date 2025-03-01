package ch.gatzka;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.ItemGridViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class ItemGridViewRepository extends ViewRepository<ItemGridViewRecord> {

  protected ItemGridViewRepository(DSLContext dslContext) {
    super(dslContext, ITEM_GRID_VIEW);
  }

}
