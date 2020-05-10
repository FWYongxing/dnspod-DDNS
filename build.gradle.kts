val kotlinVersion = "1.3.41"
val logbackVersion = "1.2.1"
val conifg4KVersion = "0.4.1"
val rxkotlinVersion = "2.4.0-beta.1"
val rxjavaVersion = "2.2.10"
val retrofitVersion = "2.6.0"
val gsonVersion = "2.8.5"
val kotlinLogging = "1.6.26"

repositories {
    mavenLocal()
    jcenter()
}

plugins {
    application
    kotlin("jvm") version "1.3.72"
}

application {
    mainClassName = "com.fuyongxing.networking.dnspod.dnspodKt"
}

group = "com.fuyongxing"
version = "0.0.1"

application {
    mainClassName = "com.fuyongxing.networking.dnspod.DnspodKt"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.config4k:config4k:$conifg4KVersion")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLogging")
    implementation("com.squareup.okhttp3:logging-interceptor:3.9.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.41")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")