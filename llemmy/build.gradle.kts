plugins {
    id("java-library")
    id("org.gradlex.extra-java-module-info") version "1.14"
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
    implementation(libs.codion.plugin.flatlaf.themes)
    implementation(libs.codion.plugin.flatlaf.intellij.themes)
    implementation(libs.flatlaf.fonts.inter)
    // FlatInspector
    implementation(libs.flatlaf.extras) {
        isTransitive = false //jsvg
    }

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
    dependsOn(rootProject.subprojects.map { it.tasks.build })
    rootProject.subprojects.forEach { subproject ->
        inputs.file(subproject.buildFile)
        inputs.files(subproject.sourceSets.main.get().allSource)
        inputs.files(subproject.sourceSets.test.get().allSource)
    }

    baseDirFollowsSourceFile()

    attributes(
        mapOf(
            "codion-version" to project.version,
            "source-highlighter" to "rouge",
            "tabsize" to "2"
        )
    )
    asciidoctorj {
        setVersion("2.5.13")
    }
}

tasks.register<WriteProperties>("writeVersion") {
    group = "build"
    description = "Create a version.properties file containing the application version"
    destinationFile = file("${temporaryDir.absolutePath}/version.properties")
    property("version", project.version)
}

// Include the version.properties file from above in the
// application resources, see usage in LlemmyAppModel
tasks.processResources {
    from(tasks.named("writeVersion"))
}