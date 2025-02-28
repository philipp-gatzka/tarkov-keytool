package ch.gatzka.core;

import java.util.function.UnaryOperator;
import org.jooq.UpdatableRecord;

public interface Sequenced<R extends UpdatableRecord<R>, I extends Number> {

  int insert(UnaryOperator<R> mapping);

  void delete(I id);

  void update(UnaryOperator<R> mapping, I id);

}
