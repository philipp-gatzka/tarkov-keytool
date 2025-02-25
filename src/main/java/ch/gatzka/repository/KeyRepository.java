package ch.gatzka.repository;

import ch.gatzka.repository.base.WriteRepository;
import ch.gatzka.tables.records.KeyRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.KEY;

@Service
public class KeyRepository extends WriteRepository<KeyRecord> {

    public KeyRepository(DSLContext dslContext) {
        super(dslContext, KEY);
    }

    public boolean existsByItemId(int itemId) {
        return exists(KEY.ITEM_ID.eq(itemId));
    }

}
