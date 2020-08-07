buildscript {
    val kotlinVersion: String by extra("1.3.72")
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.29.0")

        classpath("com.google.gms:google-services:4.3.3")
        classpath("com.google.firebase:perf-plugin:1.3.1")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.2.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
