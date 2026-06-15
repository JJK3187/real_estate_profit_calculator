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

// 의존성 jar를 별도 폴더로 복사
tasks.register<Copy>("copyDeps") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("installer-input/libs"))
}

// 앱 jar를 installer-input 폴더로 복사
tasks.register<Copy>("copyAppJar") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into(layout.buildDirectory.dir("installer-input"))
}

// jpackage로 .exe 설치 파일 생성 (JRE + JavaFX 모두 번들링)
tasks.register<Exec>("jpackage") {
    dependsOn("copyDeps", "copyAppJar")

    val jpackagePath = "C:\\Program Files\\Java\\jdk-17\\bin\\jpackage.exe"
    val jlinkPath = "C:\\Program Files\\Java\\jdk-17\\bin\\jlink.exe"
    val jdkModules = "C:\\Program Files\\Java\\jdk-17\\jmods"
    val outputDir = layout.buildDirectory.dir("installer").get().asFile.absolutePath
    val inputDir = layout.buildDirectory.dir("installer-input").get().asFile.absolutePath
    val runtimeDir = layout.buildDirectory.dir("custom-runtime").get().asFile.absolutePath
    val wixBinPath = "C:\\Program Files (x86)\\WiX Toolset v3.14\\bin"

    // Gradle 캐시에서 JavaFX win jar 경로 수집
    val javafxModulePath = configurations.runtimeClasspath.get()
        .filter { it.name.contains("javafx") && it.name.contains("win") }
        .joinToString(";") { it.absolutePath }

    environment("PATH", System.getenv("PATH") + ";$wixBinPath")

    commandLine(
        "cmd", "/c",
        "rmdir /s /q \"$outputDir\" 2>nul & rmdir /s /q \"$runtimeDir\" 2>nul & " +
        "\"$jlinkPath\" --no-header-files --no-man-pages --compress=2 " +
        "--module-path \"$jdkModules;$javafxModulePath\" " +
        "--add-modules java.base,java.desktop,java.logging,java.prefs,java.xml," +
        "javafx.controls,javafx.fxml,javafx.graphics,javafx.base " +
        "--output \"$runtimeDir\" && " +
        "\"$jpackagePath\" " +
        "--input \"$inputDir\" " +
        "--main-jar \"${project.name}-${project.version}.jar\" " +
        "--main-class com.calculator.app.Main " +
        "--runtime-image \"$runtimeDir\" " +
        "--name RealEstateProfitCalculator " +
        "--app-version 1.0.0 " +
        "--dest \"$outputDir\" " +
        "--type exe " +
        "--win-dir-chooser " +
        "--win-shortcut " +
        "--win-menu " +
        "--description \"Real Estate Profit Calculator\" " +
        "--vendor JJK"
    )

    doFirst {
        file(inputDir).mkdirs()
        println("jpackage 실행 중 (JRE + JavaFX 번들링)...")
        println("JavaFX 모듈 경로: $javafxModulePath")
    }
}
