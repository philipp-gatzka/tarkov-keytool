package ch.gatzka.repository;

import static ch.gatzka.Tables.LOOT_REPORT;

import ch.gatzka.repository.base.WriteRepository;
import ch.gatzka.tables.records.LootReportRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class LootReportRepository extends WriteRepository<LootReportRecord> {

  protected LootReportRepository(DSLContext dslContext) {
    super(dslContext, LOOT_REPORT);
  }

}
