package ch.gatzka.view;

import static ch.gatzka.Tables.KEY_GRID_VIEW;

import ch.gatzka.Utils;
import ch.gatzka.core.Repository;
import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.KeyGridViewRecord;
import ch.gatzka.tables.records.LocationViewRecord;
import ch.gatzka.view.core.FilteredGridView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Keys")
@Route("keys")
@Menu(order = 2, icon = LineAwesomeIconUrl.KEY_SOLID)
@AnonymousAllowed
public class KeyView extends FilteredGridView<KeyGridViewRecord> {

  private final LocationViewRepository locationViewRepository;

  protected KeyView(Repository<KeyGridViewRecord> repository, AuthenticatedAccount authenticatedAccount,
      LocationViewRepository locationViewRepository) {
    super(repository, authenticatedAccount);
    this.locationViewRepository = locationViewRepository;
    createView();
  }

  @Override
  protected Class<KeyGridViewRecord> getBeanClass() {
    return KeyGridViewRecord.class;
  }

  @Override
  protected Component[] createFilters() {
    TextField nameField = new TextField("Name");
    nameField.setWidthFull();
    nameField.addValueChangeListener(event -> setFilter("name", event.getValue(), value -> KEY_GRID_VIEW.NAME.likeIgnoreCase("%" + value + "%")));

    ComboBox<LocationViewRecord> locationField = new ComboBox<>("Location");
    locationField.setWidthFull();
    locationField.setItems(locationViewRepository.read());
    locationField.setItemLabelGenerator(LocationViewRecord::getClean);
    locationField.addValueChangeListener(event -> setFilter("location", event.getValue(), entry -> KEY_GRID_VIEW.TAGS.contains(new String[]{entry.getLocation()})));
    locationField.setClearButtonVisible(true);

    return new Component[]{nameField, locationField};
  }

  @Override
  protected void createGridColumns(Grid<KeyGridViewRecord> grid, GameMode gameMode) {
    grid.addComponentColumn(entry -> Utils.createImageRenderer(entry, KeyGridViewRecord::getIconLink, KeyGridViewRecord::getName))
        .setHeader("Image");
    grid.addColumn("name").setHeader("Name");

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
    }).setHeader("Tags");
  }

}
