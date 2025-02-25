package ch.gatzka.view;

import static ch.gatzka.ApplicationUtils.defaultStripedGrid;
import static ch.gatzka.Tables.ITEM_GRID_VIEW;

import ch.gatzka.ApplicationUtils;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemGridViewRepository;
import ch.gatzka.repository.TagRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemGridViewRecord;
import ch.gatzka.tables.records.TagRecord;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.jooq.Condition;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Items")
@Route(value = "items")
@Menu(order = 1, icon = LineAwesomeIconUrl.BOX_OPEN_SOLID)
@AnonymousAllowed
public class ItemGridView extends VerticalLayout {

  private final Grid<ItemGridViewRecord> grid = defaultStripedGrid(ItemGridViewRecord.class);

  private final Map<String, Condition> filterConditions = new HashMap<>();

  private final GameMode gameMode;

  private final transient TagRepository tagRepository;

  private final transient ItemGridViewRepository itemGridViewRepository;

  public ItemGridView(AuthenticatedAccount authenticatedAccount, TagRepository tagRepository, ItemGridViewRepository itemGridViewRepository) {
    this.tagRepository = tagRepository;
    this.itemGridViewRepository = itemGridViewRepository;
    this.gameMode = authenticatedAccount.isAuthenticated() ? authenticatedAccount.getAccount().getMode() : GameMode.PVP;

    setSizeFull();

    createHeader();
    createGrid();

    refreshGrid();
  }

  private <V> HasValue.ValueChangeListener<HasValue.ValueChangeEvent<V>> createFilterListener(String name, Function<V, Condition> conditionCreator) {
    return event -> {
      if (event.getValue() == null) {
        filterConditions.remove(name);
      } else {
        filterConditions.put(name, conditionCreator.apply(event.getValue()));
      }
    };
  }

  private void createHeader() {
    I18NProvider i18n = VaadinService.getCurrent().getInstantiator().getI18NProvider();
    Locale locale = VaadinService.getCurrentRequest().getLocale();

    TextField nameFilter = new TextField();
    nameFilter.addValueChangeListener(createFilterListener("name", value -> ITEM_GRID_VIEW.NAME.likeIgnoreCase("%" + value + "%")));
    nameFilter.setLabel(i18n.getTranslation("items.filter.name.label", locale));
    nameFilter.setPlaceholder(i18n.getTranslation("items.filter.name.placeholder", locale));
    nameFilter.setClearButtonVisible(true);
    nameFilter.setWidthFull();

    MultiSelectComboBox<TagRecord> tagFilter = new MultiSelectComboBox<>();
    tagFilter.setLabel(i18n.getTranslation("items.filter.tag.label", locale));
    tagFilter.setPlaceholder(i18n.getTranslation("items.filter.tag.placeholder", locale));
    tagFilter.setItems(tagRepository.read());
    tagFilter.setClearButtonVisible(true);
    tagFilter.setWidthFull();
    tagFilter.addValueChangeListener(createFilterListener("tag", value -> ITEM_GRID_VIEW.TAGS.contains(value.stream()
                                                                                                            .map(TagRecord::getTag)
                                                                                                            .toArray(String[]::new))));
    tagFilter.setItemLabelGenerator(TagRecord::getClean);

    Button resetButton = new Button(i18n.getTranslation("items.filter.button.reset.label", locale));
    resetButton.setIcon(new Icon(VaadinIcon.TRASH));
    resetButton.setWidthFull();
    resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    resetButton.addClickListener(_ -> {
      nameFilter.clear();
      tagFilter.clear();

      refreshGrid();
    });

    Button searchButton = new Button(i18n.getTranslation("items.filter.button.search.label", locale));
    searchButton.setIcon(new Icon(VaadinIcon.SEARCH));
    searchButton.setWidthFull();
    searchButton.addClickListener(_ -> refreshGrid());
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.addClickShortcut(Key.ENTER);

    VerticalLayout buttons = new VerticalLayout(resetButton, searchButton);
    buttons.setSpacing(false);
    buttons.setWidth("min-content");
    buttons.setPadding(false);
    buttons.getThemeList().add("spacing-xs");

    HorizontalLayout header = new HorizontalLayout(nameFilter, tagFilter, buttons);
    header.setAlignSelf(Alignment.END, nameFilter, tagFilter);
    header.setWidthFull();
    add(header);
  }

  private void refreshGrid() {
    grid.setItems(itemGridViewRepository.read(filterConditions.values()));
  }

  private void createGrid() {
    I18NProvider i18n = VaadinService.getCurrent().getInstantiator().getI18NProvider();
    Locale locale = VaadinService.getCurrentRequest().getLocale();

    grid.addComponentColumn(entry -> ApplicationUtils.createImageRenderer(entry, ItemGridViewRecord::getIconLink, ItemGridViewRecord::getName))
        .setHeader(i18n.getTranslation("items.grid.column.image.header", locale));
    grid.addColumn("name").setHeader(i18n.getTranslation("items.grid.column.name.header", locale));

    grid.addComponentColumn(entry -> {
      boolean isBanned = gameMode == GameMode.PVP ? entry.getPvpFleaBanned() : entry.getPveFleaBanned();
      Icon icon = (isBanned ? LumoIcon.CROSS : LumoIcon.CHECKMARK).create();
      icon.setColor(isBanned ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
      return icon;
    }).setHeader(i18n.getTranslation("items.grid.column.banned_on_flea.header", locale));

    grid.addColumn(gameMode == GameMode.PVP ? "pvpPrice" : "pvePrice")
        .setRenderer(ApplicationUtils.roubleRenderer(entry -> switch (gameMode) {
          case PVE:
            yield entry.getPvePrice();
          case PVP:
            yield entry.getPvpPrice();
        }, locale))
        .setHeader(i18n.getTranslation("items.grid.column.flea_value.header", locale));

    grid.addColumn("traderPrice")
        .setRenderer(ApplicationUtils.priceRenderer(ItemGridViewRecord::getTraderCurrency, ItemGridViewRecord::getTraderPrice, locale))
        .setHeader(i18n.getTranslation("items.grid.column.trader_value.header", locale));

    grid.addComponentColumn(entry -> {
      HorizontalLayout layout = new HorizontalLayout();
      for (String tag : entry.getTags()) {
        Span badge = new Span(new Span(tag.replace("_", " ")));
        badge.getElement().getThemeList().add("badge");
        layout.add(badge);
      }
      return layout;
    }).setHeader(i18n.getTranslation("items.grid.column.tags.header", locale));

    grid.addComponentColumn(entry -> {
      Button wikiButton = new Button(i18n.getTranslation("items.grid.column.actions.button.wiki.label", locale), VaadinIcon.BOOKMARK.create());
      wikiButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getWikiLink(), "_blank"));
      wikiButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

      Button marketButton = new Button(i18n.getTranslation("items.grid.column.actions.button.market.label", locale), VaadinIcon.GIFT.create());
      marketButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getMarketLink(), "_blank"));
      marketButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

      return new HorizontalLayout(marketButton, wikiButton);
    }).setHeader(i18n.getTranslation("items.grid.column.actions.header", locale));

    add(grid);
  }

}
