import javax.inject.Inject
import java.util.Date
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("java")
    id("com.install4j.gradle")
    id("de.undercouch.download")
    id("eclipse")
}

// Remember to also modify GaiaSkyDesktop#MIN_JAVA_VERSION
val minJavaVersion = JavaVersion.VERSION_21
java {
    sourceCompatibility = minJavaVersion
    targetCompatibility = minJavaVersion
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Extension properties
val mainClassName by extra("gaiasky.desktop.GaiaSkyDesktop")
val workDir by extra(file("../"))
val assetsDir by extra(file("${workDir}/assets"))
val tmpDir by extra(file("/tmp/gaiasky-gradle/"))
tmpDir.mkdirs()

val jreArchive by extra("jre-archive.tar.gz")

val coreProject = project(":core")

val distDirStr = coreExtra("distDir") as String
val distLibPath = "$distDirStr/lib"

// Helper to access core project extras safely
fun coreExtra(name: String): Any? {
    // Try to get from root project's extra (most likely place for tagRev)
    if (rootProject.extra.has(name)) {
        return rootProject.extra.get(name)
    }

    // Try the current project (:core) extra
    if (project.extra.has(name)) {
        return project.extra.get(name)
    }

    // Try standard project properties (gradle.properties or -P)
    return project.findProperty(name)
}

tasks.register("genVersionFile") {
    val buildtime = coreExtra("buildtime")
    val rev = coreExtra("rev")
    val tag = coreExtra("tag")
    val builder = coreExtra("builder")
    val system = coreExtra("system")

    outputs.file(layout.buildDirectory.file("classes/java/main/version"))

    doLast {
        val versionFile = file("build/classes/java/main/version")
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            buildtime=$buildtime
            build=$rev
            version=$tag
            builder=$builder
            system=$system
            """.trimIndent(),
            Charsets.UTF_8
        )
    }
}

tasks.named("compileJava") {
    dependsOn("genVersionFile")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("src"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}


install4j {
    installDir = file("${System.getProperty("user.home")}/Programs/install4j11")
}

tasks.withType<Javadoc>().configureEach {
    destinationDir = layout.buildDirectory.dir("docs/javadoc").get().asFile
    val options = options as StandardJavadocDocletOptions
    options.docTitle = "Gaia Sky Javadoc"
    options.header = "<strong><a href='https://gaiasky.space'>Gaia Sky</a></strong> | <a href='http://docs.gaiasky.space'>docs</a> | <a href='https://codeberg.org/gaiasky/'>source</a> "
    options.bottom = "<a href='https://gaiasky.space'>Gaia Sky website</a> | <a href='http://docs.gaiasky.space'>Project documentation</a> | <a href='https://codeberg.org/gaiasky/'>Code repository</a>"
    options.windowTitle = "Gaia Sky Javadoc"
}

abstract class GaiaSkyRun @Inject constructor() : JavaExec() {
    init {
        val mainClassStr = project.extra["mainClassName"] as String
        mainClass.set(mainClassStr)
    }

    fun setup() {
        val assets = project.extra["assetsDir"] as File
        val work = project.extra["workDir"] as File

        systemProperty("properties.file", "")
        systemProperty("assets.location", "./assets/")
        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
        systemProperty("org.slf4j.simpleLogger.showThreadName", "false")

        minHeapSize = "3g"
        maxHeapSize = "6g"

        val baseJvmArgs = mutableListOf("-XX:+UseZGC", "-XX:+UseCompactObjectHeaders")
        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
            baseJvmArgs.add("-XstartOnFirstThread")
        }
        jvmArgs = baseJvmArgs

        classpath = project.sourceSets.main.get().runtimeClasspath + project.files(assets)
        standardInput = System.`in`
        workingDir = work
    }
}

tasks.register<GaiaSkyRun>("run") {
    dependsOn("jar")
    description = "Runs Gaia Sky."
    setup()
}

// Configurator for I18n tasks
val i18nConfig: JavaExec.() -> Unit = {
    dependsOn("jar")
    val assets = project.extra["assetsDir"] as File
    val work = project.extra["workDir"] as File

    systemProperty("assets.location", "./assets/")
    classpath = project.sourceSets.main.get().runtimeClasspath + project.files(assets)
    standardInput = System.`in`
    workingDir = work
}

tasks.register<JavaExec>("translationStatus") {
    i18nConfig()
    description = "Runs the I18n status program..."
    mainClass.set("gaiasky.desktop.util.I18nStatus")
}
tasks.register<JavaExec>("i18nStatus") {
    i18nConfig()
    mainClass.set("gaiasky.desktop.util.I18nStatus")
}

tasks.register<JavaExec>("i18nFormatter") {
    i18nConfig()
    description = "Runs the I18n formatter program..."
    mainClass.set("gaiasky.desktop.util.I18nFormatter")
}
tasks.register<JavaExec>("runI18nFormatter") {
    i18nConfig()
    mainClass.set("gaiasky.desktop.util.I18nFormatter")
}

tasks.register("createDistDir") {
    description = "Creates the dist/ directory."
    doLast {
        val distDir = file(coreExtra("distDir") as String)
        delete(distDir)
        distDir.mkdirs()
    }
}

tasks.register("copyToLib") {
    dependsOn("createDistDir")
    doLast {
        val distLib = file(distLibPath)
        distLib.mkdirs()

        extra.set("distLib", distLib.path)

        val version = coreProject.version
        copy {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(configurations.runtimeClasspath)
            into(distLib)
            exclude("**/data", "**/assets-bak", "**/metainfo", "**/core-$version.jar", "dummyversion")
        }

        listOf("archetypes", "conf", "data", "font", "fonts", "i18n", "icon", "img",
            "music", "mappings", "bookmarks", "rest-static", "scripts", "shader",
            "shaders", "skins", "text", "cert").forEach {
            delete(file("${distLib.path}/$it"))
        }
    }
}

tasks.named<Jar>("jar") {
    dependsOn("genVersionFile")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Explicitly pull in the compiled classes (solves the missing classes issue)
    from(sourceSets.main.get().output)

    // Define the Manifest here instead of the root file
    manifest {
        attributes(
            "Implementation-Title" to "Gaia Sky",
            "Implementation-Version" to project.version.toString(),
            "Main-Class" to "gaiasky.desktop.GaiaSkyDesktop"
        )
    }

    // Define what assets to keep in the jar
    from(assetsDir) {

        include("fonts/**")
        include("fonts/**/*.md")
        exclude("fonts/chars")
        exclude("fonts/src")

        include("icon/gs_icon_256.png")
        include("icon/gs_round_256.png")
        include("icon/gsvr_round_256.png")
        include("icon/gsascii.txt")

        include("img/**")
        exclude("**/*.hiero")
        exclude("**/*.svg")
        exclude("**/*.xcf")

        include("text/**")
        include("shader/**")
        include("archetypes/**")
        include("data/**")

        include("skins/**")

        exclude("skins/raw")

        exclude("assets-bak/**")

    }
}


tasks.register("gaiaskyJar") {
    dependsOn("genVersionFile", "jar", "copyToLib")
    doLast {
        val version = coreProject.version
        val baseDir = coreProject.rootDir
        val distLib = distLibPath
        copy {
            from("$baseDir/core/build/libs/core-$version.jar")
            into(distLib)
            rename { "gaiasky-core.jar" }
        }
    }
}

tasks.register<Exec>("genReleaseNotes") {
    val tag = coreExtra("tag")
    val distDir = coreExtra("distDir")
    val baseDir = coreProject.rootDir
    workingDir("$baseDir/core/scripts/release/")
    commandLine("/bin/bash", "generate-releasenotes", "$tag", "$distDir/releasenotes.txt")
}

tasks.register("javaVersionCheck") {
    doLast {
        val vsc = System.getenv("GS_JAVA_VERSION_CHECK") ?: ""
        val javaVersionOk = JavaVersion.current().isCompatibleWith(minJavaVersion)

        if (vsc.equals("false", ignoreCase = true)) {
            if (!javaVersionOk) println("WARNING: Using ${JavaVersion.current()}, need $minJavaVersion")
        } else if (!javaVersionOk) {
            throw GradleException("Gaia Sky must be built with Java $minJavaVersion or compatible, you are using ${JavaVersion.current()}")
        }
    }
}

// Copy Task helper
fun createCopyTask(name: String, src: String, dest: String) = tasks.register<Copy>(name) {
    from(src)
    into(dest)
}

createCopyTask("copyConfig", "${coreProject.rootDir}/assets/conf", "${coreExtra("distDir")}/conf")
createCopyTask("copyMetainfo", "${coreProject.rootDir}/assets/metainfo", "${coreExtra("distDir")}")
createCopyTask("copyI18n", "${coreProject.rootDir}/assets/i18n", "${coreExtra("distDir")}/i18n")
createCopyTask("copyMappings", "../assets/mappings", "${coreExtra("distDir")}/mappings")
createCopyTask("copyBookmarks", "../assets/bookmarks", "${coreExtra("distDir")}/bookmarks")
createCopyTask("copyRestStatic", "../assets/rest-static", "${coreExtra("distDir")}/rest-static")
tasks.register<Copy>("copyScripts") {
    from("${coreProject.rootDir}/assets/scripts")
    into("${coreExtra("distDir")}/scripts")
    exclude("**/.venv", "**/__pycache__")
}
tasks.register<Copy>("copyOptFlowCam") {
    from("${coreProject.rootDir}/core/scripts/optflowcam")
    into("${coreExtra("distDir")}/extra/optflowcam")
    exclude("**/.venv", "**/__pycache__")
}
tasks.register<Copy>("copyExecutables") {
    from("exe")
    into("${coreExtra("distDir")}")
    exclude("octreegen", ".idea")
}
tasks.register<Copy>("copyRootFiles") {
    from("${coreProject.rootDir}") {
        include("README.md", "LICENSE.md", "AUTHORS.md", "CONTRIBUTORS.md")
    }
    from("${coreProject.rootDir}/assets/icon") {
        include("gs_round_256.png", "gs_icon.ico", "gs_icon.svg", "gsvr_round_256.png", "gsvr_icon.ico")
    }
    into("${coreExtra("distDir")}")
}

tasks.register<Exec>("makeExecutable") {
    dependsOn("copyConfig", "copyMetainfo", "copyI18n", "copyMappings", "copyBookmarks",
        "copyRestStatic", "copyScripts", "copyOptFlowCam", "copyExecutables", "copyRootFiles")
    val distDir = coreExtra("distDir")
    commandLine("chmod", "ugo+x", "$distDir/gaiasky")
}

tasks.register<Exec>("generateManPage") {
    dependsOn("makeExecutable")
    val distDir = coreExtra("distDir")
    val baseDir = coreProject.rootDir
    commandLine("help2man", "--no-discard-stderr", "-N", "--section=6",
        "--include=$baseDir/core/man/gaiasky.h2m", "--output=$distDir/gaiasky.6", "$distDir/gaiasky")
}

tasks.register<Exec>("gzipManPage") {
    dependsOn("generateManPage")
    val distDir = coreExtra("distDir")
    commandLine("gzip", "-f", "$distDir/gaiasky.6")
}

tasks.register("dist") {
    dependsOn("gaiaskyJar", "javaVersionCheck")
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    if (!os.isWindows) {
        dependsOn("makeExecutable")
        if (os.isLinux) dependsOn("gzipManPage")
    } else {
        dependsOn("copyConfig", "copyMetainfo", "copyI18n", "copyMappings", "copyBookmarks",
            "copyRestStatic", "copyScripts", "copyOptFlowCam", "copyExecutables", "copyRootFiles")
    }
}

tasks.register("createTar") {
    dependsOn("dist")
    doLast {
        val pDir = coreExtra("packageDir") as String
        val dName = coreExtra("distName") as String
        val rDir = coreExtra("releasesDir") as String

        file(pDir).mkdirs()
        ant.withGroovyBuilder {
            "tar"("destfile" to "$pDir/$dName.tar.gz", "compression" to "gzip", "longfile" to "gnu") {
                "tarfileset"("dir" to rDir) {
                    "include"("name" to "$dName/**")
                    "exclude"("name" to "$dName/AppRun")
                    "exclude"("name" to "$dName/gaiasky-appimage.desktop")
                }
                "tarfileset"("dir" to rDir, "filemode" to "755") {
                    "include"("name" to "$dName/gaiasky")
                }
            }
            "checksum"("file" to file("$pDir/$dName.tar.gz"), "algorithm" to "md5", "todir" to pDir)
            "checksum"("file" to file("$pDir/$dName.tar.gz"), "algorithm" to "sha-256", "todir" to pDir)
        }
    }
}

tasks.register<Download>("downloadJRE") {
    description = "Downloads the JRE, needed to include in the package files."
    src("https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%2B10/OpenJDK25U-jdk_x64_linux_hotspot_25.0.2_10.tar.gz")

    // We use project.provider {} to make this "lazy"
    // This way, Gradle doesn't look for 'tmpDir' until the task is actually executed
    dest(project.provider {
        val tDir = coreExtra("tmpDir") as File
        val jArch = coreExtra("jreArchive") as String
        File(tDir, jArch)
    })

    onlyIfModified(true)
    overwrite(true)
}

tasks.register<Copy>("downloadAndExtractJRE") {
    dependsOn("downloadJRE")

    val tDirProvider = project.provider { coreExtra("tmpDir") as File }
    val jreArchProvider = project.provider { coreExtra("jreArchive") as String }

    doFirst {
        val tDir = tDirProvider.get()
        // Delete the directories (previous extractions) but keep the .tar.gz files.
        tDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                println("Cleaning old extraction: ${file.absolutePath}")
                file.deleteRecursively()
            }
        }
    }

    from(project.provider {
        val tDir = tDirProvider.get()
        val jreArch = jreArchProvider.get()
        tarTree(resources.gzip(File(tDir, jreArch)))
    })

    into(tDirProvider)

    doLast {
        val tDir = tDirProvider.get()
        val jreArch = jreArchProvider.get()
        val archive = File(tDir, jreArch)

        // Find the extracted folder (it's the only directory now)
        val extractedDir = tDir.listFiles()?.firstOrNull { it.isDirectory }
        project.extra.set("jreDir", extractedDir)

        // Optional: delete the archive after extraction to keep /tmp clean
        if (archive.exists()) {
            archive.delete()
        }

        println("Extracted JRE into: ${extractedDir?.name}")
    }
}

fun buildTime(): Date = Date()

fun getVersionRevision(tagRev: String): Pair<String, String> {
    var tagVersion = tagRev.substringBeforeLast(".")
    var tagRevision = "1"
    if (tagVersion.contains("-")) {
        val v = tagVersion
        tagVersion = v.substringBeforeLast("-")
        tagRevision = v.substringAfterLast("-")
    }
    return Pair(tagVersion, tagRevision)
}

tasks.register("createAppImage") {
    dependsOn("createTar", "downloadAndExtractJRE")

    // Inject the service needed for execution
    val execOps = project.serviceOf<ExecOperations>()

    doLast {
        val pDir = coreExtra("packageDir") as String
        val dDir = coreExtra("distDir") as String

        val tagRev = coreExtra("tagRev") as String

        val tDir = coreExtra("tmpDir") as File
        val jreDir = tDir.listFiles()?.firstOrNull { it.isDirectory && it.name.startsWith("jdk") }
            ?: throw GradleException("JRE directory not found in ${tDir.absolutePath}. Did downloadAndExtractJRE run?")

        val appimgDir = "$pDir/gaiasky.AppDir"

        delete(appimgDir)
        file(appimgDir).mkdirs()

        copy {
            from(dDir)
            into(appimgDir)
            exclude("**/*-natives-macos.jar", "**/*-natives-macos-arm64.jar", "**/*-natives-windows.jar", "**/*-natives-windows-x86.jar")
        }
        copy { from(jreDir) { into("usr") }; into(appimgDir) }

        file("$appimgDir/usr/share/metainfo/").mkdirs()
        copy { from("$appimgDir/gaiasky.appdata.xml"); into("$appimgDir/usr/share/metainfo/") }

        delete("$appimgDir/gaiasky", "$appimgDir/gaiasky.cmd", "$appimgDir/gs_icon.ico", "$appimgDir/gs_icon.svg", "$appimgDir/gaiasky.desktop", "$appimgDir/gaiasky.appdata.xml")
        file("$appimgDir/gaiasky-appimage.desktop").renameTo(file("$appimgDir/gaiasky.desktop"))

        val appImgName = "gaiasky_${tagRev}_x86_64.appimage"

        // Use the injected service instead of project.exec
        execOps.exec {
            commandLine("appimagetool", "-n", appimgDir, "$pDir/$appImgName")
            environment = mapOf("ARCH" to "x86_64")
        }

        ant.withGroovyBuilder {
            "checksum"("file" to file("$pDir/$appImgName"), "algorithm" to "md5", "todir" to pDir)
            "checksum"("file" to file("$pDir/$appImgName"), "algorithm" to "sha-256", "todir" to pDir)
        }
    }
}

tasks.register("createArch") {
    dependsOn("createTar", "genReleaseNotes")
    description = "Creates the Arch Linux AUR files for Gaia Sky."

    doLast {
        val packageDir = coreExtra("packageDir")?.toString() ?: ""
        val distDir = coreExtra("distDir")?.toString() ?: ""
        val distName = coreExtra("distName")?.toString() ?: ""
        val tagRev = coreExtra("tagRev")?.toString() ?: ""

        if (tagRev.isEmpty()) {
            throw GradleException("Property 'tagRev' is missing. Ensure it is defined in the root project.")
        }

        val archDir = file("$packageDir/arch/")
        archDir.mkdirs()

        copy {
            from("installerscripts/arch")
            into(archDir)
        }
        copy {
            from(distDir)
            into(archDir)
            include("releasenotes.txt")
        }

        val md5Tar = file("$packageDir/$distName.tar.gz.md5").readText().trim()
        val sha256Tar = file("$packageDir/$distName.tar.gz.sha-256").readText().trim()

        val (tagVersion, tagRevision) = getVersionRevision(tagRev)

        ant.withGroovyBuilder {
            "replace"("file" to "$packageDir/arch/PKGBUILD", "token" to "@version.revision@", "value" to tagRev)
            "replace"("file" to "$packageDir/arch/PKGBUILD", "token" to "@version@", "value" to tagVersion)
            "replace"("file" to "$packageDir/arch/PKGBUILD", "token" to "@revision@", "value" to tagRevision)
            "replace"("file" to "$packageDir/arch/PKGBUILD", "token" to "@md5checksum@", "value" to md5Tar)
            "replace"("file" to "$packageDir/arch/PKGBUILD", "token" to "@sha256checksum@", "value" to sha256Tar)
        }
    }
}

tasks.register("createDebian") {
    dependsOn("createTar", "genReleaseNotes")
    description = "Creates the Debian package for Gaia Sky."

    val execOps = project.serviceOf<org.gradle.process.ExecOperations>()

    doLast {
        val packageDir = coreExtra("packageDir") as String
        val distDir = coreExtra("distDir") as String
        val tagRev = coreExtra("tagRev") as String
        val debDir = file("$packageDir/debian")

        debDir.mkdirs()

        copy {
            from("installerscripts/debian")
            into(debDir)
        }

        val notesList = file("$distDir/releasenotes.txt").readLines()
        val notesStr = notesList
            .map { it.trim() }
            .filter { it.startsWith("-") }
            .joinToString("\n") { it.replaceFirst("- ", " * ") }

        val (tagVersion, tagRevision) = getVersionRevision(tagRev)

        ant.withGroovyBuilder {
            "replace"("file" to "$packageDir/debian/changelog", "token" to "@version@", "value" to "$tagVersion-$tagRevision")
            "replace"("file" to "$packageDir/debian/changelog", "token" to "@changelog@", "value" to notesStr)
            "replace"("file" to "$packageDir/debian/changelog", "token" to "@date@", "value" to buildTime().toString())
        }

        copy {
            from("$distDir/README.md", "$distDir/gaiasky.6.gz")
            into(debDir)
        }

        // Using project.exec for gunzip
        execOps.exec {
            commandLine("gunzip", "-f", "$packageDir/debian/gaiasky.6.gz")
        }
    }
}

tasks.register("prepareInstall4jScript") {
    dependsOn("createTar")

    doLast {
        val packageDir = coreExtra("packageDir") as String
        val distDir = coreExtra("distDir") as String
        val distName = coreExtra("distName") as String
        val baseDir = project.rootDir
        val tagRev = coreExtra("tagRev") as String
        val tag = coreExtra("tag") as String

        copy {
            from("installerscripts/template.install4j")
            into(packageDir)
            rename { it.replace("template.install4j", "$distName.install4j") }
        }

        ant.withGroovyBuilder {
            "replace"("file" to "$packageDir/$distName.install4j", "token" to "@gs-release-folder@", "value" to distDir)
            "replace"("file" to "$packageDir/$distName.install4j", "token" to "@gs-git-folder@", "value" to baseDir.absolutePath)
            "replace"("file" to "$packageDir/$distName.install4j", "token" to "@version-tag@", "value" to tag)
        }
    }
}

// Ensure you have the Install4j plugin import at the top
tasks.register<com.install4j.gradle.Install4jTask>("install4jMedia") {
    dependsOn("prepareInstall4jScript")

    val packageDir = coreExtra("packageDir") as String
    val distName = coreExtra("distName") as String

    if (project.hasProperty("winKeystorePassword")) {
        winKeystorePassword = project.property("winKeystorePassword") as String
    } else if (gradle.startParameter.taskNames.any { it.contains("pack") }) {
        throw GradleException("Please specify the keystore password for signing Windows packages:\ngradlew core:pack -PwinKeystorePassword=yourpass")
    }

    projectFile = file("$packageDir/$distName.install4j")
    // Map project variables to Install4j
    variables = mapOf(
        "majorVersion" to (project.findProperty("tag") ?: ""),
        "build" to (project.findProperty("rev") ?: "")
    )
}

tasks.register<Exec>("genGPGSignatures") {
    dependsOn("install4jMedia")
    val packageDir = coreExtra("packageDir") as String
    workingDir("${project.rootDir}/core/scripts/release/")
    commandLine("/bin/bash", "sign-packages.sh", packageDir)
}

tasks.register("pack") {
    dependsOn("genGPGSignatures", "createAppImage", "createArch", "createDebian")

    doLast {
        val packageDir = coreExtra("packageDir") as String
        val distName = coreExtra("distName") as String
        val tagRev = coreExtra("tagRev") as String

        fun updateSumFile(fileName: String, suffix: String) {
            val sumFile = file("$packageDir/$fileName")
            if (sumFile.exists()) {
                val tarSum = file("$packageDir/$distName.tar.gz.$suffix").readText().trim()
                val appSum = file("$packageDir/gaiasky_${tagRev}_x86_64.appimage.$suffix").readText().trim()

                sumFile.appendText("$tarSum *$distName.tar.gz\n")
                sumFile.appendText("$appSum *gaiasky_${tagRev}_x86_64.appimage\n")

                file("$packageDir/$distName.tar.gz.$suffix").delete()
                file("$packageDir/gaiasky_${tagRev}_x86_64.appimage.$suffix").delete()
            }
        }

        updateSumFile("md5sums", "md5")
        updateSumFile("sha256sums", "sha-256")
    }
}

tasks.named<Test>("test") {
    useJUnit()
    maxHeapSize = "1G"
    testLogging {
        events("passed", "skipped", "failed", "standard_out", "standard_error")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

eclipse {
    project {
        val appName = coreProject.extra["appName"] as String
        name = appName.lowercase() + "-core"
    }
}
