import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.8.0-beta01"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    idea
}

group = "net.lashua.zonedit"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Add any additional compiler options here
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    implementation("org.jetbrains.compose.components:components-splitpane-desktop:1.5.1")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.8.0-beta01")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

compose.desktop {
    application {
        mainClass = "net.lashua.zonedit.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "zonedit"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "Zone Editor"
                upgradeUuid = "5d7b8d6a-c6d8-4e45-b3f3-b4b7b3d6c8d9"
                dirChooser = true
                perUserInstall = true
            }

            macOS {
                bundleID = "net.lashua.zonedit"
            }

            linux {
                menuGroup = "Development"
            }

            modules("java.sql", "java.naming", "jdk.unsupported")

            jvmArgs("-Dfile.encoding=UTF-8", "-Djava.awt.headless=false")
        }
    }
}
