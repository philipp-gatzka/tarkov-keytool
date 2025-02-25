package ch.gatzka;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@Theme(value = "tarkov-keytool", variant = Lumo.DARK)
@SpringBootApplication
public class Application implements AppShellConfigurator {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public I18NProvider i18NProvider() {
    return new I18NProvider() {
      @Override
      public List<Locale> getProvidedLocales() {
        return List.of(Locale.ENGLISH);
      }

      @Override
      public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
          log.warn("Got lang request for key with null value!");
          return "";
        }

        final ResourceBundle bundle = ResourceBundle.getBundle("translation", locale);

        String value;
        try {
          value = bundle.getString(key);
        } catch (final MissingResourceException e) {
          log.error("Missing resource for locale {}: '{}'", locale, key);
          return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
          value = MessageFormat.format(value, params);
        }
        return value;
      }
    };
  }

}
