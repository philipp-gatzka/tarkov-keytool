package ch.gatzka.core;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

public abstract class TableRepository<R extends UpdatableRecord<R>> extends Repository<R> {

  protected TableRepository(DSLContext dslContext, Table<R> table) {
    super(dslContext, table);
  }

  public int insert(UnaryOperator<R> mapping) {
    return dslContext.insertInto(table).set(mapping.apply(dslContext.newRecord(table))).execute();
  }

  public int update(UnaryOperator<R> mapping, Condition... conditions) {
    return Arrays.stream(dslContext.batchUpdate(read(conditions).map(mapping::apply)).execute()).sum();
  }

  public int delete(Condition... conditions) {
    return dslContext.deleteFrom(table).where(conditions).execute();
  }

}
