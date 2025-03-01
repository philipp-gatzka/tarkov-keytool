package ch.gatzka.view;

import ch.gatzka.Utils;
import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.service.ItemValueService;
import ch.gatzka.table.ItemRepository;
import ch.gatzka.table.KeyReportRepository;
import ch.gatzka.table.LootReportRepository;
import ch.gatzka.tables.records.ItemRecord;
import ch.gatzka.tables.records.KeyViewRecord;
import ch.gatzka.view.model.LootModel;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Report")
@Route("report")
@Menu(order = 3, icon = LineAwesomeIconUrl.PEN_SOLID)
@RolesAllowed("USER")
public class ReportView extends VerticalLayout {

  private final KeyViewRepository keyViewRepository;

  private final AuthenticatedAccount authenticatedAccount;

  private final GameMode gameMode;

  private final List<LootModel> list = new ArrayList<>();

  private final Grid<LootModel> grid = Utils.defaultStripedGrid(LootModel.class);

  private final List<ItemRecord> items;

  private final ItemValueService itemValueService;

  private final ComboBox<KeyViewRecord> keyField = new ComboBox<>("Select key");

  private final KeyReportRepository keyReportRepository;

  private final LootReportRepository lootReportRepository;

  public ReportView(KeyViewRepository keyViewRepository, AuthenticatedAccount authenticatedAccount,
      ItemRepository itemRepository, ItemValueService itemValueService, KeyReportRepository keyReportRepository,
      LootReportRepository lootReportRepository) {
    this.keyViewRepository = keyViewRepository;
    this.authenticatedAccount = authenticatedAccount;
    this.gameMode = authenticatedAccount.getAccount().getGameMode();
    this.items = itemRepository.read();
    this.keyReportRepository = keyReportRepository;
    this.lootReportRepository = lootReportRepository;

    setSizeFull();

    createHeader();
    createGrid();
    createFooter();
    this.itemValueService = itemValueService;
  }

  private void createHeader() {
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

    HorizontalLayout header = new HorizontalLayout(keyField);
    header.setWidthFull();
    add(header);
  }

  private void createGrid() {
    grid.setSizeFull();

    grid.addComponentColumn(entry -> Utils.createImageRenderer(entry, LootModel::getIconLink, LootModel::getName))
        .setHeader("Image");
    grid.addColumn("name").setHeader("Name");
    grid.addColumn("count").setHeader("Count");

    grid.addColumn(entry -> itemValueService.itemValue(entry.getItem(), gameMode) * entry.getCount())
        .setRenderer(Utils.roubleRenderer(entry -> itemValueService.itemValue(entry.getItem(), gameMode) * entry.getCount()))
        .setHeader("Value");

    grid.addComponentColumn((ValueProvider<LootModel, Component>) lootModel -> {
      Button buttonAdd = new Button(VaadinIcon.PLUS.create());
      buttonAdd.addClickListener(_ -> addLoot(new LootModel(lootModel.getItem(), lootModel.getCount() + 1)));

      Button buttonRemove = new Button(VaadinIcon.MINUS.create());
      buttonRemove.addClickListener(_ -> addLoot(new LootModel(lootModel.getItem(), lootModel.getCount() - 1)));

      Button buttonTrash = new Button(VaadinIcon.TRASH.create());
      buttonTrash.addClickListener(_ -> addLoot(new LootModel(lootModel.getItem(), 0)));

      return new HorizontalLayout(buttonAdd, buttonRemove, buttonTrash);
    }).setHeader("Actions");

    grid.setItems(list);

    add(grid);
  }

  private void createFooter() {
    Button buttonAdd = new Button("Add Item");
    buttonAdd.setIcon(VaadinIcon.PLUS.create());
    buttonAdd.addClickListener(this::onAdd);

    Button buttonSave = new Button("Save");
    buttonSave.setIcon(VaadinIcon.HARDDRIVE.create());
    buttonSave.addClickListener(this::onSave);

    Button buttonReset = new Button("Reset");
    buttonReset.setIcon(VaadinIcon.TRASH.create());
    buttonReset.addClickListener(this::onReset);

    HorizontalLayout footer = new HorizontalLayout(buttonAdd, buttonSave, buttonReset);
    footer.setWidthFull();
    add(footer);
  }

  private void onReset(ClickEvent<Button> buttonClickEvent) {
    list.clear();
    keyField.setValue(null);
    refreshGrid();
  }

  private void addLoot(LootModel lootModel) {
    if (lootModel.getCount() <= 0) {
      list.removeIf(loot -> loot.getItem().equals(lootModel.getItem()));
    } else if (list.stream().anyMatch(loot -> loot.getItem().equals(lootModel.getItem()))) {
      list.removeIf(loot -> loot.getItem().equals(lootModel.getItem()));
      list.add(lootModel);
    } else {
      list.add(lootModel);
    }
    refreshGrid();
  }

  private void onAdd(ClickEvent<Button> buttonClickEvent) {
    openDialog(this::addLoot);
  }

  private void onSave(ClickEvent<Button> buttonClickEvent) {
    if (keyField.getValue() == null) {
      Notification notification = new Notification("Please select a key first", 3000, Notification.Position.MIDDLE);
      notification.setThemeName("error");
      notification.open();
      return;
    }

    Integer keyReportId = keyReportRepository.insertWithId(entity -> entity.setReportedBy(authenticatedAccount.getAccount()
        .getId()).setKeyId(keyField.getValue().getItemId()).setGameMode(gameMode));

    list.forEach(lootModel -> lootReportRepository.insert(entity -> entity.setItemId(lootModel.getItem().getId())
        .setCount(lootModel.getCount())
        .setKeyReportId(keyReportId)));

    Notification notification = new Notification("Report saved successfully", 3000, Notification.Position.MIDDLE);
    notification.setThemeName("success");
    notification.open();

    onReset(null);
  }

  private void refreshGrid() {
    grid.setItems(list);
  }

  private void openDialog(Consumer<LootModel> onSubmit) {
    Dialog dialog = new Dialog();

    dialog.setHeaderTitle("Report Loot");
    dialog.setMinWidth(270, Unit.PIXELS);
    dialog.setWidth(30, Unit.PERCENTAGE);
    dialog.setMaxWidth(40, Unit.PERCENTAGE);

    ComboBox<ItemRecord> selectItemBox = new ComboBox<>();
    selectItemBox.setAutofocus(true);
    selectItemBox.setLabel("Item");
    selectItemBox.setItems(items);
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
        onSubmit.accept(new LootModel(selectItemBox.getValue(), count.getValue()));
        dialog.close();
      }
    });

    Button cancelButton = new Button("Cancel", _ -> dialog.close());
    dialog.getFooter().add(cancelButton);
    dialog.getFooter().add(saveButton);

    dialog.open();
  }

}
