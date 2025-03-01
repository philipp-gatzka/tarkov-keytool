package ch.gatzka.table;

import static ch.gatzka.Tables.LOOT_REPORT;

import ch.gatzka.core.Sequenced;
import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.LootReportRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.springframework.stereotype.Service;

@Service
public class LootReportRepository extends TableRepository<LootReportRecord> implements Sequenced<LootReportRecord, Integer> {

  protected LootReportRepository(DSLContext dslContext) {
    super(dslContext, LOOT_REPORT);
  }

  @Override
  public TableField<LootReportRecord, Integer> getSequencedField() {
    return LOOT_REPORT.ID;
  }

}
