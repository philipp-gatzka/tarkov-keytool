package ch.gatzka.service;

import ch.gatzka.enums.AccountRole;
import ch.gatzka.enums.GameMode;
import ch.gatzka.tables.records.AccountRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;

import java.util.Optional;

import static ch.gatzka.Tables.ACCOUNT;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final DSLContext dslContext;

    public boolean accountExistsByEmail(String email) {
        return dslContext.fetchExists(ACCOUNT, ACCOUNT.EMAIL.eq(email));
    }

    public void createUserAccount(String email) {
        dslContext.insertInto(ACCOUNT)
                .set(ACCOUNT.EMAIL, email)
                .set(ACCOUNT.ROLES, new AccountRole[]{AccountRole.USER})
                .set(ACCOUNT.MODE, GameMode.PVP)
                .execute();
    }

    public AccountRecord getAccountByEmail(String email) {
        return dslContext.fetchSingle(ACCOUNT, ACCOUNT.EMAIL.eq(email));
    }

    public Optional<AccountRecord> findAccountByEmail(String email){
        return dslContext.fetchOptional(ACCOUNT, ACCOUNT.EMAIL.eq(email));
    }

}
