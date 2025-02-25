package ch.gatzka.repository.base;

import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import java.util.function.Function;

@Slf4j
public abstract class WriteRepository<R extends UpdatableRecord<R>> extends ReadOnlyRepository<R> {

    public WriteRepository(DSLContext dslContext, Table<R> table) {
        super(dslContext, table);
    }

    public int insert(Function<R, R> mapping) {
        return dslContext.insertInto(table).set(mapping.apply(dslContext.newRecord(table))).execute();
    }

    public void update(Function<R, R> mapping, Condition... conditions) {
        log.debug("Updating records with conditions: {}", (Object) conditions);
        dslContext.batchUpdate(read(conditions).map(mapping::apply)).execute();
    }

    public int delete(Condition... conditions) {
        log.debug("Deleting records with conditions: {}", (Object) conditions);
        return dslContext.deleteFrom(table).where(conditions).execute();
    }

}
