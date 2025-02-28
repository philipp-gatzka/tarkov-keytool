package ch.gatzka;

import static ch.gatzka.Tables.KEY;

import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.KeyRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class KeyRepository extends TableRepository<KeyRecord> {

  protected KeyRepository(DSLContext dslContext) {
    super(dslContext, KEY);
  }

}
