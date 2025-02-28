plugins {
    id("java-library")
    alias(libs.plugins.lombok)
    alias(libs.plugins.apollo)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.web)
    implementation(libs.apollo)
}

tasks {
    compileJava {
        dependsOn(generateApolloSources)
    }
    named("generateApolloSources") {
        dependsOn("downloadTarkovApolloSchemaFromIntrospection")
    }
}

val apolloSchemaFile = layout.buildDirectory.file("schema.graphql")

apollo {
    service("tarkov") {
        packageName.set(group.toString())
        srcDir("src/main/resources")
        introspection {
            endpointUrl.set("https://api.tarkov.dev/graphql")
            schemaFile.set(apolloSchemaFile)
        }
        schemaFile.set(apolloSchemaFile)
    }
}