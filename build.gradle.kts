import de.undercouch.gradle.tasks.download.Download
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.pack200.java.Pack200Adapter

plugins {
    idea
    java
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("de.undercouch.download") version "5.3.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("gg.essential.loom") version "1.9.29"
}

group = "dev.meyi.bazaarnotifier"
version = "1.7.5"
val mod_id = "bazaarnotifier"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    log4jConfigs.from(file("log4j2.xml"))
    forge {
        pack200Provider.set(Pack200Adapter())
    }
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

repositories {
    mavenCentral()
    maven("https://maven.essential.gg/releases")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.polyfrost.cc/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    modCompileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    shade("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
}

val resourcesFile = file("src/main/resources/resources.json")
val resourcesURL = "https://raw.githubusercontent.com/AngusSteak/BazaarNotifierShard/resources/resources.json"

tasks.register<Download>("downloadResources") {
    src(resourcesURL)
    dest(resourcesFile)
    overwrite(false)
}

tasks.register("destroyResources") {
    doLast {
        if (resourcesFile.exists()) {
            delete(resourcesFile)
            println("Destroyed existing resource file.")
        }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    inputs.property("version", project.version)
    inputs.property("mcversion", "1.8.9")
    from(sourceSets["main"].resources.srcDirs) {
        include("mcmod.info")
        expand("version" to project.version, "mcversion" to "1.8.9")
    }
    from(sourceSets["main"].resources.srcDirs) {
        exclude("mcmod.info")
    }

    dependsOn("downloadResources")
    finalizedBy("destroyResources")
    outputs.upToDateWhen { false }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<Jar> {
        manifest.attributes["FMLCorePluginContainsFMLMod"] = "true"
        manifest.attributes["ForceLoadAsMod"] = "true"
    }

    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dev")
        configurations = listOf(shade)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named("remapJar") {
        dependsOn("shadowJar")
    }

    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    "ModSide" to "CLIENT",
                    "ForceLoadAsMod" to true,
                    "TweakOrder" to "0",
                    "TweakClass" to "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
                )
            )
        }
        enabled = false
    }
}
