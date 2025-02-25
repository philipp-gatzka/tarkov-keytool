package ch.gatzka.security;

import ch.gatzka.enums.AccountRole;
import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.AccountRepository;
import ch.gatzka.tables.records.AccountRecord;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service extends DefaultOAuth2UserService {

  private final AccountRepository accountRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String email = oAuth2User.getAttribute("email");

    if (!accountRepository.existsByEmail(email)) {
      accountRepository.insert(entity -> entity.setEmail(email)
                                               .setRoles(new AccountRole[]{AccountRole.USER})
                                               .setMode(GameMode.PVP));
    }

    AccountRecord account = accountRepository.findByEmail(email);
    return new DefaultOAuth2User(getAuthorities(account), oAuth2User.getAttributes(), "email");
  }

  private List<? extends GrantedAuthority> getAuthorities(AccountRecord account) {
    return Arrays.stream(account.getRoles()).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
  }

}
