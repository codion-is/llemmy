plugins {
    id("java")
    // Just for managing the license headers
    id("com.diffplug.spotless") version "7.0.1"
    // For the asciidoctor docs
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

allprojects {
    version = "1.0.2"
}

java {
    toolchain {
        // Use the latest possible Java version
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

configure(allprojects) {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    dependencies {
        implementation(platform(rootProject.libs.codion.framework.bom))
        implementation(platform(rootProject.libs.langchain4j.bom))
    }

    spotless {
        // Just the license headers
        java {
            licenseHeaderFile("${rootDir}/license_header").yearSeparator(" - ")
        }
        format("javaMisc") {
            target("src/**/package-info.java", "src/**/module-info.java")
            licenseHeaderFile("${rootDir}/license_header", "\\/\\*\\*").yearSeparator(" - ")
        }
    }
}