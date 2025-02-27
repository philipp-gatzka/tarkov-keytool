package ch.gatzka.view;

import static ch.gatzka.Routines.getItemValue;
import static ch.gatzka.Tables.ITEM;
import static ch.gatzka.Tables.KEY_VIEW;

import ch.gatzka.ApplicationUtils;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyViewRepository;
import ch.gatzka.repository.LatestItemPriceViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemRecord;
import ch.gatzka.tables.records.KeyViewRecord;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Report")
@Route(value = "report")
@Menu(order = 3, icon = LineAwesomeIconUrl.PLUS_SOLID)
@RolesAllowed("USER")
public class ReportView extends VerticalLayout {

  private final GameMode gameMode;

  private final AuthenticatedAccount authenticatedAccount;

  private final Map<Integer, KeyViewRecord> keys;

  private final Map<Integer, ItemRecord> items;

  private final ComboBox<KeyViewRecord> keySelect = new ComboBox<>();

  private final H3 valueDisplay = new H3();

  private final List<Entry> loot = new ArrayList<>();

  private final Grid<Entry> grid = ApplicationUtils.defaultStripedGrid(Entry.class);

  private final ItemRepository itemRepository;

  private final DefaultDSLContext dslContext;

  private final LatestItemPriceViewRepository latestItemPriceViewRepository;

  public ReportView(AuthenticatedAccount authenticatedAccount, ItemRepository itemRepository,
      KeyViewRepository keyViewRepository, DefaultDSLContext dslContext,
      LatestItemPriceViewRepository latestItemPriceViewRepository) {
    this.authenticatedAccount = authenticatedAccount;
    this.itemRepository = itemRepository;
    this.gameMode = authenticatedAccount.getAccount().getMode();
    this.items = itemRepository.read().intoMap(ITEM.ID, entity -> entity);
    this.keys = keyViewRepository.read().intoMap(KEY_VIEW.ID, entity -> entity);
    this.dslContext = dslContext;
    this.latestItemPriceViewRepository = latestItemPriceViewRepository;

    setSizeFull();

    createHeader();
    createGrid();
    createFooter();
  }

  private void createGrid() {
    grid.addComponentColumn(entry -> ApplicationUtils.createImageRenderer(itemRepository.find(entry.itemId), ItemRecord::getHorizontalSlots, ItemRecord::getVerticalSlots, ItemRecord::getIconLink, ItemRecord::getName))
        .setHeader("Image");
    grid.addColumn(entry -> itemRepository.find(entry.itemId).getName()).setHeader("Name");
    grid.addColumn(entry -> entry.count).setHeader("Count");
    grid.addColumn(entry -> dslContext.select(getItemValue(entry.itemId, gameMode))
            .fetchSingleInto(Integer.class) * entry.count)
        .setRenderer(ApplicationUtils.roubleRenderer(entry -> dslContext.select(getItemValue(entry.itemId, gameMode))
            .fetchSingleInto(Integer.class) * entry.count))
        .setHeader("Value");

    grid.addColumn(new ComponentRenderer<>(Button::new, (button, entry) -> {
      button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      button.addClickListener(_ -> {
        this.loot.remove(entry);
        refreshGrid();
        refreshValueDisplay();
      });
      button.setIcon(new Icon(VaadinIcon.TRASH));
    })).setHeader("Manage");

    add(grid);
  }

  private void createFooter() {
    Button saveButton = new Button("Save");
    saveButton.addClickListener(this::save);
    saveButton.setIcon(LineAwesomeIcon.SAVE_SOLID.create());

    Button resetButton = new Button("Reset");
    resetButton.addClickListener(this::reset);
    resetButton.setIcon(LineAwesomeIcon.TRASH_ALT.create());

    Button addButton = new Button("Add");
    addButton.addClickListener(this::add);
    addButton.setIcon(LineAwesomeIcon.PLUS_SOLID.create());

    valueDisplay.setWidthFull();
    valueDisplay.getStyle().setFontWeight(Style.FontWeight.BOLD);
    valueDisplay.getStyle().setAlignSelf(Style.AlignSelf.CENTER);
    valueDisplay.getStyle().setTextAlign(Style.TextAlign.RIGHT);

    HorizontalLayout footer = new HorizontalLayout(saveButton, resetButton, addButton, valueDisplay);
    footer.setWidthFull();
    add(footer);
  }

  private void add(ClickEvent<Button> buttonClickEvent) {
    openDialog((itemId, count) -> {
      Optional<Entry> first = loot.stream().filter(entry -> entry.itemId == itemId).findFirst();

      if (first.isPresent()) {
        Entry entry = first.get();
        loot.remove(entry);
        loot.add(new Entry(itemId, entry.count + count));
      } else {
        loot.add(new Entry(itemId, count));
      }

      refreshGrid();
      refreshValueDisplay();
    });
  }

  private void refreshGrid() {
    grid.setItems(loot);
  }

  private void refreshValueDisplay() {
    int keyPrice = 0;
    int usePrice = 0;

    Optional<KeyViewRecord> optionalSelectedKey = keySelect.getOptionalValue();
    if (optionalSelectedKey.isPresent()) {
      KeyViewRecord key = optionalSelectedKey.get();
      keyPrice = latestItemPriceViewRepository.findByItemIdAndMode(key.getId(), gameMode).getFleaPrice();
      usePrice = keyPrice / key.getUses();
    }

    int totalValue = loot.stream()
        .mapToInt(entry -> dslContext.select(getItemValue(entry.itemId, gameMode))
            .fetchSingleInto(Integer.class) * entry.count)
        .sum();

    double satisfaction = 0;

    if (usePrice != 0) {
      satisfaction = Math.floor((totalValue / (float) usePrice) * 100);
    }

    valueDisplay.setText("Key Price: " + ApplicationUtils.roubleNumber(keyPrice) + " Use Price: " + ApplicationUtils.roubleNumber(usePrice) + " Total value: " + ApplicationUtils.roubleNumber(totalValue) + " Satisfaction: " + satisfaction + "%");

    if (satisfaction >= 100d) {
      valueDisplay.getStyle().set("color", "var(--lumo-success-text-color)");
    } else {
      valueDisplay.getStyle().set("color", "var(--lumo-error-text-color)");
    }
  }

  private void save(ClickEvent<Button> buttonClickEvent) {

  }

  private void createHeader() {
    keySelect.setLabel("Select Key");
    keySelect.setItems(keys.values());
    keySelect.setItemLabelGenerator(KeyViewRecord::getName);
    keySelect.setRenderer(new ComponentRenderer<>(entry -> {
      Component imageRenderer = ApplicationUtils.createImageRenderer(entry, KeyViewRecord::getIconLink, KeyViewRecord::getName);
      Span nameRenderer = new Span(entry.getName());
      nameRenderer.getStyle().setFontWeight(Style.FontWeight.BOLD);
      nameRenderer.getStyle().setAlignSelf(Style.AlignSelf.CENTER);
      return new HorizontalLayout(imageRenderer, nameRenderer);
    }));
    keySelect.setClearButtonVisible(true);
    keySelect.setWidthFull();
    keySelect.addValueChangeListener(event -> {
      refreshValueDisplay();
    });

    HorizontalLayout header = new HorizontalLayout(keySelect);
    header.setWidthFull();
    add(header);
  }

  private void reset(ClickEvent<Button> buttonClickEvent) {
    keySelect.clear();
    loot.clear();

    refreshGrid();
    refreshValueDisplay();
  }

  private void openDialog(BiConsumer<Integer, Integer> consumer) {
    Dialog dialog = new Dialog();

    dialog.setHeaderTitle("Report Loot");
    dialog.setMinWidth(270, Unit.PIXELS);
    dialog.setWidth(30, Unit.PERCENTAGE);
    dialog.setMaxWidth(40, Unit.PERCENTAGE);

    ComboBox<ItemRecord> selectItemBox = new ComboBox<>();
    selectItemBox.setAutofocus(true);
    selectItemBox.setLabel("Item");
    selectItemBox.setItems(items.values());
    selectItemBox.setItemLabelGenerator(ItemRecord::getName);
    selectItemBox.setWidth(100, Unit.PERCENTAGE);
    selectItemBox.setRequired(true);
    selectItemBox.setRequiredIndicatorVisible(true);
    selectItemBox.setManualValidation(true);
    selectItemBox.setErrorMessage("Item is required");

    IntegerField count = new IntegerField("Count");
    count.setValue(1);
    count.setMin(1);
    count.setStepButtonsVisible(true);
    count.setWidth(100, Unit.PERCENTAGE);
    count.setRequired(true);
    count.setRequiredIndicatorVisible(true);
    count.setManualValidation(true);
    count.setErrorMessage("Count is required");

    VerticalLayout dialogLayout = new VerticalLayout();
    dialogLayout.add(selectItemBox, count);

    dialog.add(dialogLayout);

    Button saveButton = new Button("Save");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(_ -> {
      if (selectItemBox.isEmpty()) {
        selectItemBox.setInvalid(true);
      } else if (count.isEmpty()) {
        selectItemBox.setInvalid(false);
        count.setInvalid(true);
      } else {
        count.setInvalid(false);
        consumer.accept(selectItemBox.getValue().getId(), count.getValue());
        dialog.close();
      }
    });

    Button cancelButton = new Button("Cancel", _ -> dialog.close());
    dialog.getFooter().add(cancelButton);
    dialog.getFooter().add(saveButton);

    dialog.open();
  }

  @RequiredArgsConstructor
  public static class Entry {

    private final int itemId;

    private final int count;

  }

}
