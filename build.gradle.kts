plugins {
    id("java")
    alias(libs.plugins.sonar)
    alias(libs.plugins.lombok)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.vaadin)
}

dependencies {
    implementation(platform(libs.vaadin.bom))
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.vaadin)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.oauth2)
    implementation(libs.spring.boot.jooq)
    implementation(libs.spring.boot.actuator)

    implementation(project(":jooq"))

    implementation(libs.lineawesome)

    developmentOnly(libs.spring.boot.devtools)

    runtimeOnly(libs.postgres)
}

sonar {
    properties {
        property("sonar.projectKey", "ch.gatzka:tarkov-keytool")
        property("sonar.organization", "philipp-gatzka-org")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

configurations.runtimeClasspath.configure {
    extendsFrom(configurations.developmentOnly.get())
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