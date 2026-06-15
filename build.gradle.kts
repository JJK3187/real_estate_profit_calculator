plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

application {
    mainClass.set("com.calculator.app.Main")
}

group = "com"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

// fat jar: 모든 의존성을 포함한 단일 jar 생성
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.calculator.app.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// jpackage로 .exe 설치 파일 생성
tasks.register<Exec>("jpackage") {
    dependsOn(tasks.jar)
    val jpackagePath = "C:\\Program Files\\Java\\jdk-17\\bin\\jpackage.exe"
    val outputDir = layout.buildDirectory.dir("installer").get().asFile.absolutePath
    val wixBinPath = "C:\\Program Files (x86)\\WiX Toolset v3.14\\bin"

    environment("PATH", System.getenv("PATH") + ";$wixBinPath")

    commandLine(
        jpackagePath,
        "--input", layout.buildDirectory.dir("libs").get().asFile.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", "com.calculator.app.Main",
        "--name", "RealEstateProfitCalculator",
        "--app-version", "1.0.0",
        "--dest", outputDir,
        "--type", "exe",
        "--win-dir-chooser",
        "--win-shortcut",
        "--win-menu",
        "--description", "Real Estate Profit Calculator",
        "--vendor", "JJK"
    )

    doFirst {
        file(outputDir).mkdirs()
        println("jpackage 실행 중...")
    }
}
