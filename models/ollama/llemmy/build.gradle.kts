import org.gradle.internal.os.OperatingSystem

plugins {
    // The Badass Jlink Plugin provides jlink and jpackage
    // functionality and applies the java application plugin
    // https://badass-jlink-plugin.beryx.org
    id("org.beryx.jlink") version "3.1.3"
    id("org.gradlex.extra-java-module-info") version "1.14"
}

dependencies {
    implementation(project(":llemmy"))
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.http.client)
    implementation(libs.langchain4j.http.client.jdk)
    implementation(libs.langchain4j.ollama)
}

apply(from = "../../../langchain4j-module-info.gradle")

// Configure the application plugin, the jlink plugin relies
// on this configuration when building the runtime image
application {
    mainModule = "is.codion.demo.llemmy.ollama"
    mainClass = "is.codion.demo.llemmy.ollama.Runner"
    applicationDefaultJvmArgs = listOf(
        // This app doesn't require a lot of memory
        "-Xmx64m",
        // Just in case we're debugging in Linux, nevermind
        "-Dsun.awt.disablegrab=true"
    )
}

// Configure the Jlink plugin
jlink {
    // Specify the jlink image name
    imageName = "${project.name}-${project.version}"
    // The options for the jlink task
    options = listOf(
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        // Add the modular runtimeOnly dependencies, which are handled by the ServiceLoader.
        // These don't have an associated 'requires' clause in module-info.java
        // and are therefore not added automatically by the jlink plugin.
        "--add-modules",
        // The local JDBC connection implementation
        "is.codion.framework.db.local," +
                // The H2 database implementation
                "is.codion.dbms.h2," +
                // The Logback plugin
                "is.codion.plugin.logback.proxy"
    )

    mergedModule {
        requires("org.slf4j")
        requires("java.net.http")
        requires("java.naming")
        requires("java.sql")
        requires("com.fasterxml.jackson.databind")
        uses("dev.langchain4j.model.ollama.spi.OllamaChatModelBuilderFactory")
        uses("dev.langchain4j.http.client.HttpClientBuilderFactory")
        uses("dev.langchain4j.spi.data.message.ChatMessageJsonCodecFactory")
        provides("dev.langchain4j.http.client.HttpClientBuilderFactory")
            .with("dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory")
        provides("java.sql.Driver").with("org.h2.Driver")
    }

    jpackage {
        if (OperatingSystem.current().isLinux) {
            icon = "../../../llemmy/src/main/icons/llemmy.png"
            installerOptions = listOf(
                "--linux-shortcut"
            )
        }
        if (OperatingSystem.current().isWindows) {
            icon = "../../../llemmy/src/main/icons/llemmy.ico"
            installerOptions = listOf(
                "--win-menu",
                "--win-shortcut"
            )
        }
    }
}