package ch.gatzka.view.service;

import ch.gatzka.tables.records.ItemGridViewRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static ch.gatzka.Tables.ITEM;
import static ch.gatzka.Tables.ITEM_GRID_VIEW;

@Service
@RequiredArgsConstructor
public class ItemGridViewService {

    private final DSLContext dslContext;

    public Collection<ItemGridViewRecord> readItems(Collection<Condition> conditions) {
        return dslContext.selectFrom(ITEM_GRID_VIEW).where(conditions).fetch();
    }

    public Collection<String> readItemTags(){
        Table<?> allTags = DSL.unnest(ITEM.TAGS).as("all_tags", "tag");
        return dslContext.select(allTags.field("tag"))
                .from(ITEM)
                .join(allTags)
                .on(DSL.trueCondition()) // Cross join
                .groupBy(allTags.field("tag"))
                .fetchInto(String.class);
    }
}
