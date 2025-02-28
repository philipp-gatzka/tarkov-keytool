package ch.gatzka;

import java.util.List;
import java.util.function.Consumer;
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

}
