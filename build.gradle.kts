plugins {
    val kotlinVersion = "1.6.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.8.1"
    id("me.him188.kotlin-jvm-blocking-bridge") version "2.1.0-162.1"
}

group = "NoMathExpectation.NMEBoot"
version = "1.2"

val ktor_version: String by project

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    gradlePluginPortal()
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-resources:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}

tasks.compileJava {
    options.release.set(11)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "11"
}


