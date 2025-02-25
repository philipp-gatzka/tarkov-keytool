package ch.gatzka.repository;

import static ch.gatzka.Tables.KEY_GRID_VIEW;

import ch.gatzka.repository.base.ViewRepository;
import ch.gatzka.tables.records.KeyGridViewRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.springframework.stereotype.Service;

@Service
public class KeyGridViewRepository extends ViewRepository<KeyGridViewRecord> {

  public KeyGridViewRepository(DSLContext dslContext) {
    super(dslContext, KEY_GRID_VIEW);
  }

}
