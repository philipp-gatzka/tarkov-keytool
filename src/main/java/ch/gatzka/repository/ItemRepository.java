package ch.gatzka.repository;

import static ch.gatzka.Sequences.ITEM_ID_SEQ;
import static ch.gatzka.Tables.ITEM;

import ch.gatzka.repository.base.SequencedWriteRepository;
import ch.gatzka.tables.records.ItemRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class ItemRepository extends SequencedWriteRepository<ItemRecord> {

  public ItemRepository(DSLContext dslContext) {
    super(dslContext, ITEM, ITEM_ID_SEQ, ITEM.ID);
  }

  public boolean existsByTarkovId(String tarkovId) {
    return exists(ITEM.TARKOV_ID.eq(tarkovId));
  }

  public ItemRecord findByTarkovId(String tarkovId) {
    return find(ITEM.TARKOV_ID.eq(tarkovId));
  }

}
