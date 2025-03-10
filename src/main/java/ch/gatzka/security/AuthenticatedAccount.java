package ch.gatzka.security;

import ch.gatzka.table.AccountRepository;
import ch.gatzka.tables.records.AccountRecord;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedAccount {

  private final AccountRepository accountRepository;

  private final AuthenticationContext authenticationContext;

  public Optional<Info> getOptional() {
    return authenticationContext.getAuthenticatedUser(DefaultOAuth2User.class)
        .filter(userDetails -> accountRepository.existsByEmail(userDetails.getName()))
        .map(userDetails -> new Info(userDetails.getAttributes(), accountRepository.getByEmail(userDetails.getName())));
  }

  public Info get() {
    return getOptional().orElseThrow(() -> new IllegalStateException("User is not authenticated"));
  }

  public AccountRecord getAccount() {
    return get().account();
  }

  public String getString(String key) {
    return String.valueOf(get().attributes().get(key));
  }

  public boolean isAuthenticated() {
    return authenticationContext.isAuthenticated();
  }

  public void logout() {
    authenticationContext.logout();
  }

  public record Info(Map<String, Object> attributes, AccountRecord account) {

  }

}
