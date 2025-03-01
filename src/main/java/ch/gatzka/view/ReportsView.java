package ch.gatzka.view;

import static ch.gatzka.Tables.KEY_REPORT_VIEW;
import static ch.gatzka.Tables.KEY_VIEW;
import static ch.gatzka.Tables.LOOT_REPORT_VIEW;

import ch.gatzka.Utils;
import ch.gatzka.core.Repository;
import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.table.WipeRepository;
import ch.gatzka.tables.records.KeyReportViewRecord;
import ch.gatzka.tables.records.KeyViewRecord;
import ch.gatzka.tables.records.LocationViewRecord;
import ch.gatzka.tables.records.LootReportViewRecord;
import ch.gatzka.view.core.FilteredGridView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Reports")
@Route("reports")
@Menu(order = 4, icon = LineAwesomeIconUrl.LIST_ALT_SOLID)
@RolesAllowed("USER")
public class ReportsView extends FilteredGridView<KeyReportViewRecord> {

  private final KeyViewRepository keyViewRepository;

  private final LocationViewRepository locationViewRepository;

  private final LootReportViewRepository lootReportViewRepository;

  private final WipeRepository wipeRepository;

  protected ReportsView(Repository<KeyReportViewRecord> repository, AuthenticatedAccount authenticatedAccount,
      KeyViewRepository keyViewRepository, LocationViewRepository locationViewRepository,
      LootReportViewRepository lootReportViewRepository, WipeRepository wipeRepository) {
    super(repository, authenticatedAccount);
    this.keyViewRepository = keyViewRepository;
    this.locationViewRepository = locationViewRepository;
    this.lootReportViewRepository = lootReportViewRepository;
    this.wipeRepository = wipeRepository;

    setFilter("mode", gameMode, KEY_REPORT_VIEW.GAME_MODE::eq);

    createView();
  }

  @Override
  protected Class<KeyReportViewRecord> getBeanClass() {
    return KeyReportViewRecord.class;
  }

  @Getter
  @RequiredArgsConstructor
  private enum HistoryMode {
    SINCE_WIPE("Since Wipe"), LAST_60_DAYS("Last 60 Days"), LAST_30_DAYS("Last 30 Days"), LAST_7_DAYS("Last 7 Days"), ALL("All");

    private final String label;
  }

  @Override
  protected Component[] createFilters() {
    ComboBox<KeyViewRecord> keyField = new ComboBox<>("Key");
    keyField.setWidthFull();
    keyField.setItems(keyViewRepository.read());
    keyField.setItemLabelGenerator(entry -> "%s - %s uses".formatted(entry.getName(), entry.getUses()));
    keyField.setRenderer(new ComponentRenderer<>(entry -> {
      Component image = Utils.createImageRenderer(entry, KeyViewRecord::getIconLink, KeyViewRecord::getName);

      Span name = new Span(entry.getName());
      name.getStyle().setFontWeight(Style.FontWeight.BOLD);

      Span uses = new Span("%s uses".formatted(entry.getUses()));
      uses.getStyle().setFontWeight(Style.FontWeight.LIGHTER);

      VerticalLayout layout = new VerticalLayout(name, uses);
      layout.setSpacing(false);

      return new HorizontalLayout(image, layout);
    }));
    keyField.setClearButtonVisible(true);
    keyField.addValueChangeListener(event -> setFilter("key", event.getValue(), keyViewRecord -> KEY_REPORT_VIEW.KEY_ID.eq(keyViewRecord.getItemId())));

    ComboBox<HistoryMode> historyMode = new ComboBox<>("History");
    historyMode.setWidthFull();
    historyMode.setItemLabelGenerator(HistoryMode::getLabel);
    historyMode.setItems(HistoryMode.values());
    historyMode.addValueChangeListener(event -> setFilter("history", event.getValue(), value -> switch (value) {
      case LAST_60_DAYS -> KEY_REPORT_VIEW.REPORTED_AT.greaterThan(LocalDateTime.now().minusDays(60));
      case LAST_30_DAYS -> KEY_REPORT_VIEW.REPORTED_AT.greaterThan(LocalDateTime.now().minusDays(30));
      case LAST_7_DAYS -> KEY_REPORT_VIEW.REPORTED_AT.greaterThan(LocalDateTime.now().minusDays(7));
      case ALL -> DSL.trueCondition();
      case SINCE_WIPE -> KEY_REPORT_VIEW.REPORTED_AT.greaterThan(wipeRepository.getLatestWipe().getWipeDate());
    }));
    historyMode.setValue(HistoryMode.SINCE_WIPE);

    ComboBox<LocationViewRecord> locationField = new ComboBox<>("Location");
    locationField.setWidthFull();
    locationField.setItems(locationViewRepository.read());
    locationField.setItemLabelGenerator(LocationViewRecord::getClean);
    locationField.setClearButtonVisible(true);
    locationField.addValueChangeListener(event -> setFilter("location", event.getValue(), entry -> KEY_REPORT_VIEW.TAGS.contains(new String[]{entry.getLocation()})));

    Checkbox onlySelf = new Checkbox("Show only my reports");
    onlySelf.setWidthFull();
    onlySelf.setValue(false);
    onlySelf.addValueChangeListener(event -> setFilter("onlySelf", event.getValue(), value -> value ? KEY_REPORT_VIEW.REPORTED_BY.eq(authenticatedAccount.getAccount()
        .getId()) : DSL.trueCondition()));

    onlySelf.getStyle().setAlignSelf(Style.AlignSelf.END);
    onlySelf.setHeight(36, Unit.PIXELS);
    onlySelf.getStyle().setPaddingBottom("4px");
    onlySelf.getStyle().set("align-content", "center");

    return new Component[]{keyField, historyMode, locationField, onlySelf};
  }

  @Override
  protected void createGridColumns(Grid<KeyReportViewRecord> grid, GameMode gameMode) {
    grid.addComponentColumn(entry -> Utils.createImageRenderer(entry, KeyReportViewRecord::getIconLink, KeyReportViewRecord::getName))
        .setHeader("Image");
    grid.addColumn("name").setHeader("Name");

    grid.addColumn("value").setRenderer(Utils.roubleRenderer(KeyReportViewRecord::getValue)).setHeader("Total Value");

    grid.addComponentColumn(entry -> {
      HorizontalLayout layout = new HorizontalLayout();
      for (String tag : entry.getTags()) {
        if (tag.equals("Keys")) {
          continue;
        }
        Span badge = new Span(new Span(tag.replace("_", " ")));
        badge.getElement().getThemeList().add("badge");
        layout.add(badge);
      }
      return layout;
    }).setHeader("Location");

    grid.addColumn("reportedAt")
        .setRenderer(new TextRenderer<>(entry -> entry.getReportedAt()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
        .setHeader("Reported At");

    grid.addColumn("reportedBy")
        .setRenderer(new TextRenderer<>(entry -> "User#" + entry.getReportedBy()))
        .setHeader("Reported By");

    grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createItemDetailView));
  }

  private Component createItemDetailView(KeyReportViewRecord keyReport) {
    FormLayout formLayout = new FormLayout();

    Result<LootReportViewRecord> lootReports = lootReportViewRepository.read(LOOT_REPORT_VIEW.KEY_REPORT_ID.eq(keyReport.getId()));

    int totalItemsFound = lootReports.stream().mapToInt(LootReportViewRecord::getCount).sum();

    NumberField itemCountField = new NumberField("Total items found");
    itemCountField.setReadOnly(true);
    itemCountField.setValue((double) totalItemsFound);

    KeyViewRecord key = keyViewRepository.get(KEY_VIEW.ITEM_ID.eq(keyReport.getKeyId()));
    int keyPrice = keyReport.getGameMode() == GameMode.PVP ? key.getPvpFleaPrice() : key.getPveFleaPrice();
    int pricePerUse = keyPrice / key.getUses();

    TextField pricePerUseField = new TextField("Price per Use");
    pricePerUseField.setReadOnly(true);
    pricePerUseField.setValue(Utils.roubleNumber(pricePerUse));

    long totalValue = keyReport.getValue();

    TextField totalValueField = new TextField("Total value");
    totalValueField.setReadOnly(true);
    totalValueField.setValue(Utils.roubleNumber(totalValue));

    long profit = totalValue - pricePerUse;

    TextField profitField = new TextField("Total profit");
    profitField.setReadOnly(true);
    profitField.setValue(Utils.roubleNumber(profit));

    double profitPercent = totalValue / (double) pricePerUse;

    DecimalFormat df = new DecimalFormat("#.##");

    TextField profitPercentField = new TextField("Profit percentage");
    profitPercentField.setReadOnly(true);
    profitPercentField.setValue(df.format(profitPercent * 100) + "%");

    formLayout.add(itemCountField, pricePerUseField, totalValueField, profitField, profitPercentField);

    Grid<LootReportViewRecord> grid = Utils.defaultStripedGrid(LootReportViewRecord.class);

    grid.addComponentColumn(entry -> Utils.createImageRenderer(entry, LootReportViewRecord::getIconLink, LootReportViewRecord::getName, LootReportViewRecord::getHorizontalSlots, LootReportViewRecord::getVerticalSlots))
        .setHeader("Image");

    grid.addColumn("name").setHeader("Name");

    grid.addColumn("count").setHeader("Count");

    grid.addColumn(keyReport.getGameMode() == GameMode.PVP ? "pvpValue" : "pveValue")
        .setRenderer(Utils.roubleRenderer(keyReport.getGameMode() == GameMode.PVP ? LootReportViewRecord::getPvpValue : LootReportViewRecord::getPveValue))
        .setHeader("Value");

    grid.setItems(lootReports);

    grid.setHeight("revert-layer");

    return new VerticalLayout(formLayout, grid);
  }

}
