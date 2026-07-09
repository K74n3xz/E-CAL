plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.android.library)
    implementation(libs.plugin.jetbrains.kotlin.jvm)
    implementation(libs.plugin.ktlint)
}
