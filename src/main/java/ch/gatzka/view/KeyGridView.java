package ch.gatzka.view;

import static ch.gatzka.ApplicationUtils.defaultStripedGrid;

import ch.gatzka.ApplicationUtils;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.KeyGridViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.KeyGridViewRecord;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

@PageTitle("Keys")
@Route(value = "keys")
@Menu(order = 2, icon = LineAwesomeIconUrl.KEY_SOLID)
@AnonymousAllowed
public class KeyGridView extends VerticalLayout {

  private final Grid<KeyGridViewRecord> grid = defaultStripedGrid(KeyGridViewRecord.class);

  private final Map<String, Condition> filterConditions = new HashMap<>();

  private final GameMode gameMode;

  private final transient KeyGridViewRepository keyGridViewRepository;

  public KeyGridView(KeyGridViewRepository keyGridViewRepository, AuthenticatedAccount authenticatedAccount) {
    this.keyGridViewRepository = keyGridViewRepository;
    this.gameMode = authenticatedAccount.isAuthenticated() ? authenticatedAccount.getAccount().getMode() : GameMode.PVP;

    setSizeFull();

    // createHeader();
    createGrid();

    refreshGrid();
  }

  private <V> HasValue.ValueChangeListener<HasValue.ValueChangeEvent<V>> createFilterListener(String name,
      Function<V, Condition> conditionCreator) {
    return event -> {
      if (event.getValue() == null) {
        filterConditions.remove(name);
      } else {
        filterConditions.put(name, conditionCreator.apply(event.getValue()));
      }
    };
  }

  private void refreshGrid() {
    grid.setItems(keyGridViewRepository.read(filterConditions.values()));
  }

  private void createGrid() {
    I18NProvider i18n = VaadinService.getCurrent().getInstantiator().getI18NProvider();
    Locale locale = VaadinService.getCurrentRequest().getLocale();

    grid.addComponentColumn(entry -> ApplicationUtils.createImageRenderer(entry, KeyGridViewRecord::getIconLink, KeyGridViewRecord::getName))
        .setHeader(i18n.getTranslation("keys.grid.column.image.header", locale));
    grid.addColumn("name").setHeader(i18n.getTranslation("keys.grid.column.name.header", locale));

    grid.addComponentColumn(entry -> {
      boolean isBanned = gameMode == GameMode.PVP ? entry.getPvpFleaBanned() : entry.getPveFleaBanned();
      Icon icon = (isBanned ? LumoIcon.CROSS : LumoIcon.CHECKMARK).create();
      icon.setColor(isBanned ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
      return icon;
    }).setHeader(i18n.getTranslation("keys.grid.column.banned_on_flea.header", locale));

    grid.addColumn("uses").setHeader(i18n.getTranslation("keys.grid.column.uses.header", locale));

    grid.addColumn(gameMode == GameMode.PVP ? "pvpPrice" : "pvePrice")
        .setRenderer(ApplicationUtils.roubleRenderer(entry -> switch (gameMode) {
          case PVE:
            yield entry.getPvePrice();
          case PVP:
            yield entry.getPvpPrice();
        }))
        .setHeader(i18n.getTranslation("keys.grid.column.flea_value.header", locale));

    grid.addColumn(gameMode == GameMode.PVP ? "pvpPricePerUse" : "pvePricePerUse")
        .setRenderer(ApplicationUtils.roubleRenderer(entry -> switch (gameMode) {
          case PVE:
            yield entry.getPvePricePerUse();
          case PVP:
            yield entry.getPvpPricePerUse();
        }))
        .setHeader(i18n.getTranslation("keys.grid.column.flea_value_per_use.header", locale));

    grid.addColumn("traderPrice")
        .setRenderer(ApplicationUtils.priceRenderer(KeyGridViewRecord::getTraderCurrency, KeyGridViewRecord::getTraderPrice))
        .setHeader(i18n.getTranslation("keys.grid.column.trader_value.header", locale));

    grid.addColumn("traderPricePerUse")
        .setRenderer(ApplicationUtils.priceRenderer(KeyGridViewRecord::getTraderCurrency, KeyGridViewRecord::getTraderPrice))
        .setHeader(i18n.getTranslation("keys.grid.column.trader_value_per_use.header", locale));

    add(grid);
  }

}
