import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.Date
import java.util.concurrent.TimeUnit

plugins {
    id("java")
    id("com.install4j.gradle") version "10.0.4"
    id("com.dorongold.task-tree") version "1.5"
    id("de.undercouch.download") version "4.1.1"
}

allprojects {
    apply(plugin = "eclipse")
    apply(plugin = "idea")

    extra.set("appName", "GaiaSky")
    extra.set("gdxVersion", "1.14.2")
    extra.set("gdxcontrollersVersion", "2.2.4")
    extra.set("lwjglVersion", "3.3.4")
    extra.set("jcommanderVersion", "2.+")
    extra.set("slf4jVersion", "2.0.+")
    extra.set("sparkjavaVersion", "2.9.+")
    extra.set("jafamaVersion", "2.3.+")
    extra.set("commonsioVersion", "2.+")
    extra.set("py4jVersion", "0.10.9.+")
    extra.set("oshiVersion", "6.9.+")
    extra.set("stilVersion", "4.3")
    extra.set("jsampVersion", "1.3.+")
    extra.set("jacksonVersion", "2.20.+")
    extra.set("ashleyVersion", "1.7.4")
    extra.set("jtarVersion", "2.+")
    extra.set("junitVersion", "4.+")
    extra.set("annotationsVersion", "26.+")
    // Vulnerability fixes
    extra.set("jettyVersion", "10.0.24")
    extra.set("jsonVersion", "20231013")
    extra.set("xmlrpcVersion", "3.0")

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Fetch git info method.
fun fetchGitInfo(cmd: List<String>): String? {
    return try {
        // Handle Windows shell requirement if necessary
        var finalCmd = cmd
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            finalCmd = listOf("cmd", "/c") + cmd
        }

        val pb = ProcessBuilder(finalCmd)
        pb.directory(rootDir)
        val proc = pb.start()

        // Use text.trim() but handle empty streams gracefully
        val output = proc.inputStream.bufferedReader().use { it.readText().trim() }
        val exited = proc.waitFor(5, TimeUnit.SECONDS)

        if (exited && proc.exitValue() == 0) output else null
    } catch (e: Exception) {
        // Only shows if the command itself can't start
        println("DEBUG: Native Process failed: ${e.message}")
        null
    }
}

val gitTag = fetchGitInfo(listOf("git", "describe", "--abbrev=0", "--tags", "HEAD")) ?: "unknown"
val gitRev = fetchGitInfo(listOf("git", "rev-parse", "--short", "HEAD")) ?: "???"

project(":core") {
    apply(plugin = "java-library")

    val currentOS = DefaultNativePlatform.getCurrentOperatingSystem()
    val currentArch = DefaultNativePlatform.getCurrentArchitecture()

    extra.set("tag", gitTag)
    extra.set("rev", gitRev)

    val systemString: String = if (currentOS.isWindows) {
        "${currentOS.displayName} ${currentArch.displayName}"
    } else {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("uname", "-snmr"))
            proc.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "unknown"
        }
    }
    extra.set("system", systemString)

    extra.set("builder", System.getProperty("user.name"))
    extra.set("buildtime", Date())
    version = gitTag

    println("")
    println("CURRENT SYSTEM")
    println("==============")
    println("java version: ${JavaVersion.current()}")
    println("system: ${extra.get("system")}")

    println("")
    println("GAIA SKY")
    println("========")
    println("git tag: ${extra.get("tag")}")
    println("git rev: ${extra.get("rev")}")
    println("buildtime: ${extra.get("buildtime")}")
    println("builder: ${extra.get("builder")}")
    println("")

    // Set some build variables
    val baseDir = System.getProperty("user.dir")
    val tag = "${extra.get("tag")}"
    val tagRev = "${extra.get("tag")}.${extra.get("rev")}"

    extra.set("baseDir", baseDir)
    extra.set("tag", tag)
    extra.set("tagRev", tagRev)
    extra.set("distName", "gaiasky-$tagRev")
    extra.set("releasesDir", "$baseDir/releases")
    extra.set("distDir", "${extra.get("releasesDir")}/${extra.get("distName")}")
    extra.set("packageName", "packages-$tagRev")
    extra.set("packageDir", "${extra.get("releasesDir")}/${extra.get("packageName")}")

    println("")
    println("BUILD VARIABLES AND INFO")
    println("========================")
    println("baseDir: ${extra.get("baseDir")}")
    println("tagRev: ${extra.get("tagRev")}")
    println("distName: ${extra.get("distName")}")
    println("distDir: ${extra.get("distDir")}")
    println("packageName: ${extra.get("packageName")}")
    println("packageDir: ${extra.get("packageDir")}")
    println("")

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    dependencies {
        // Helper to get extra properties
        fun property(name: String): String = project.extra.get(name) as String

        compileOnly("com.badlogicgames.gdx:gdx-tools:${property("gdxVersion")}")

        implementation("org.lwjgl:lwjgl-openxr:${property("lwjglVersion")}")
        implementation("org.lwjgl:lwjgl-glfw:${property("lwjglVersion")}")
        runtimeOnly("org.lwjgl:lwjgl-openxr:${property("lwjglVersion")}:natives-linux")
        runtimeOnly("org.lwjgl:lwjgl-openxr:${property("lwjglVersion")}:natives-windows")

        implementation("com.badlogicgames.gdx:gdx:${property("gdxVersion")}")
        implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${property("gdxVersion")}")
        runtimeOnly("com.badlogicgames.gdx:gdx-platform:${property("gdxVersion")}:natives-desktop")

        implementation("com.badlogicgames.gdx:gdx-freetype:${property("gdxVersion")}")
        runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:${property("gdxVersion")}:natives-desktop")

        implementation("com.badlogicgames.gdx-controllers:gdx-controllers-core:${property("gdxcontrollersVersion")}")
        implementation("com.badlogicgames.gdx-controllers:gdx-controllers-desktop:${property("gdxcontrollersVersion")}")

        compileOnly("org.jetbrains:annotations:${property("annotationsVersion")}")
        implementation("com.badlogicgames.ashley:ashley:${property("ashleyVersion")}")
        implementation("uk.ac.starlink:stil:${property("stilVersion")}")
        implementation("uk.ac.starlink:jsamp:${property("jsampVersion")}")
        implementation("commons-io:commons-io:${property("commonsioVersion")}")
        implementation("org.jcommander:jcommander:${property("jcommanderVersion")}")
        implementation("net.jafama:jafama:${property("jafamaVersion")}")
        implementation("org.kamranzafar:jtar:${property("jtarVersion")}")

        implementation("net.sf.py4j:py4j:${property("py4jVersion")}")
        implementation("com.github.oshi:oshi-core-java11:${property("oshiVersion")}")
        implementation("com.fasterxml.jackson.core:jackson-databind:${property("jacksonVersion")}")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${property("jacksonVersion")}")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${property("jacksonVersion")}")

        implementation("org.slf4j:slf4j-nop:${property("slf4jVersion")}")
        implementation("com.sparkjava:spark-core:${property("sparkjavaVersion")}")

        implementation("org.eclipse.jetty:jetty-xml:${property("jettyVersion")}")
        implementation("org.eclipse.jetty:jetty-http:${property("jettyVersion")}")
        implementation("org.eclipse.jetty:jetty-server:${property("jettyVersion")}")
        implementation("org.json:json:${property("jsonVersion")}")
        implementation("org.apache.xmlrpc:xmlrpc:${property("xmlrpcVersion")}")

        // Testing
        testImplementation("junit:junit:${property("junitVersion")}")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.+")

        // Assets
        implementation(files("../assets"))
    }

}
