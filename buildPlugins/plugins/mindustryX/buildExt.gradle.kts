package mindustryX

plugins {
    java
    id("mindustryX.loaderMod")
}

tasks{
    val writeMindustryX by registering {
        outputs.cacheIf { true }
        val outFile = rootDir.parentFile.resolve("assets/mod.hjson")
        outputs.file(outFile)
        val version = (project.properties["buildversion"] ?: "1.0-dev") as String
        val upstreamBuild = (project.properties["upstreamBuild"] ?: "custom") as String
        inputs.property("buildVersion", version)
        inputs.property("upstreamBuild", upstreamBuild)
        doLast {
            outFile.writeText("""
            displayName: MindustryX Loader
            name: MindustryX
            author: WayZer
            main: mindustryX.loader.Main
            version: "$version"
            minGameVersion: "$upstreamBuild"
            hidden: true
            dependencies: []
        """.trimIndent())
        }
    }
    processResources.configure { dependsOn(writeMindustryX) }

    val packetsFile = rootDir.parentFile.resolve("assets/packets.jsonl")
    //protocol version is the integer build (e.g. 160.1 -> 160)
    val protocolVersion = ((project.properties["upstreamBuild"] ?: "0") as String).substringBefore('.')

    val syncPackets by registering(JavaExec::class) {
        group = "mdtx"
        description = "Sync since/until in assets/packets.jsonl with the current packet registration order"
        dependsOn(tasks.named("classes"))
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("mindustryX.tools.PacketSync")
        args(packetsFile.absolutePath, protocolVersion)
    }
}