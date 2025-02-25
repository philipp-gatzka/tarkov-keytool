package ch.gatzka.repository.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class ReadOnlyRepository<R extends TableRecord<R>> {

    protected final DSLContext dslContext;

    protected final Table<R> table;

    public Result<R> read(Collection<Condition> conditions) {
        return read(conditions.toArray(Condition[]::new));
    }

    public Result<R> read(Condition... conditions) {
        log.debug("Reading records from table {} with conditions {}", table.getName(), conditions);
        Result<R> result = dslContext.fetch(table, conditions);
        log.debug("Read {} records from table {} with condition {}", result.size(), table.getName(), conditions);
        return result;
    }

    public Result<R> read() {
        return read(DSL.trueCondition());
    }

    public Optional<R> findOptional(Condition... conditions) {
        log.debug("Reading optional record from table {} with conditions {}", table.getName(), conditions);
        Optional<R> result = dslContext.fetchOptional(table, conditions);
        log.debug("Read {} record from table {} with condition {}", result.isPresent() ? 1 : 0, table.getName(), conditions);
        return result;
    }

    public R find(Condition... conditions) {
        log.debug("Reading record from table {} with conditions {}", table.getName(), conditions);
        return dslContext.fetchSingle(table, conditions);
    }

    public int count(Condition... conditions) {
        log.debug("Counting records from table {} with conditions {}", table.getName(), conditions);
        int result = dslContext.fetchCount(table, conditions);
        log.debug("Counted {} records from table {} with condition {}", result, table.getName(), conditions);
        return result;
    }

    public int count() {
        return count(DSL.trueCondition());
    }

    public boolean exists(Condition... conditions) {
        log.debug("Checking existence of records from table {} with conditions {}", table.getName(), conditions);
        boolean result = dslContext.fetchExists(table, conditions);
        log.debug("Checked existence of {} records from table {} with condition {}", result ? 1 : 0, table.getName(), conditions);
        return result;
    }

}
