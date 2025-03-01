package ch.gatzka.table;

import static ch.gatzka.Tables.WIPE;

import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.WipeRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class WipeRepository extends TableRepository<WipeRecord> {

  protected WipeRepository(DSLContext dslContext) {
    super(dslContext, WIPE);
  }

  public WipeRecord getLatestWipe() {
    return dslContext.selectFrom(WIPE).orderBy(WIPE.ID.desc()).limit(1).fetchSingle();
  }

}
