buildscript {
    val kotlin = "1.4.0"

    allprojects {
        extra.apply {
            set("kotlin", kotlin)
        }
    }

    repositories {
        google()
        jcenter()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        classpath("com.google.gms:google-services:4.3.14")
        classpath ("com.google.firebase:firebase-crashlytics-gradle:2.9.5")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}