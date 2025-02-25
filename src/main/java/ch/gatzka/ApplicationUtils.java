package ch.gatzka;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.server.VaadinService;
import lombok.experimental.UtilityClass;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;

@UtilityClass
public class ApplicationUtils {

    public static final String ROUBLE = "₽";

    public static <V> Grid<V> defaultStripedGrid(Class<V> clazz) {
        Grid<V> grid = new Grid<>(clazz, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        return grid;
    }

    public static String formattedNumber(Number number, Locale locale) {
        locale = VaadinService.getCurrentRequest().getLocale();
        return NumberFormat.getInstance(locale).format(number);
    }

    public static String priceNumber(String currency, Number number, Locale locale) {
        return currency + " " + formattedNumber(number, locale);
    }

    public static String roubleNumber(Number number, Locale locale) {
        return priceNumber(ROUBLE, number, locale);
    }

    public static <V> TextRenderer<V> roubleRenderer(Function<V, Number> mapping, Locale locale) {
        return priceRenderer(_ -> ROUBLE, mapping, locale);
    }

    public static <V> TextRenderer<V> priceRenderer(Function<V, String> currencyMapping, Function<V, Number> numberMapping, Locale locale) {
        return new TextRenderer<>(entry -> priceNumber(currencyMapping.apply(entry), numberMapping.apply(entry), locale));
    }

    public static <V> Component createImageRenderer(V entry, Function<V, String> linkMapping, Function<V, String> altMapping) {
        Image image = new Image(linkMapping.apply(entry), altMapping.apply(entry));
        image.setMaxHeight(75, Unit.PIXELS);
        HorizontalLayout layout = new HorizontalLayout(FlexComponent.Alignment.CENTER, image);
        layout.setPadding(false);
        return layout;
    }
}
