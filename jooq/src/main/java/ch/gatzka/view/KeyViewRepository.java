package ch.gatzka.view;

import static ch.gatzka.Tables.KEY_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.KeyViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class KeyViewRepository extends ViewRepository<KeyViewRecord> {

  protected KeyViewRepository(DSLContext dslContext) {
    super(dslContext, KEY_VIEW);
  }

}
