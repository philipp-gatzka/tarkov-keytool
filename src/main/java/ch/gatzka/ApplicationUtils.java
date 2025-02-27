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
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class ApplicationUtils {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationUtils.class);

  public static final String ROUBLE = "₽";

  public static <V> Grid<V> defaultStripedGrid(Class<V> clazz) {
    Grid<V> grid = new Grid<>(clazz, false);
    grid.setSizeFull();
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    return grid;
  }

  public static String formattedNumber(Number number) {
    Locale locale = VaadinService.getCurrentRequest().getLocale();
    return NumberFormat.getInstance(locale).format(number);
  }

  public static String priceNumber(String currency, Number number) {
    return currency + " " + formattedNumber(number);
  }

  public static String roubleNumber(Number number) {
    return priceNumber(ROUBLE, number);
  }

  public static <V> TextRenderer<V> roubleRenderer(Function<V, Number> mapping) {
    return priceRenderer(_ -> ROUBLE, mapping);
  }

  public static <V> TextRenderer<V> priceRenderer(Function<V, String> currencyMapping,
      Function<V, Number> numberMapping) {
    return new TextRenderer<>(entry -> priceNumber(currencyMapping.apply(entry), numberMapping.apply(entry)));
  }

  public static <V> Component createImageRenderer(V entry, Function<V, String> linkMapping,
      Function<V, String> altMapping) {
    return createImageRenderer(entry, 1, 1, linkMapping, altMapping);
  }

  public static <V> Component createImageRenderer(V entry, ToIntFunction<V> hSizeMapping, ToIntFunction<V> vSizeMapping,
      Function<V, String> linkMapping, Function<V, String> altMapping) {
    final float UNIT = 50f;
    Image image = new Image(linkMapping.apply(entry), altMapping.apply(entry));
    image.setWidth(UNIT * hSizeMapping.applyAsInt(entry), Unit.PIXELS);
    image.setHeight(UNIT * vSizeMapping.applyAsInt(entry), Unit.PIXELS);
    HorizontalLayout layout = new HorizontalLayout(FlexComponent.Alignment.CENTER, image);
    layout.setPadding(false);
    return layout;
  }

  public static <V> Component createImageRenderer(V entry, int hSize, int vSize, Function<V, String> linkMapping,
      Function<V, String> altMapping) {
    return createImageRenderer(entry, _ -> hSize, _ -> vSize, linkMapping, altMapping);
  }

  public static <V> void executeProgressive(V[] array, Consumer<V> consumer) {
    executeProgressive(Arrays.stream(array).toList(), consumer);
  }

  public static <V> void executeProgressive(Collection<V> collection, Consumer<V> consumer) {

    final int total = collection.size();
    if (total == 0) {
      return;
    }
    int processed = 0;
    int lastLoggedProgress = -1;
    long startTime = System.currentTimeMillis();

    for (V item : collection) {
      processed++;
      int progress = (processed * 100) / total;
      long elapsedTime = System.currentTimeMillis() - startTime;
      long estimatedRemainingTime = (total - processed) * (elapsedTime / processed);

      if (progress != lastLoggedProgress) {
        lastLoggedProgress = progress;

        if (progress % 10 == 0) {
          logger.info("\tProgress {}%, Estimated remaining time: {} ms", progress, estimatedRemainingTime);
        } else {
          logger.debug("\tProgress: {}%, Estimated remaining time: {} ms", progress, estimatedRemainingTime);
        }
      }

      consumer.accept(item);
    }
  }

}
