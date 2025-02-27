plugins {
    id("java")
    alias(libs.plugins.sonar)
}

sonar {
    properties {
        property("sonar.projectKey", "ch.gatzka:tarkov-keytool")
        property("sonar.organization", "philipp-gatzka-org")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

allprojects {
    group = "ch.gatzka"

    repositories {
        mavenCentral()
    }

    tasks.withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.release.set(23)
    }

    extensions.findByType(JavaPluginExtension::class)?.apply {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }
}