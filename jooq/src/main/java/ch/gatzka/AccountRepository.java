package ch.gatzka;

import static ch.gatzka.Tables.ACCOUNT;

import ch.gatzka.core.Sequenced;
import ch.gatzka.core.TableRepository;
import ch.gatzka.tables.records.AccountRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;
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
  public TableField<AccountRecord, Integer> getSequencedField() {
    return ACCOUNT.ID;
  }

}
