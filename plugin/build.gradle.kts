import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// TODO: Update value to your plugin's name.
val pluginName = "LiveWallpaper"

// TODO: Update value to match your plugin's package name.
val pluginPackageName = "org.godotengine.plugin.android.LiveWallpaper"

android {
    namespace = pluginPackageName
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 21
        manifestPlaceholders["godotPluginName"] = pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginName}\"")
        setProperty("archivesBaseName", pluginName)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

}

dependencies {
    implementation("org.godotengine:godot:4.3.0.stable")
    // Include only headers/interfaces for compile
   // compileOnly(fileTree(mapOf("dir" to "C:/Development/Projects/open source/Godot Engine 4.3.beta3.dev/godot/platform/android/java/lib/src/", "include" to listOf("**/*.java", "**/*.kt"))))
    // Or specify jars/libraries that provide the necessary headers
    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// BUILD TASKS DEFINITION
val copyDebugAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated debug AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-debug.aar")
    into("addons/$pluginName/bin/debug")
}

val copyReleaseAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated release AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-release.aar")
    into("addons/$pluginName/bin/release")
}

val cleanDemoAddons by tasks.registering(Delete::class) {
    delete("addons/$pluginName")
}

val copyAddonsToDemo by tasks.registering(Copy::class) {
    description = "Copies the export scripts templates to the plugin's addons directory"

    dependsOn(cleanDemoAddons)
    finalizedBy(copyDebugAARToAddons)
    finalizedBy(copyReleaseAARToAddons)

    from("export_scripts_template")
    into("addons/$pluginName")
}

tasks.named("assemble").configure {
    finalizedBy(copyAddonsToDemo)
}

tasks.named<Delete>("clean").apply {
    dependsOn(cleanDemoAddons)
}
