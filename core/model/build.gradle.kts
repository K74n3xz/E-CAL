plugins {
    id("kotlin-common-conventions")
    id("kotlin-library-conventions")
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    testImplementation(libs.junit)
}
