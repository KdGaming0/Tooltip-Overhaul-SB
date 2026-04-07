plugins {
    id("net.fabricmc.fabric-loom-remap")

    // `maven-publish`
    // id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

repositories {
    mavenCentral()
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://maven.azureaaron.net/releases")
            }
        }

        filter {
            includeGroup("net.azureaaron")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")
    include("maven.modrinth:midnightlib:${property("deps.midnightlib_version")}")

    modImplementation("net.azureaaron:hm-api:${property("deps.hm_api_version")}")
    include("net.azureaaron:hm-api:${property("deps.hm_api_version")}")

    modImplementation("maven.modrinth:ui-lib:${property("deps.uilib_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modRuntimeOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = rootProject.file("src/main/resources/${property("mod.id")}.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true")
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        inputs.property("fabric_api", project.property("deps.fabric_api"))
        inputs.property("fabric_loader", project.property("deps.fabric_loader"))

        inputs.property("midnightlib_version", project.property("deps.midnightlib_version"))
        inputs.property("modmenu_version", project.property("deps.modmenu_version"))
        inputs.property("hm_api_version", project.property("deps.hm_api_version"))
        inputs.property("uilib_version", project.property("deps.uilib_version"))


        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep"),

            "fabric_api" to project.property("deps.fabric_api"),
            "fabric_loader" to project.property("deps.fabric_loader"),

            "midnightlib_version" to project.property("deps.midnightlib_version"),
            "modmenu_version" to project.property("deps.modmenu_version"),
            "hm_api_version" to project.property("deps.hm_api_version"),
            "uilib_version" to project.property("deps.uilib_version")
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${JavaVersion.VERSION_21}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    jar {
        from("LICENSE") {
            rename { fileName -> "${fileName}_${project.property("mod.id")}" }
        }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

/*
// Publishes builds to Modrinth and Curseforge with changelog from the CHANGELOG.md file
publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null
        || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }
}
 */
/*
// Publishes builds to a maven repository under `com.kd_gaming1:template:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            // To authenticate, create `myMavenUsername` and `myMavenPassword` properties in your Gradle home properties.
            // See https://stonecutter.kikugie.dev/wiki/tips/properties#defining-properties
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
