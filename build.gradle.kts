plugins {
	id("net.fabricmc.fabric-loom")
	id("org.jetbrains.kotlin.jvm") version "2.4.20"
	id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
}

val modId = providers.gradleProperty("mod_id").get()

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	// Loom adds the Minecraft / Fabric repositories automatically.
	// Add extra mod repositories here when depending on other mods.
}

loom {
	// Server-side only mod: compile against the dedicated server jar only.
	// This makes it a compile error to accidentally reference client-only classes.
	serverOnlyMinecraftJar()

	mods {
		register(modId) {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		named("server") {
			// Dev server lives in ./run (git-ignored). Accept the EULA in run/eula.txt once.
			runDirectory = layout.projectDirectory.dir("run")
			jvmArguments.add("-Xmx2G")
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API: events (tick, join/leave, commands, lifecycle) we need for tab list / bossbar systems.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	// Kotlin runtime + kotlinx (serialization, coroutines) are provided by Fabric Language Kotlin at runtime.
	implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// Adventure (the text API Paper uses): Component, MiniMessage, Audience. Bundled into our jar (jar-in-jar), so the
	// server doesn't need to install it. Its interface injection makes ServerPlayer / CommandSourceStack Audiences.
	include(implementation("net.kyori:adventure-platform-fabric:${providers.gradleProperty("adventure_platform_version").get()}")!!)
}

tasks.processResources {
	val props = mapOf("version" to version, "mod_id" to modId)
	inputs.properties(props)

	filesMatching("fabric.mod.json") {
		expand(props)
	}
}

kotlin {
	jvmToolchain(25)
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
	withSourcesJar()
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}
