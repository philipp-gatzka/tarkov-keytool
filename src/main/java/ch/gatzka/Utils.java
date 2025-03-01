package ch.gatzka;

import ch.gatzka.enums.Currency;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.server.VaadinService;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class Utils {

  public static <V> void executeProgressive(String process, List<V> collection, Consumer<V> consumer) {

    final int total = collection.size();
    if (total == 0) {
      return;
    }
    int processed = 0;
    int lastLoggedProgress = -1;
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < collection.size(); i++) {
      V item = collection.get(i);
      processed++;
      int progress = (processed * 100) / total;
      long elapsedTime = System.currentTimeMillis() - startTime;
      long estimatedRemainingTime = (total - processed) * (elapsedTime / processed);

      if (progress != lastLoggedProgress) {
        lastLoggedProgress = progress;

        if (progress % 10 == 0) {
          log.info("{} | Progress {}% ({}/{}), Estimated remaining time: {} ms", process, progress, i, collection.size(), estimatedRemainingTime);
        } else {
          log.debug("{} | Progress: {}% ({}/{}), Estimated remaining time: {} ms", process, progress, i, collection.size(), estimatedRemainingTime);
        }
      }
      consumer.accept(item);
    }
  }

  public static <V> Grid<V> defaultStripedGrid(Class<V> clazz) {
    Grid<V> grid = new Grid<>(clazz, false);
    grid.setSizeFull();
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    return grid;
  }

  public static <V> Component createImageRenderer(V entry, Function<V, String> linkMapping,
      Function<V, String> altMapping) {
    Image image = new Image(linkMapping.apply(entry), altMapping.apply(entry));
    image.setMaxHeight(75, Unit.PIXELS);
    HorizontalLayout layout = new HorizontalLayout(FlexComponent.Alignment.CENTER, image);
    layout.setPadding(false);
    return layout;
  }

  public static <V> Component createImageRenderer(V entry, Function<V, String> linkMapping,
      Function<V, String> altMapping, ToIntFunction<V> hSizeMapping, ToIntFunction<V> vSizeMapping) {
    Image image = new Image(linkMapping.apply(entry), altMapping.apply(entry));
    image.setHeight(vSizeMapping.applyAsInt(entry) * 50f, Unit.PIXELS);
    image.setWidth(hSizeMapping.applyAsInt(entry) * 50f, Unit.PIXELS);
    HorizontalLayout layout = new HorizontalLayout(FlexComponent.Alignment.CENTER, image);
    layout.setPadding(false);
    return layout;
  }

  public static String formattedNumber(Number number) {
    Locale locale = VaadinService.getCurrentRequest().getLocale();
    return NumberFormat.getInstance(locale).format(number);
  }

  public static String priceNumber(Currency currency, Number number) {
    return currency.getLiteral() + " " + formattedNumber(number);
  }

  public static String roubleNumber(Number number) {
    return priceNumber(Currency.₽, number);
  }

  public static <V> TextRenderer<V> roubleRenderer(Function<V, Number> mapping) {
    return priceRenderer(_ -> Currency.₽, mapping);
  }

  public static <V> TextRenderer<V> priceRenderer(Function<V, Currency> currencyMapping,
      Function<V, Number> numberMapping) {
    return new TextRenderer<>(entry -> priceNumber(currencyMapping.apply(entry), numberMapping.apply(entry)));
  }

}
