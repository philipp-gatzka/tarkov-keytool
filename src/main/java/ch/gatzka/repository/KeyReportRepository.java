package ch.gatzka.repository;

import static ch.gatzka.Sequences.KEY_REPORT_ID_SEQ;
import static ch.gatzka.Tables.KEY_REPORT;

import ch.gatzka.repository.base.SequencedWriteRepository;
import ch.gatzka.tables.records.KeyReportRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class KeyReportRepository extends SequencedWriteRepository<KeyReportRecord> {

  public KeyReportRepository(DSLContext dslContext) {
    super(dslContext, KEY_REPORT, KEY_REPORT_ID_SEQ, KEY_REPORT.ID);
  }

}
