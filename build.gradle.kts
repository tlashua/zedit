import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
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
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    
    // Add TOML dependencies
    implementation("com.akuleshov7:ktoml-core:0.5.0")
    implementation("com.akuleshov7:ktoml-file:0.5.0")
    
    // Existing logging dependencies
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

            // Add ProGuard configuration
            modules("java.sql")
            modules("java.naming")
            modules("jdk.unsupported")
            
            jvmArgs(
                "-Dfile.encoding=UTF-8",
                "-Djava.awt.headless=false"
            )
        }
    }
}

// Add ProGuard configuration
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
