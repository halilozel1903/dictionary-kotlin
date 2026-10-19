import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.ktor.client.core)
    // Entries are read from the JSON tree, so missing or empty fields never fail a lookup; no serialization plugin.
    implementation(libs.kotlinx.serialization.json)
    // Default engine. Pass any other Ktor engine (OkHttp, Android, Java) through DictionaryConfig.engine.
    implementation(libs.ktor.client.cio)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.dictionary-kotlin".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "dictionary")
    pom {
        name.set("Dictionary Kotlin")
        description.set("Kotlin coroutine client for the Free Dictionary API: typed entries with phonetics and audio, meanings by part of speech, definitions, examples, synonyms and antonyms, a typed not-found result and caching.")
    }
}
