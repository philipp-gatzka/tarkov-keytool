package ch.gatzka;

import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;

@Layout
@AnonymousAllowed
public class ApplicationLayout extends AppLayout {

  private final AuthenticatedAccount authenticatedUser;

  private H1 title;

  public ApplicationLayout(AuthenticatedAccount authenticatedUser) {
    this.authenticatedUser = authenticatedUser;

    setPrimarySection(Section.DRAWER);

    addHeaderContent();
    addDrawerContent();

  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.setAriaLabel("Menu toggle");

    title = new H1();
    title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

    addToNavbar(true, toggle, title);
  }

  private Footer createFooter() {
    Select<GameMode> gameMode = new Select<>();
    gameMode.setItems(GameMode.values());
    gameMode.setItemLabelGenerator(GameMode::getLiteral);
    gameMode.setWidthFull();

    if (authenticatedUser.isAuthenticated()) {
      gameMode.setValue(authenticatedUser.getAccount().getMode());
    } else {
      gameMode.setValue(GameMode.PVP);
    }

    gameMode.addValueChangeListener(event -> {
      if (!authenticatedUser.isAuthenticated() && event.getValue() != GameMode.PVP) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);

        dialog.setHeaderTitle("Game mode selection requires login");

        Anchor loginLink = new Anchor("/oauth2/authorization/google", "Login with Google");
        loginLink.setRouterIgnore(true);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create());

        cancelButton.addClickListener(_ -> {
          gameMode.setValue(GameMode.PVP);

          // FIX REOPENING OF DIALOG

          dialog.close();
        });

        dialog.getFooter().add(loginLink, cancelButton);

        dialog.open();
      } else {
        authenticatedUser.getAccount().setMode(event.getValue()).update();
        UI.getCurrent().getPage().reload();
      }
    });

    VerticalLayout layout = new VerticalLayout(gameMode);
    layout.setPadding(false);
    layout.setSizeFull();

    if (authenticatedUser.isAuthenticated()) {
      String picture = authenticatedUser.getString("picture");

      Avatar avatar = new Avatar(authenticatedUser.getString("email"));
      avatar.setImage(picture);

      H5 name = new H5(authenticatedUser.getString("name"));
      name.setWidthFull();

      HorizontalLayout userLayout = new HorizontalLayout(avatar, name);
      userLayout.setAlignSelf(FlexComponent.Alignment.CENTER, name);
      userLayout.setWidthFull();

      Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
      logoutButton.setWidthFull();

      logoutButton.addClickListener(_ -> authenticatedUser.logout());

      layout.add(userLayout, logoutButton);
    } else {
      Anchor loginLink = new Anchor("/oauth2/authorization/google", "Login with Google");
      loginLink.setRouterIgnore(true);
      loginLink.setWidthFull();
      loginLink.getStyle().set("text-align", "center");
      layout.add(loginLink);
    }

    return new Footer(layout);
  }

  private void addDrawerContent() {
    Span appName = new Span("Tarkov Keytool");
    appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
    Header header = new Header(appName);

    Scroller scroller = new Scroller(createNavigation());

    addToDrawer(header, scroller, createFooter());
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
    menuEntries.forEach(entry -> {
      if (entry.icon() != null) {
        nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
      } else {
        nav.addItem(new SideNavItem(entry.title(), entry.path()));
      }
    });

    return nav;
  }

  @Override
  protected void afterNavigation() {
    super.afterNavigation();
    title.setText(getCurrentPageTitle());
  }

  private String getCurrentPageTitle() {
    return MenuConfiguration.getPageHeader(getContent()).orElse("");
  }

}
