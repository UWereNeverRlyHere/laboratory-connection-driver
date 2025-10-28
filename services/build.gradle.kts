dependencies {
    implementation(project(":commons"))
    implementation(project(":repository"))

    // Using versions from libs.versions.toml
    implementation(rootProject.libs.javaDbf)
    //WORD TO PDF
    implementation(rootProject.libs.poiOoxml)
    implementation(rootProject.libs.pdfbox)
    implementation(rootProject.libs.jSerialComm)


    implementation(rootProject.libs.jnaPlatform)
    implementation(rootProject.libs.apacheCommonsNet)
    implementation(rootProject.libs.jfreeChart)
}

tasks.register<Copy>("copyTemplates") {
    description = "Копирует шаблоны Word документов в выходную директорию с JAR файлом"
    group = "build"
    from("templates")
    into(layout.buildDirectory.dir("libs/templates"))
}


// Додаємо копіювання утиліт у ресурси
tasks.register<Copy>("copyTools") {
    description = "Копіює утилітні JAR файли у ресурси"
    group = "build"
    from("tools") {
        include("*.jar")
    }
    into(layout.buildDirectory.dir("resources/main/tools"))
}

// Додаємо до processResources, щоб включити в JAR
tasks.processResources {
    dependsOn("copyTools")
    from("tools") {
        include("*.jar")
        into("tools")
    }
}

tasks.jar {
    finalizedBy("copyTemplates")
}
