package ch.gatzka.view.core;

import ch.gatzka.Utils;
import ch.gatzka.core.Repository;
import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.jooq.Condition;
import org.jooq.TableRecord;

public abstract class FilteredGridView<T extends TableRecord<T>> extends VerticalLayout {

  protected final Repository<T> repository;

  private final Grid<T> grid = Utils.defaultStripedGrid(getBeanClass());

  protected final GameMode gameMode;

  private final Map<String, Condition> filterConditions = new HashMap<>();

  protected final AuthenticatedAccount authenticatedAccount;

  protected FilteredGridView(Repository<T> repository, AuthenticatedAccount authenticatedAccount) {
    this.repository = repository;
    this.authenticatedAccount = authenticatedAccount;
    this.gameMode = authenticatedAccount.isAuthenticated() ? authenticatedAccount.getAccount()
        .getGameMode() : GameMode.PVP;

    refreshGrid();
  }

  protected abstract Class<T> getBeanClass();

  protected void createView() {
    setSizeFull();

    createHeader();
    createGridColumns(grid, gameMode);

    add(grid);
  }

  protected void createHeader() {
    HorizontalLayout header = new HorizontalLayout(createFilters());
    header.setWidthFull();
    add(header);
  }

  protected abstract Component[] createFilters();

  protected abstract void createGridColumns(Grid<T> grid, GameMode gameMode);

  protected void refreshGrid() {
    grid.setItems(repository.read(filterConditions.values()));
  }

  protected <V> void setFilter(String filter, V newValue, Function<V, Condition> conditionMapping) {
    if (newValue == null) {
      filterConditions.remove(filter);
    } else {
      filterConditions.put(filter, conditionMapping.apply(newValue));
    }
    refreshGrid();
  }

}
