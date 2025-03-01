import java.io.ByteArrayOutputStream

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
    implementation(project(":api"))

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

fun gitBranch(): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --abbrev-ref HEAD".split(" ")
            standardOutput = byteOut
        }
        String(byteOut.toByteArray()).trim().also {
            if (it == "HEAD")
                logger.warn("Unable to determine current branch: Project is checked out with detached head!")
        }
    } catch (e: Exception) {
        logger.warn("Unable to determine current branch: ${e.message}")
        "Unknown Branch"
    }
}

fun getBranchVersion(): String 
    val branch = gitBranch()
    if (branch == "main") return project.version.toString()
    return project.version.toString() + "." + gitBranch().replace("/", "-")
}

tasks {
    register("buildDockerImage") {
        dependsOn(bootJar)
        doLast {
            exec {
                commandLine("docker", "build", "-t", "tarkov-keytool:${getBranchVersion()}", ".")
            }
        }
    }
    register("tagDockerImage") {
        dependsOn("buildDockerImage")
        doLast {
            exec {
                commandLine(
                    "docker",
                    "tag",
                    "tarkov-keytool:${getBranchVersion()}",
                    "ghcr.io/philipp-gatzka/tarkov-keytool:${getBranchVersion()}"
                )
            }
        }
    }
    register("tagDockerImageLatest") {
        doLast {
            exec {
                commandLine(
                    "docker",
                    "tag",
                    "tarkov-keytool:${getBranchVersion()}",
                    "ghcr.io/philipp-gatzka/tarkov-keytool:latest"
                )
            }
        }
    }
    register("pushDockerImage") {
        dependsOn("tagDockerImage")
        doLast {
            exec {
                commandLine("docker", "push", "ghcr.io/philipp-gatzka/tarkov-keytool:${getBranchVersion()}")
            }
        }
        if (gitBranch() == "main") {
            dependsOn("tagDockerImageLatest")
            doLast {
                exec {
                    commandLine("docker", "push", "ghcr.io/philipp-gatzka/tarkov-keytool:latest")
                }
            }
        }
    }
}