package ch.gatzka.core;

import java.util.function.UnaryOperator;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;

public interface Sequenced<R extends UpdatableRecord<R>, I extends Number> {

  default I insertWithId(UnaryOperator<R> mapping) {
    return getDslContext().insertInto(getTable())
        .set(mapping.apply(getDslContext().newRecord(getTable())))
        .returningResult(getSequencedField())
        .fetchSingle(getSequencedField());
  }

  default void deleteById(I id) {
    getSelf().delete(getSequencedField().eq(id));
  }

  default void updateById(UnaryOperator<R> mapping, I id) {
    getSelf().update(mapping, getSequencedField().eq(id));
  }

  Table<R> getTable();

  DSLContext getDslContext();

  TableField<R, I> getSequencedField();

  TableRepository<R> getSelf();

}
