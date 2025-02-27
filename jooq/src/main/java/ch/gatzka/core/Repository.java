package ch.gatzka.core;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;

public abstract class Repository<R extends TableRecord<R>> {

  protected final DSLContext dslContext;

  protected final Table<R> table;

  protected Repository(DSLContext dslContext, Table<R> table) {
    this.dslContext = dslContext;
    this.table = table;
  }

  public int count() {
    return count(DSL.trueCondition());
  }

  public int count(Condition... conditions) {
    return dslContext.fetchCount(table, conditions);
  }

  public boolean exists(Condition... conditions) {
    return dslContext.fetchExists(table, conditions);
  }

  public Result<R> read() {
    return read(DSL.trueCondition());
  }

  public Result<R> read(Condition... conditions) {
    return dslContext.fetch(table, conditions);
  }

  public R get(Condition... conditions) {
    return dslContext.fetchOne(table, conditions);
  }

}
