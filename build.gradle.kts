import de.undercouch.gradle.tasks.download.Download
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
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

tasks {
    val downloadTask by registering(Download::class) {
        src(resourcesURL)
        dest(resourcesFile)
        overwrite(true)
    }

    val retrieveResources by registering {
        dependsOn(downloadTask)
        doLast {
            println("Resources retrieved from $resourcesURL")
        }
    }

    val destroyResources by registering {
        doLast {
            if (resourcesFile.exists()) {
                delete(resourcesFile)
                println("Deleted: $resourcesFile")
            }
        }
    }

    processResources {
        dependsOn(retrieveResources)
        finalizedBy(destroyResources)

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

        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dev")
        configurations = listOf(shade)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named<RemapJarTask>("remapJar") {
        dependsOn(shadowJar)
        archiveClassifier.set("")
    }

    named<Jar>("jar") {
        enabled = false
    }

    named<Jar>("shadowJar") {
        manifest.attributes(
            "FMLCorePluginContainsFMLMod" to "true",
            "ForceLoadAsMod" to "true",
            "ModSide" to "CLIENT",
            "TweakOrder" to "0",
            "TweakClass" to "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
        )
    }
}
