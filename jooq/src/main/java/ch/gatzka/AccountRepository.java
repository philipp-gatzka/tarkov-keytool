package ch.gatzka;

import static ch.gatzka.Tables.ACCOUNT;

import ch.gatzka.core.Sequenced;
import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.AccountRecord;
import java.util.function.UnaryOperator;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class AccountRepository extends TableRepository<AccountRecord> implements Sequenced<AccountRecord, Integer> {

  protected AccountRepository(DSLContext dslContext) {
    super(dslContext, ACCOUNT);
  }

  public boolean existsByEmail(String email) {
    return exists(ACCOUNT.EMAIL.eq(email));
  }

  public AccountRecord getByEmail(String email) {
    return get(ACCOUNT.EMAIL.eq(email));
  }

  @Override
  public int insert(UnaryOperator<AccountRecord> mapping) {
    return dslContext.insertInto(ACCOUNT)
        .set(mapping.apply(dslContext.newRecord(ACCOUNT)))
        .returningResult(ACCOUNT.ID)
        .fetchSingleInto(Integer.class);
  }

  @Override
  public void delete(Integer id) {
    delete(ACCOUNT.ID.eq(id));
  }

  @Override
  public void update(UnaryOperator<AccountRecord> mapping, Integer id) {
    update(mapping, ACCOUNT.ID.eq(id));
  }

}
