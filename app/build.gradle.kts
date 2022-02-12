plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android")
    id ("kotlin-kapt")
    id("com.google.gms.google-services")
    jacoco
}

jacoco {
    toolVersion = "0.8.3"
}

// ./gradlew test connectedDevelopDebugAndroidTest jacocoTestReport
// task must be created, examples in Kotlin which call tasks.jacocoTestReport do not work
tasks.create("jacocoTestReport", JacocoReport::class.java) {
    group = "Reporting"
    description = "Generate Jacoco coverage reports."

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*")

    val developDebug = "developDebug"

    sourceDirectories.setFrom(files(listOf(
            "$projectDir/src/main/java",
            "$projectDir/src/main/kotlin"
    )))
    classDirectories.setFrom(files(listOf(
            fileTree("dir" to "$buildDir/intermediates/javac/$developDebug", "excludes" to fileFilter),
            fileTree("dir" to "$buildDir/tmp/kotlin-classes/$developDebug", "excludes" to fileFilter)
    )))

    // execution data from both unit and instrumentation tests
    executionData.setFrom(fileTree(
            "dir" to project.buildDir,
            "includes" to listOf(
                    // unit tests
                    "jacoco/test${"developDebug".capitalize()}UnitTest.exec",
                    // instrumentation tests
                    "outputs/code_coverage/${developDebug}AndroidTest/connected/**/*.ec"
            )
    ))

    // dependsOn("test${"developDebug".capitalize()}UnitTest")
    // dependsOn("connected${"developDebug".capitalize()}AndroidTest")
}

tasks.withType(Test::class.java) {
    (this.extensions.getByName("jacoco") as JacocoTaskExtension).apply {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val acraEmail = project.rootProject.file("local.properties")
    .let { if (it.exists()) it.readLines() else emptyList() }
    .firstOrNull { it.startsWith("acra.email") }
    ?.substringAfter("=")
    ?: System.getenv()["ACRA_EMAIL"]
    ?: ""

android {
    compileSdkVersion(31)
    defaultConfig {
        versionCode = 30708 // Version Code = 지속적으로 increment 해야 Google Play Console 에서 받아줌.
        versionName = "0.01.01A" // User 에게 보여지는 Version Number.
        applicationId = "com.theglendales.alarm"
        minSdkVersion(23)
        targetSdkVersion(31)
        testApplicationId = "com.theglendales.alarm.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = true
            buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
            //applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isDebuggable=false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
        }
    }
    flavorDimensions("default")
    productFlavors {
        create("develop") {
            applicationId = "com.theglendales.alarm"
        }
        create("premium") {
            applicationId = "com.premium.alarm"
        }
    }

    lintOptions {
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    adbOptions {
        timeOutInMs = 20 * 60 * 1000  // 20 minutes
        installOptions("-d", "-t")
    }

    dexOptions {
        preDexLibraries = System.getenv("TRAVIS") != "true"
    }

    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    useLibrary("android.test.mock")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }
}



dependencies {
    // App dependencies
    implementation(kotlin("stdlib", version = project.extra["kotlin"] as String))
    implementation("ch.acra:acra-mail:5.5.0")
    implementation("com.melnykov:floatingactionbutton:1.2.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("org.koin:koin-core:2.1.5") //koin for Kotlin 1
//koin 관련 내가 추가 -> 현재는 viewModel 을 Koin 으로 자동 등록하지 않을 예정여서 필요 없음.
    //implementation("org.koin:koin-core-ext:2.1.5")// koin for kotlin 2
    //implementation("org.koin:koin-android:2.1.5") //koin for android. !!! 여기서 android or androidx 라고 써도 됨. 정확히는 모르겠으나 일단은 android 로 써서 사용했음.
    //implementation("org.koin:koin-android-viewmodel:2.1.5") // koin Android ViewModel Features // !!! 여기서 android or androidx 라고 써도 됨. 정확히는 모르겠으나 일단은 android 로 써서 사용했음.
    //implementation("org.koin:koin-android-scope:2.1.5")*/
    //implementation("org.koin:koin-android-architecture:2.1.5") // sharedViewModel 쓰기 위해서 이거..?
// <--

    implementation("androidx.fragment:fragment:1.4.0")
    implementation("androidx.preference:preference:1.1.1")
//jjong added
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    //VuMeter
    implementation ("io.gresse.hugo.vumeterlibrary:vumeterlibrary:1.0.17")
    //Glide
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    kapt ("android.arch.lifecycle:compiler:1.1.1")
    kapt ("com.github.bumptech.glide:compiler:4.12.0")
    //Firebase
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    //SlidingUpPanel
    implementation("com.sothree.slidinguppanel:library:3.4.0")
    //Lottie
    implementation("com.airbnb.android:lottie:3.7.0")
    //Coroutine
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0") // **이 버전 올릴 때 위애-core 도 같이 올려줄것! 안그러면 난리남!
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.1.1")
    //Flow 사용 위해 넣음 (22.1.5)

    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.4.0") //(Secondfrag.kt 에서 by viewmodels 로 쉽게 inject 하는것 이거 사용)
    //Flow 사용 위해 넣음 2 (repeatOnLifecycle() 사용위해)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    //ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer:2.14.0")
    //GSON (SharedPref 에 Object 저장용)
    implementation("com.google.code.gson:gson:2.8.8")
    // ImageView 에 넣을 GMAIl 스타일 Circle Text Builder https://github.com/amulyakhare/TextDrawable
    implementation("com.amulyakhare:com.amulyakhare.textdrawable:1.0.1")
    // Swipe Reveal Layout (ListView 에서 Swipe 할 수 있게 하는 3rd party)
    implementation("com.chauthai.swipereveallayout:swipe-reveal-layout:1.4.1")
    //Billing Client (IAP related!)
    implementation ("com.android.billingclient:billing-ktx:4.0.0")


}

dependencies {
    testImplementation("net.wuerl.kotlin:assertj-core-kotlin:0.1.1")
    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("io.mockk:mockk:1.10.0")

}

dependencies {


    implementation("androidx.legacy:legacy-support-v4:1.0.0")//    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    androidTestImplementation("com.squareup.assertj:assertj-android:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    // androidx.test.ext.junit.rules.ActivityScenarioRule
    // androidx.test.ext.junit.runners.AndroidJUnit4
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
}