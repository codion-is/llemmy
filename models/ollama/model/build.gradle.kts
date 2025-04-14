plugins {
    id("application")
}

dependencies {
    implementation(libs.codion.swing.common.ui)
    implementation(libs.codion.plugin.flatlaf)

    implementation("org.testcontainers:testcontainers:1.20.6")
}

application {
    mainClass = "is.codion.demo.llemmy.ollama.model.Runner"
    applicationDefaultJvmArgs = listOf("-Xmx64m")
}