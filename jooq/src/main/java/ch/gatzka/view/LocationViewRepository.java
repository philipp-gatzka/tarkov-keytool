package ch.gatzka.view;

import static ch.gatzka.Tables.LOCATION_VIEW;

import ch.gatzka.core.ViewRepository;
import ch.gatzka.tables.records.LocationViewRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class LocationViewRepository extends ViewRepository<LocationViewRecord> {

  protected LocationViewRepository(DSLContext dslContext) {
    super(dslContext, LOCATION_VIEW);
  }

}
