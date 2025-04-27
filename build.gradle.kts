plugins {
    id("java")
    // Just for managing the license headers
    id("com.diffplug.spotless") version "7.0.1"
    // For the asciidoctor docs
    id("org.asciidoctor.jvm.convert") version "4.0.4"
}

allprojects {
    version = "0.9.0"
}

java {
    toolchain {
        // Use the latest possible Java version
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

configure(allprojects) {
    apply(plugin = "com.diffplug.spotless")
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