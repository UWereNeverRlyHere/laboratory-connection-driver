dependencies {
    implementation(project(":commons"))
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstructProcessor)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        // Відкриваємо java.text для reflection (MapStruct/FreeMarker)
        "--add-opens", "java.base/java.text=ALL-UNNAMED",
        "--add-opens", "java.base/sun.text=ALL-UNNAMED",

        // Опції MapStruct
        "-Amapstruct.suppressGeneratorTimestamp=true",
        "-Amapstruct.verbose=true"
    ))

    // Форсувати non-incremental
    options.isIncremental = false
}