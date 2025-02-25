package ch.gatzka.repository.base;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableRecord;

public abstract class ViewRepository<R extends TableRecord<R>> extends ReadOnlyRepository<R> {

    public ViewRepository(DSLContext dslContext, Table<R> table) {
        super(dslContext, table);
    }

}
