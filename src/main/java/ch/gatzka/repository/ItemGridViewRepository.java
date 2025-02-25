package ch.gatzka.repository;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;

import ch.gatzka.repository.base.ViewRepository;
import ch.gatzka.tables.records.ItemGridViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class ItemGridViewRepository extends ViewRepository<ItemGridViewRecord> {

  public ItemGridViewRepository(DSLContext dslContext) {
    super(dslContext, ITEM_GRID_VIEW);
  }

}
