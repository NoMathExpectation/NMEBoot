plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.14.0"
    id("me.him188.kotlin-jvm-blocking-bridge") version "2.1.0-170.1"
}

group = "NoMathExpectation.NMEBoot"
version = "1.2"

val ktor_version: String by project
val exposedVersion: String by project

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    gradlePluginPortal()
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-resources:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.40.0.0")

    implementation("com.github.plexpt:chatgpt:1.1.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")

    implementation(fileTree("locallib") { include("*.jar") })
}

tasks.compileJava {
    options.release.set(11)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "11"
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}


