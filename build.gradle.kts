import org.gradle.jvm.tasks.Jar

val ktorVersion = "1.2.2"
val kotlinVersion = "1.3.40"
val logbackVersion = "1.2.1"
val hikariVersion = "2.2.5"
val koinVersion = "2.0.1"
val conifg4KVersion = "0.4.1"
val exposedVersion = "0.16.1"
val postgresqlVersion = "42.2.6"
val rxkotlinVersion = "2.4.0-beta.1"
val rxjavaVersion = "2.2.10"
val retrofitVersion = "2.6.0"
val moshiVersion = "1.8.0"
val klaxonVersion = "5.0.1"
val gsonVersion = "2.8.5"
val rabbitmqVersion = "5.7.2"
val kotlinLogging = "1.6.26"

repositories {
    mavenLocal()
    jcenter()
    maven {
        url = uri("https://kotlin.bintray.com/ktor")
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.41"
}

group = "com.fuyongxing"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
//    mainClassName = "com.fuyongxing.networking.DnspodKt"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("ch.qos.logback:logback-classic:$logbackVersion")
    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-auth:$ktorVersion")
    compile("io.ktor:ktor-auth-jwt:$ktorVersion")
    compile("com.zaxxer:HikariCP:$hikariVersion")
    compile("org.koin:koin-ktor:$koinVersion")
    compile("org.koin:koin-core:$koinVersion")
    compile("io.github.config4k:config4k:$conifg4KVersion")
    compile("org.jetbrains.exposed:exposed:$exposedVersion")
    compile("org.postgresql:postgresql:$postgresqlVersion")
    compile("io.reactivex.rxjava2:rxkotlin:$rxkotlinVersion")
    compile("io.reactivex.rxjava2:rxjava:$rxjavaVersion")
    compile("com.squareup.retrofit2:retrofit:$retrofitVersion")
    compile("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    compile("com.google.code.gson:gson:$gsonVersion")
    compile("com.beust:klaxon:$klaxonVersion")
    compile("com.rabbitmq:amqp-client:$rabbitmqVersion")
    compile("io.github.microutils:kotlin-logging:$kotlinLogging")
    compile("com.squareup.okhttp3:logging-interceptor:3.9.0")

    testCompile("io.ktor:ktor-server-tests:$ktorVersion")
}
val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "com.fuyongxing.networking.DnspodKt"
    }
    from(configurations.compile
        .map { if (it.isDirectory) it else zipTree(it) })
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
