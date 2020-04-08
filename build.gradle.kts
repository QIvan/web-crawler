plugins {
    java
    application
    id("io.freefair.lombok") version "4.1.6"
}

group = "ru.qivan"
version = "1.0.0"

application {
    mainClassName = "ru.qivan.WebCrawler"
}

tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}



java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
//    compileOnly "org.projectlombok:lombok:1.18.10"
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.6.1")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    implementation(group = "org.jsoup", name = "jsoup", version = "1.12.1")
    implementation(group = "junit", name = "junit", version = "4.12")
    implementation(group = "org.mockito", name = "mockito-core", version = "3.2.0")
    implementation(group = "org.hamcrest", name = "hamcrest", version = "2.2")
//    annotationProcessor "org.projectlombok:lombok:1.18.10"
}

