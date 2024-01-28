import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.Bytecode
import javassist.bytecode.Descriptor
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

buildscript {
    dependencies {
        classpath("org.javassist:javassist:3.30.2-GA")
    }
}
plugins {
    java
    id("de.undercouch.download") version "5.6.0"
//    kotlin()
}

project.tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

sourceSets {
    main {
        java.srcDirs("src/", layout.buildDirectory.dir("/generated/sources/annotationProcessor/java/main"))
    }
}

val patchArc = task<Jar>("patchArc") {
    group = "build"
    destinationDirectory.set(temporaryDir)
    archiveBaseName.set("patched")

    val patchSrc by configurations.creating
    dependencies {
        configurations.api.configure {
            val arcLib = dependencies.find { it.name == "arc-core" }
                    ?: error("Can't find arc-core")
            dependencies.remove(arcLib)
            patchSrc(arcLib)
        }
    }
    inputs.files(patchSrc)
    dependencies.api(files(this))

    val transform = mutableMapOf<String, CtClass.() -> Unit>()
    transform["arc.util.Http\$HttpRequest"] = clz@{
        getDeclaredMethod("block").apply {
            val code = Bytecode(methodInfo.constPool)
            val desc = Descriptor.ofMethod(CtClass.voidType, arrayOf(this@clz))
            code.addAload(0)
            code.addInvokestatic("mindustryX.Hooks", "onHttp", desc)
            methodInfo.codeAttribute.iterator().insertEx(code.get())
            methodInfo.rebuildStackMapIf6(classPool, classFile)
        }
    }

    val genDir = layout.buildDirectory.dir("generated/patched")
    from(genDir)
    from(zipTree(patchSrc.files.single())){
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    doFirst {
        genDir.get().asFileTree.forEach { it.delete() }

        val pool = ClassPool()
        pool.appendSystemPath()
        patchSrc.files.forEach {
            pool.appendClassPath(it.path)
        }

        transform.forEach { (clz, block) ->
            pool.get(clz).also(block)
                    .writeFile(genDir.get().asFile.path)
        }
    }
}

val writeVersion = tasks.create("writeVersion") {
    val version = (project.properties["buildversion"] ?: "1.0-dev") as String
    val upstreamBuild = (project.properties["upstreamBuild"] ?: "custom") as String
    inputs.property("buildversion", version)
    inputs.property("upstreamBuild", upstreamBuild)
    val file = projectDir.resolve("assets/MindustryX.hjson")
    outputs.file(file)

    doLast {
        file.writeText("""
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

tasks.processResources {
    dependsOn(patchArc, writeVersion)
}

val downloadOriginJar = tasks.create<de.undercouch.gradle.tasks.download.Download>("downloadOriginJar") {
    val upstreamBuild = project.properties["upstreamBuild"] as String?
    val output = temporaryDir.resolve("v$upstreamBuild.jar")
    inputs.property("upstreamBuild", upstreamBuild)

    src("https://github.com/Anuken/Mindustry/releases/download/v$upstreamBuild/Mindustry.jar")
    dest(output)
    overwrite(false)
}
val genLoaderMod = tasks.create("genLoaderMod") {
    val distTask = tasks.getByPath("::desktop:dist")
    dependsOn(downloadOriginJar, distTask)
    val inputF = distTask.outputs.files.singleFile
    val baseF = downloadOriginJar.outputFiles.single()
    val outputF = layout.buildDirectory.file("libs/Mindustry.loader.jar")
    inputs.files(inputF, baseF)
    outputs.file(outputF)
    doLast {
        val input = ZipFile(inputF)
        val base = ZipFile(baseF)
        val output = ZipOutputStream(outputF.get().asFile.outputStream())
        val baseMap = base.entries().asSequence().associateBy { it.name }

        for (entry in input.entries()) {
            if (entry.name.startsWith("sprites") || entry.name == "version.properties") continue
            val baseEntry = baseMap[entry.name]
            if (baseEntry != null) {
                val a = input.getInputStream(entry).use { it.readAllBytes() }
                val b = base.getInputStream(baseEntry).use { it.readAllBytes() }
                val ext = entry.name.substringAfterLast('.', "")
                val eq = when (ext) {
                    "", "frag", "vert", "js", "properties" -> a.filter { it != 10.toByte() && it != 13.toByte() } == b.filter { it != 10.toByte() && it != 13.toByte() }
                    else -> a.contentEquals(b)
                }
                if (eq) continue
            }
            var outputEntry = entry
            //rename to mod.hjson
            if (entry.name == "MindustryX.hjson") {
                outputEntry = ZipEntry("mod.hjson")
            }
            output.putNextEntry(outputEntry)
            output.write(input.getInputStream(entry).use { it.readAllBytes() })
            output.closeEntry()
        }
        output.close()
    }
}