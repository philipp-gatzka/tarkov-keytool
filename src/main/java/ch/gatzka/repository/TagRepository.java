package ch.gatzka.repository;

import static ch.gatzka.Tables.TAG;

import ch.gatzka.repository.base.ViewRepository;
import ch.gatzka.tables.records.TagRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class TagRepository extends ViewRepository<TagRecord> {

  public TagRepository(DSLContext dslContext) {
    super(dslContext, TAG);
  }

}
