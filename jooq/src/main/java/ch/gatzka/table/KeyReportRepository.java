package ch.gatzka.table;

import static ch.gatzka.Tables.KEY_REPORT;

import ch.gatzka.core.Sequenced;
import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.KeyReportRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.springframework.stereotype.Service;

@Service
public class KeyReportRepository extends TableRepository<KeyReportRecord> implements Sequenced<KeyReportRecord, Integer> {

  protected KeyReportRepository(DSLContext dslContext) {
    super(dslContext, KEY_REPORT);
  }

  @Override
  public TableField<KeyReportRecord, Integer> getSequencedField() {
    return KEY_REPORT.ID;
  }

}
