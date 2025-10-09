plugins {
    java
    alias(libs.plugins.module.plugin) apply false
    alias(libs.plugins.fx.plugin) apply false
    alias(libs.plugins.jlink.plugin) apply false

}

group = "ywh.labs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Define common dependency versions for all subprojects
subprojects {
    group = "ywh.labs"
    version = "1.0-SNAPSHOT"
    apply(plugin = "java")
    apply(plugin = "org.javamodularity.moduleplugin")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(rootProject.libs.versions.javaVersion.get()))
        modularity.inferModulePath.set(true)
    }


    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        implementation(rootProject.libs.gson){
            exclude(group = "org.apache.logging.log4j")
        }
        implementation(rootProject.libs.bundles.logging){
            exclude(group = "org.apache.logging.log4j")

        }
        implementation(rootProject.libs.jetbrainsAnnotations)

        // Тестування
        testCompileOnly(rootProject.libs.lombok)
        testAnnotationProcessor(rootProject.libs.lombok)

        testImplementation(platform(rootProject.libs.junit.bom))
        testImplementation(rootProject.libs.bundles.testing)

    }

    tasks.test {
        useJUnitPlatform()
    }



}


dependencies {
    testImplementation(platform(rootProject.libs.junit.bom))
    testImplementation(rootProject.libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()
}

allprojects {
    if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
        tasks.register("prepareKotlinBuildScriptModel")
    }
}
