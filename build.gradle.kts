// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.hilt) apply false
    id("checkstyle")
}

val checkstyleVersion = libs.versions.checkstyleVersion.get()

subprojects {
    apply(plugin = "checkstyle")

    extensions.configure<CheckstyleExtension> {
        toolVersion = checkstyleVersion
        configFile = rootProject.file("config/checkstyle/google_checks.xml")
        isIgnoreFailures = false
        isShowViolations = true
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
