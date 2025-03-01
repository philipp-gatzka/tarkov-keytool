package ch.gatzka.view;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;
import static ch.gatzka.enums.GameMode.PVP;

import ch.gatzka.ItemGridViewRepository;
import ch.gatzka.Utils;
import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemGridViewRecord;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.jooq.Condition;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Items")
@Route("items")
@Menu(order = 1, icon = LineAwesomeIconUrl.BOX_OPEN_SOLID)
@AnonymousAllowed
public class ItemView extends VerticalLayout {

  private final ItemGridViewRepository itemGridViewRepository;

  private final GameMode gameMode;

  private final Grid<ItemGridViewRecord> grid = Utils.defaultStripedGrid(ItemGridViewRecord.class);

  private final Map<String, Condition> filterConditions = new HashMap<>();

  public ItemView(ItemGridViewRepository itemGridViewRepository, AuthenticatedAccount authenticatedAccount) {
    this.itemGridViewRepository = itemGridViewRepository;
    this.gameMode = authenticatedAccount.isAuthenticated() ? authenticatedAccount.getAccount()
        .getGameMode() : GameMode.PVP;

    setSizeFull();

    createHeader();
    createGrid();

    refreshGrid();
  }

  private void createHeader() {
    TextField nameField = new TextField("Name");
    nameField.setWidthFull();
    nameField.addValueChangeListener(event -> setFilter("name", event.getValue(), value -> ITEM_GRID_VIEW.NAME.likeIgnoreCase("%" + value + "%")));

    HorizontalLayout header = new HorizontalLayout(nameField);
    header.setWidthFull();
    add(header);
  }

  private <V> void setFilter(String filter, V newValue, Function<V, Condition> conditionMapping) {
    if (newValue == null) {
      filterConditions.remove(filter);
    } else {
      filterConditions.put(filter, conditionMapping.apply(newValue));
    }
    refreshGrid();
  }

  private void refreshGrid() {
    grid.setItems(itemGridViewRepository.read(filterConditions.values()));
  }

  private void createGrid() {
    grid.addComponentColumn(entry -> Utils.createImageRenderer(entry, ItemGridViewRecord::getIconLink, ItemGridViewRecord::getName, ItemGridViewRecord::getHorizontalSlots, ItemGridViewRecord::getVerticalSlots))
        .setHeader("Image");
    grid.addColumn("name").setHeader("Name");

    grid.addComponentColumn(entry -> {
      boolean isBanned = gameMode == PVP ? entry.getPvpBannedOnFlea() : entry.getPveBannedOnFlea();
      Icon icon = (isBanned ? LumoIcon.CROSS : LumoIcon.CHECKMARK).create();
      icon.setColor(isBanned ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
      return icon;
    }).setHeader("Banned on Flea");

    grid.addColumn(gameMode == PVP ? "pvpFleaPrice" : "pveFleaPrice")
        .setRenderer(Utils.roubleRenderer(entry -> switch (gameMode) {
          case PVE:
            yield entry.getPveFleaPrice();
          case PVP:
            yield entry.getPvpFleaPrice();
        }))
        .setHeader("Flea price");

    grid.addColumn("traderPrice")
        .setRenderer(Utils.priceRenderer(ItemGridViewRecord::getTraderCurrency, ItemGridViewRecord::getTraderPrice))
        .setHeader("Trader price");

    grid.addComponentColumn(entry -> {
      HorizontalLayout layout = new HorizontalLayout();
      for (String tag : entry.getTags()) {
        Span badge = new Span(new Span(tag.replace("_", " ")));
        badge.getElement().getThemeList().add("badge");
        layout.add(badge);
      }
      return layout;
    }).setHeader("Tags");

    grid.addComponentColumn(entry -> {
      Button wikiButton = new Button("Wiki", VaadinIcon.BOOKMARK.create());
      wikiButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getWikiLink(), "_blank"));
      wikiButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

      Button marketButton = new Button("Market", VaadinIcon.GIFT.create());
      marketButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getMarketLink(), "_blank"));
      marketButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

      return new HorizontalLayout(marketButton, wikiButton);
    }).setHeader("Actions");

    add(grid);
  }

}
