plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ywh.utils"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("ywh.utils.Main")
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
    // Enable automatic module path for all dependencies
    modularity.inferModulePath.set(false)
}

dependencies {
    // Documents4j для конвертації
    implementation("com.documents4j:documents4j-local:1.1.13")
    implementation("com.documents4j:documents4j-transformer-msoffice-word:1.1.13")


}

// Використовуємо shadowJar замість звичайного jar
tasks.shadowJar {
    archiveFileName.set("docx2pdf.jar")
    manifest {
        attributes["Main-Class"] = "ywh.utils.Main"
    }

    // Виключаємо конфліктні пакети або переміщуємо їх
    exclude("META-INF/services/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Переміщуємо конфліктні класи в shadow пакет
    relocate("org.slf4j", "shadow.org.slf4j")
    relocate("ch.qos.logback", "shadow.ch.qos.logback")
    relocate("com.google.errorprone", "shadow.com.google.errorprone")
}

// Вимикаємо звичайний jar task
tasks.jar {
    enabled = false
}

// Копіювання shadowJar файлу
tasks.register<Copy>("copyJarToServicesTools") {
    description = "Копіює Shadow JAR файл в папку services/tools"
    group = "build"

    dependsOn("shadowJar")

    from(layout.buildDirectory.file("libs/docx2pdf.jar"))
    into("..\\services\\tools")

    doFirst {
        file("..\\services\\tools").mkdirs()
    }

    doLast {
        println("Shadow JAR файл скопійований в: ..\\services\\tools\\docx2pdf.jar")
    }
}

tasks.register<Copy>("copyJarToTools") {
    description = "Копіює Shadow JAR файл в папку docx2pdf-converter/tools"
    group = "build"

    dependsOn("shadowJar")

    from(layout.buildDirectory.file("libs/docx2pdf.jar"))
    into(project.projectDir.resolve("tools"))

    doFirst {
        project.projectDir.resolve("tools").mkdirs()
    }

    doLast {
        println("Shadow JAR файл скопійований в: ${project.projectDir}\\tools\\docx2pdf.jar")
    }
}

tasks.register("copyJarToBothLocations") {
    description = "Копіює Shadow JAR файл в обидві папки"
    group = "build"

    dependsOn("copyJarToServicesTools", "copyJarToTools")

    doLast {
        println("Shadow JAR файл скопійований в обидві локації!")
    }
}

tasks.build {
    finalizedBy("copyJarToBothLocations")
}

tasks.test {
    useJUnitPlatform()
}