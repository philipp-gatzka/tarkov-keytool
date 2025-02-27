plugins {
    id("java")

    alias(libs.plugins.sonar)
}

group = "ch.gatzka"

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(23)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

sonar {
    properties {
        property("sonar.projectKey", "ch.gatzka:tarkov-keytool")
        property("sonar.organization", "philipp-gatzka-org")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}