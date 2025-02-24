package ch.gatzka.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Main")
@Route(value = "")
@Menu(order = 0, icon = LineAwesomeIconUrl.ADDRESS_CARD)
@AnonymousAllowed
public class MainView extends VerticalLayout {
}
