plugins {
    val kotlinVersion = "1.9.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
    //not using anymore due to lack of maintenance
    //id("me.him188.kotlin-jvm-blocking-bridge") version "3.0.0-180.1"
}

group = "NoMathExpectation.NMEBoot"
version = "1.2"

val ktor_version: String by project
val exposedVersion: String by project

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")
    mavenLocal()
    gradlePluginPortal()

    maven {
        url = uri("https://libraries.minecraft.net")
    }
}
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    //not using anymore due to lack of maintenance
    //implementation("me.him188:kotlin-jvm-blocking-bridge-runtime:3.0.0-180.1")

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
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    implementation("com.github.plexpt:chatgpt:1.1.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")

    implementation("com.seaboat:TextAnalyzer:0.2.0-SNAPSHOT")
    shadowLink("com.seaboat:TextAnalyzer")

    implementation("com.mojang:brigadier:1.0.18")

    implementation("love.forte.simbot:simbot-core:4.0.0-beta1")
    implementation("love.forte.simbot.component:simbot-component-mirai-core:3.2.0.0")
    implementation("love.forte.simbot.component:simbot-component-kook-core:4.0.0-dev3")

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


