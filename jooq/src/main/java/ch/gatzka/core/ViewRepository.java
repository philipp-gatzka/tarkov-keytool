package ch.gatzka.core;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableRecord;

public abstract class ViewRepository<R extends TableRecord<R>> extends Repository<R> {

  protected ViewRepository(DSLContext dslContext, Table<R> table) {
    super(dslContext, table);
  }

}
