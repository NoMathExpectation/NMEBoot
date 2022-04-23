plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.8.1"
    id("me.him188.kotlin-jvm-blocking-bridge") version "2.0.0-160.3"
}

group = "NoMathExpectation.NMEBoot"
version = "1.1"

val ktor_version: String by project

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    gradlePluginPortal()
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-java:$ktor_version")
    implementation("io.ktor:ktor-client-resources:$ktor_version")
}

tasks.compileJava {
    options.release.set(11)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "11"
}


