// Top-level build file for FoodExpiryApp
// Food Expiry Date Reminder App

plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}