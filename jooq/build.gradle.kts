plugins {
    id("java-library")
    alias(libs.plugins.jooq)
    alias(libs.plugins.flyway)
    alias(libs.plugins.sonar)
}

group = "ch.gatzka"

tasks {
    register<DefaultTask>("startContainer") {
        group = "container"
        doLast {
            databaseContainer.start()
            project.extensions.extraProperties["databaseUrl"] = databaseContainer.jdbcUrl
            project.extensions.extraProperties["databaseUsername"] = databaseContainer.username
            project.extensions.extraProperties["databasePassword"] = databaseContainer.password
        }
        finalizedBy("stopContainer")
    }
    register<DefaultTask>("stopContainer") {
        group = "container"
        doLast {
            databaseContainer.stop()
        }
    }
    register<org.flywaydb.gradle.task.FlywayMigrateTask>("migrateContainer") {
        setGroup("container")
        doFirst {
            url = project.extensions.extraProperties["databaseUrl"].toString()
            user = project.extensions.extraProperties["databaseUsername"].toString()
            password = project.extensions.extraProperties["databasePassword"].toString()
            schemas = arrayOf("public")
        }
        dependsOn("startContainer")
        finalizedBy("stopContainer")
    }
    jooqCodegen {
        dependsOn("migrateContainer")
        finalizedBy("stopContainer")
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
                                transform = org.jooq.meta.jaxb.MatcherTransformType.PASCAL
                                expression = "$0_Table"
                            }
                            recordClass {
                                transform = org.jooq.meta.jaxb.MatcherTransformType.PASCAL
                                expression = "$0_Record"
                            }
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.jooq)
}

buildscript {
    dependencies {
        classpath(libs.postgres)
        classpath(libs.testcontainer.postgres)
        classpath(libs.flyway.postgres)
    }
}

val databaseContainer = org.testcontainers.containers.PostgreSQLContainer("postgres:latest").apply {
    withDatabaseName("postgres")
    withUsername("postgres")
    withPassword("postgres")
    setPortBindings(listOf("60356:5432"))
}
