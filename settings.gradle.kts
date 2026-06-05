import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "SpringYamlToEnvConverter"

pluginManagement {
    repositories {
        // Use Aliyun mirror for plugin management
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        gradlePluginPortal()
        mavenCentral()
    }
    
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        // Use Aliyun mirror for China (faster and more stable)
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        
        mavenCentral()
        
        // Add Google Maven as fallback
        google()
        
        // Add Gradle Plugin Portal explicitly
        gradlePluginPortal()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}
