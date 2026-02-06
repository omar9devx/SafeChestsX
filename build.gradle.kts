plugins {
    java
    kotlin("jvm") version "1.9.24"
}

group = "com.safechestsx"
version = "4.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    if (file("libs").exists()) {
        flatDir { dirs("libs") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    val vaultJar = file("libs/Vault.jar")
    if (vaultJar.exists()) {
        compileOnly(files(vaultJar))
    } else {
        compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    }
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val kotlinStdlib = configurations.runtimeClasspath.get()
        .filter { it.name.startsWith("kotlin-stdlib") }
    from(kotlinStdlib.map { if (it.isDirectory) it else zipTree(it) })
}
