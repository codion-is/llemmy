plugins {
    id("java-library")
    id("org.gradlex.extra-java-module-info") version "1.12"
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

dependencies {
    // The Codion framework UI module, transitively pulls in all required
    // modules, such as the model layer and the core database module
    api(libs.codion.swing.framework.ui)
    // Provides the local JDBC connection implementation
    implementation(libs.codion.framework.db.local)
    // Include all the standard Flat Look and Feels and a bunch of IntelliJ
    // theme based ones, available via the View -> Select Look & Feel menu
    implementation(libs.codion.plugin.flatlaf)
    implementation(libs.codion.plugin.flatlaf.intellij.themes)
    implementation(libs.flatlaf.extras);
    implementation(libs.flatlaf.fonts.inter)

    implementation(libs.langchain4j.core)

    // Provides the Logback logging library as a transitive dependency
    runtimeOnly(libs.codion.plugin.logback.proxy)
    // The H2 database implementation
    runtimeOnly(libs.codion.dbms.h2)
    // And the H2 database driver
    runtimeOnly(libs.h2)

    // The domain model unit test module
    testImplementation(libs.codion.framework.domain.test)
}

apply(from = "../langchain4j-module-info.gradle")

// The application version simply follows the Codion framework version used
version = libs.versions.codion.get().replace("-SNAPSHOT", "")

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    // System properties required for running the unit tests
                    testTask.configure {
                        // The JDBC url
                        systemProperty("codion.db.url", "jdbc:h2:mem:h2db")
                        // The database initialization script
                        systemProperty("codion.db.initScripts", "classpath:create_schema.sql")
                        // The user to use when running the tests
                        systemProperty("codion.test.user", "sa")
                    }
                }
            }
        }
    }
}

// Configure the docs generation
tasks.asciidoctor {
    inputs.dir("src")
    baseDirFollowsSourceFile()
    attributes(
        mapOf(
            "codion-version" to project.version,
            "source-highlighter" to "prettify",
            "tabsize" to "2"
        )
    )
    asciidoctorj {
        setVersion("2.5.13")
    }
}

// Copies the documentation to the Codion github pages repository, nevermind
tasks.register<Sync>("copyToGitHubPages") {
    group = "documentation"
    from(tasks.asciidoctor)
    into("../../codion-pages/doc/" + project.version + "/tutorials/llemmy")
}

tasks.register<WriteProperties>("writeVersion") {
    group = "build"
    description = "Create a version.properties file containing the application version"
    destinationFile = file("${temporaryDir.absolutePath}/version.properties")
    property("version", libs.versions.codion.get().replace("-SNAPSHOT", ""))
}

// Include the version.properties file from above in the
// application resources, see usage in LlemmyAppModel
tasks.processResources {
    from(tasks.named("writeVersion"))
}