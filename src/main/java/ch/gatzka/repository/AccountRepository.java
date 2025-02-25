package ch.gatzka.repository;

import static ch.gatzka.Sequences.ACCOUNT_ID_SEQ;
import static ch.gatzka.Tables.ACCOUNT;

import ch.gatzka.repository.base.SequencedWriteRepository;
import ch.gatzka.tables.records.AccountRecord;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class AccountRepository extends SequencedWriteRepository<AccountRecord> {

  public AccountRepository(DSLContext dslContext) {
    super(dslContext, ACCOUNT, ACCOUNT_ID_SEQ, ACCOUNT.ID);
  }

  public boolean existsByEmail(String email) {
    return exists(ACCOUNT.EMAIL.eq(email));
  }

  public AccountRecord findByEmail(String email) {
    return find(ACCOUNT.EMAIL.eq(email));
  }

  public Optional<AccountRecord> findOptionalByEmail(String email) {
    return findOptional(ACCOUNT.EMAIL.eq(email));
  }

}
