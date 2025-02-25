import org.apache.tools.ant.filters.ReplaceTokens
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.jooq.meta.jaxb.MatcherTransformType
import org.testcontainers.containers.PostgreSQLContainer

plugins {
    id("java")
    id("idea")
    id("com.vaadin") version "24.6.5"
    id("org.sonarqube") version "6.0.1.5171"
    id("io.freefair.lombok") version "8.12.2"
    id("org.flywaydb.flyway") version "11.3.3"
    id("org.springframework.boot") version "3.4.3"
    id("org.jooq.jooq-codegen-gradle") version "3.19.18"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ch.gatzka"

val databaseMigrationUrl: String? = findProperty("database.migration.url") as String?
val databaseMigrationUser: String? = findProperty("database.migration.user") as String?
val databaseMigrationPassword: String? = findProperty("database.migration.password") as String?

tasks {
    withType<JavaCompile> {
        options.release.set(23)
        options.encoding = "UTF-8"
    }
    withType<Test> {
        useJUnitPlatform()
    }
    compileJava {
        dependsOn(jooqCodegen)
    }
    jooqCodegen {
        dependsOn("migrateDEV")
        finalizedBy("stopContainer")
        inputs.files(fileTree("src/main/resources/db/migration"))
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from("src/main/resources") {
            include("application-development.properties")

            project.properties.forEach { (key, value) ->
                if (key != null) {
                    filter<ReplaceTokens>("tokens" to mapOf(key to value.toString()))
                    filter<ReplaceTokens>("tokens" to mapOf("project.$key" to value.toString()))
                }
            }
        }
    }
    register("printVersion") {
        doLast {
            println(project.version)
        }
    }
    register("startContainer") {
        doLast {
            if (databaseContainer.isRunning) {
                databaseContainer.stop()
            }
            databaseContainer.start()
            project.extensions.extraProperties["databaseUrl"] = databaseContainer.jdbcUrl
            project.extensions.extraProperties["databaseUsername"] = databaseContainer.username
            project.extensions.extraProperties["databasePassword"] = databaseContainer.password
        }
    }
    register("stopContainer") {
        doLast {
            databaseContainer.stop()
        }
    }
    register("buildDockerImage") {
        dependsOn(bootJar)
        doLast {
            exec {
                commandLine("docker", "build", "-t", "tarkov-keytool:${project.version}", ".")
            }
        }
    }
    register("tagDockerImage") {
        dependsOn("buildDockerImage")
        doLast {
            exec {
                commandLine("docker", "tag", "tarkov-keytool:${project.version}", "ghcr.io/philipp-gatzka/tarkov-keytool:${project.version}")
            }
        }
    }
    register("pushDockerImage") {
        dependsOn("tagDockerImage")
        doLast {
            exec {
                commandLine("docker", "push", "ghcr.io/philipp-gatzka/tarkov-keytool:${project.version}")
            }
        }
    }
    register<FlywayMigrateTask>("migrateDEV") {
        doFirst {
            url = project.extensions.extraProperties["databaseUrl"].toString()
            user = project.extensions.extraProperties["databaseUsername"].toString()
            password = project.extensions.extraProperties["databasePassword"].toString()
            schemas = arrayOf("public")
        }
        dependsOn("startContainer")
        finalizedBy("stopContainer")
    }
    register<FlywayMigrateTask>("migrate") {
        doFirst {
            checkNotNull(databaseMigrationUrl) { "gradle property 'database.migration.url' is not set" }
            checkNotNull(databaseMigrationUser) { "gradle property 'database.migration.user' is not set" }
            checkNotNull(databaseMigrationPassword) { "gradle property 'database.migration.password' is not set" }
        }
        url = databaseMigrationUrl
        user = databaseMigrationUser
        password = databaseMigrationPassword
        schemas = arrayOf("public")
    }
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://127.0.0.1:60356/postgres"
            user = "postgres"
            password = "postgres"
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                excludes = "flyway_schema_history"
                inputSchema = "public"
                isIncludeSequences = true
                isIncludeSystemSequences = true
            }
            target {
                packageName = project.group.toString()
            }
            generate {
                isFluentSetters = true
            }
            strategy {
                matchers {
                    tables {
                        table {
                            tableClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Table"
                            }
                            recordClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Record"
                            }
                        }
                    }
                }
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "ch.gatzka:tarkov-keytool")
        property("sonar.organization", "philipp-gatzka-org")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

configurations {
    named("runtimeClasspath") {
        extendsFrom(configurations.getByName("developmentOnly"))
    }
}

dependencies {
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.parttio:line-awesome:2.1.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.postgresql:postgresql:42.7.5")
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:24.6.5")
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.3")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
    sourceSets["main"].java {
        srcDir("build/generated-sources/jooq")
    }
}

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.5")
        classpath("org.testcontainers:postgresql:1.20.5")
        classpath("org.flywaydb:flyway-database-postgresql:11.3.3")
    }
}

val databaseContainer = PostgreSQLContainer("postgres:15").apply {
    withDatabaseName("postgres")
    withUsername("postgres")
    withPassword("postgres")
    setPortBindings(listOf("60356:5432"))
}