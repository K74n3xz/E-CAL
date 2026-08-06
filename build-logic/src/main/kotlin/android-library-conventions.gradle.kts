plugins {
    id("com.android.library")
    id("org.jetbrains.kotlinx.kover")
}

android {
    compileSdk {
        version = release(37)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kover {
    currentProject {
        createVariant("coverage") {
            add("debug")
        }
    }
}
