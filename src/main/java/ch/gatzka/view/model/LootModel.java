package ch.gatzka.view.model;

import ch.gatzka.tables.records.ItemRecord;
import lombok.Getter;

@Getter
public class LootModel {

  private final ItemRecord item;

  private final String name;

  private final String iconLink;

  private final int count;

  public LootModel(ItemRecord item, int count) {
    this.item = item;
    this.count = count;
    this.iconLink = item.getIconLink();
    this.name = item.getName();
  }

}
