package ch.gatzka.view;

import static ch.gatzka.Tables.LOOT_REPORT_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.LootReportViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class LootReportViewRepository extends ViewRepository<LootReportViewRecord> {

  protected LootReportViewRepository(DSLContext dslContext) {
    super(dslContext, LOOT_REPORT_VIEW);
  }

}
