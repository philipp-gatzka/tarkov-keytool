package ch.gatzka;

import static ch.gatzka.tables.ItemTable.ITEM;

import ch.gatzka.core.Sequenced;
import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.ItemRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.springframework.stereotype.Service;

@Service
public class ItemRepository extends TableRepository<ItemRecord> implements Sequenced<ItemRecord, Integer> {

  protected ItemRepository(DSLContext dslContext) {
    super(dslContext, ITEM);
  }

  @Override
  public TableField<ItemRecord, Integer> getSequencedField() {
    return ITEM.ID;
  }

}
