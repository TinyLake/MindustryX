package mindustryX

plugins {
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
}