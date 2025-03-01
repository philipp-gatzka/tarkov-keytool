package ch.gatzka;

import static ch.gatzka.Tables.KEY_GRID_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.KeyGridViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class KeyGridViewRepository extends ViewRepository<KeyGridViewRecord> {

  protected KeyGridViewRepository(DSLContext dslContext) {
    super(dslContext, KEY_GRID_VIEW);
  }

}
