pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {

}


rootProject.name = "Mini_Laboratory_Connection_Driver"
include("services")
include("commons")
include("repository")
include("connection_driver_application")
