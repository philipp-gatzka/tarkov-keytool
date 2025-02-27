package ch.gatzka.repository.base;

import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;

@Slf4j
public abstract class SequencedWriteRepository<R extends UpdatableRecord<R>> extends WriteRepository<R> {

  private final Sequence<Integer> sequence;

  private final TableField<R, Integer> sequencedField;

  protected SequencedWriteRepository(DSLContext dslContext, Table<R> table, Sequence<Integer> sequence,
      TableField<R, Integer> sequencedField) {
    super(dslContext, table);
    this.sequence = sequence;
    this.sequencedField = sequencedField;
  }

  public R find(int id) {
    return find(sequencedField.eq(id));
  }

  @Override
  public int insert(UnaryOperator<R> mapping) {
    return dslContext.transactionResult(configuration -> {
      DSLContext dslContext = configuration.dsl();
      Integer id = dslContext.nextval(sequence);
      R newRecord = dslContext.newRecord(table);
      newRecord.set(sequencedField, id);
      dslContext.insertInto(table).set(mapping.apply(newRecord)).execute();
      return id;
    });
  }

  public void update(UnaryOperator<R> mapping, int id) {
    log.debug("Updating record with id {} with conditions {}", id, sequencedField.eq(id));
    update(mapping, sequencedField.eq(id));
  }

  public void delete(int id) {
    log.debug("Deleting record with id {}", id);
    delete(sequencedField.eq(id));
  }

}
