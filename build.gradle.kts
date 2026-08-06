// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.plugin) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":core:model"))
    kover(project(":core:application"))
    kover(project(":core:data"))
    kover(project(":core:preference"))
}

kover {
    currentProject {
        createVariant("coverage") {}
    }

    reports {
        filters {
            excludes {
                annotatedBy(
                    "dagger.internal.DaggerGenerated",
                    "javax.annotation.processing.Generated"
                )
                androidGeneratedClasses()
                classes(
                    "*ComposableSingletons*",
                    "*.database.AppDatabase_Impl*",
                    "*.database.dao.*_Impl*",
                    "*Hilt_*",
                    "*Dagger*",
                    "*_Factory*",
                    "*_MembersInjector*",
                    "*_GeneratedInjector*",
                    "*_HiltModule*",
                    "dagger.hilt.internal.aggregatedroot.codegen.*",
                    "hilt_aggregated_deps.*"
                )
            }
        }

        variant("coverage") {
            html {
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }
            log {
                header = "Aggregated JVM and Robolectric coverage"
            }
        }
    }
}

tasks.register("ci") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all checks, assembles the debug APK, and generates coverage reports."

    dependsOn(
        ":app:assembleDebug",
        ":app:ktlintCheck",
        ":app:lint",
        ":core:application:ktlintCheck",
        ":core:data:ktlintCheck",
        ":core:data:lint",
        ":core:model:ktlintCheck",
        ":core:preference:ktlintCheck",
        ":core:preference:lint",
        ":koverHtmlReportCoverage",
        ":koverXmlReportCoverage",
        ":koverLogCoverage"
    )
}
