package ch.gatzka.view;

import static ch.gatzka.Tables.KEY_REPORT_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.KeyReportViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class KeyReportViewRepository extends ViewRepository<KeyReportViewRecord> {

  protected KeyReportViewRepository(DSLContext dslContext) {
    super(dslContext, KEY_REPORT_VIEW);
  }

}
