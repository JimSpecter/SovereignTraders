buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "net.sovereign"
version = "1.0.4"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        java {
            exclude("net/bitbylogic/**")
        }
    }

    create("free") {
        kotlin.srcDir("src/free/kotlin")
        resources.srcDir("src/free/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
}

tasks.named<ProcessResources>("processFreeResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.tcoded.com/releases")
    maven("https://repo.nightexpressdev.com/releases")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://maven.citizensnpcs.co/repo")
}

dependencies {
    paperweight.foliaDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")
    implementation("com.github.BitByLogics:Bits-Menus:1.1.0")
    implementation("com.github.TechnicallyCoded:FoliaLib:0.4.4")
    implementation(kotlin("stdlib"))

    compileOnly("su.nightexpress.coinsengine:CoinsEngine:2.6.0")
    compileOnly("org.black_ixx:playerpoints:3.3.3")
    compileOnly("net.citizensnpcs:citizens-main:2.0.41-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun configureSharedShadow(task: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar) {
    task.relocate("kotlin", "net.sovereign.libs.kotlin")
    task.relocate("org.jetbrains", "net.sovereign.libs.jetbrains")
    task.relocate("org.intellij", "net.sovereign.libs.intellij")
    task.relocate("net.bitbylogic", "net.sovereign.libs.bitbylogic")
    task.relocate("com.tcoded.folialib", "net.sovereign.libs.folialib")
    task.mergeServiceFiles()
    task.configurations = listOf(project.configurations.runtimeClasspath.get())
    task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val shadowFreeJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    group = "build"
    description = "Build shaded JAR."
    dependsOn(tasks.named("jar"))
    archiveClassifier.set("free")

    configureSharedShadow(this)

    from("src/free/resources") {
        filesMatching("paper-plugin.yml") { expand("version" to project.version) }
    }
    from("src/main/resources")
    from(sourceSets.named("free").get().output)
    from(sourceSets.main.get().output)
}

val proguardFree by tasks.registering(proguard.gradle.ProGuardTask::class) {
    group = "build"
    description = "Obfuscates the shadow JAR with ProGuard."
    dependsOn(shadowFreeJar)

    val shadowJarFile = shadowFreeJar.get().archiveFile.get().asFile
    val tempOutFile  = layout.buildDirectory.dir("proguard").get().asFile
        .resolve("free-proguarded.jar")

    injars(shadowJarFile)
    outjars(tempOutFile)

    val javaHome = System.getProperty("java.home")
    val jmod = file("$javaHome/jmods/java.base.jmod")
    if (jmod.exists()) {
        libraryjars(
            mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class"),
            jmod
        )
    }

    configurations.compileClasspath.get().files.forEach { libraryjars(it) }

    configuration(file("proguard-free.pro"))

    doLast {
        tempOutFile.copyTo(shadowJarFile, overwrite = true)
        tempOutFile.delete()
        logger.lifecycle("ProGuard obfuscation complete: ${shadowJarFile.name}")
    }
}

tasks.register("buildFree") {
    group = "build"
    description = "Build & obfuscate the Free edition."
    dependsOn(proguardFree)
}

fun configureSharedRunServer(task: xyz.jpenilla.runpaper.task.RunServer, runDir: File) {
    task.minecraftVersion("1.21.8")
    task.runDirectory(runDir)

    task.downloadPlugins {
        url("https://hangarcdn.papermc.io/plugins/TNE/VaultUnlocked/versions/2.17.0/PAPER/VaultUnlocked-2.17.0.jar")
        url("https://github.com/EssentialsX/Essentials/releases/download/2.21.2/EssentialsX-2.21.2.jar")
        url("https://github.com/retrooper/packetevents/releases/download/v2.11.2/packetevents-spigot-2.11.2.jar")
    }

    val localJars = listOf(
        "dtlTradersPlus-6.4.37.jar",
        "Citizens.jar",
        "GemsEconomy.jar",
        "CoinsEngine.jar",
        "PlayerPoints.jar"
    )
    for (jarName in localJars) {
        val jar = rootProject.file(jarName)
        if (jar.exists()) task.pluginJars(jar)
    }

    task.jvmArgs("-Dcom.mojang.eula.agree=true")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        enabled = false
        archiveClassifier.set("free")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
    }

    build {
        dependsOn(proguardFree)
    }

    runServer { enabled = false }

    register<xyz.jpenilla.runpaper.task.RunServer>("runServerFree") {
        group = "run paper"
        description = "Run a Paper server with the plugin."
        configureSharedRunServer(this, file("run-free"))
        dependsOn(proguardFree)
        pluginJars(shadowFreeJar.flatMap { it.archiveFile })
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runDevBundleServer") {
        downloadPlugins {
            url("https://hangarcdn.papermc.io/plugins/TNE/VaultUnlocked/versions/2.17.0/PAPER/VaultUnlocked-2.17.0.jar")
            url("https://github.com/Euphillya/Essentials-Folia/releases/download/build-folia-patches-90/EssentialsX-2.22.0-dev%2B74-c1be1f6-Folia.jar")
            url("https://github.com/retrooper/packetevents/releases/download/v2.11.2/packetevents-spigot-2.11.2.jar")
        }

        val localJars = listOf(
            "dtlTradersPlus-6.4.37.jar",
            "Citizens.jar",
            "GemsEconomy.jar",
            "CoinsEngine.jar",
            "PlayerPoints.jar"
        )
        for (jarName in localJars) {
            val jar = rootProject.file(jarName)
            if (jar.exists()) pluginJars(jar)
        }

        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}
