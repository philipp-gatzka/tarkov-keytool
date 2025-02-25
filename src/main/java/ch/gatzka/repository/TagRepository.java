package ch.gatzka.repository;

import ch.gatzka.repository.base.ReadOnlyRepository;
import ch.gatzka.tables.records.TagRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static ch.gatzka.Tables.TAG;

@Service
public class TagRepository extends ReadOnlyRepository<TagRecord> {

    public TagRepository(DSLContext dslContext) {
        super(dslContext, TAG);
    }

}
