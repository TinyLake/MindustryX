import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.rewriter.DexRewriter
import com.android.tools.smali.dexlib2.rewriter.Rewriter
import com.android.tools.smali.dexlib2.rewriter.RewriterModule
import com.android.tools.smali.dexlib2.rewriter.Rewriters
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
    repositories {
        google()
    }
    dependencies {
        classpath("org.javassist:javassist:3.30.2-GA")
        classpath("com.android.tools.smali:smali-dexlib2:3.0.5")
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
val distTask = tasks.getByPath("::desktop:dist")
val genLoaderMod = tasks.create("genLoaderMod") {
    val androidTask = tasks.findByPath("::android:compileReleaseJavaWithJavac")
    dependsOn(downloadOriginJar, distTask)
    if (androidTask != null)
        dependsOn(androidTask)
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
        if (androidTask != null) {
            val root = androidTask.outputs.files.first()
            root.resolve("mindustryX").walkTopDown().forEach {
                if (it.isDirectory) return@forEach
                val path = it.toRelativeString(root)
                output.putNextEntry(ZipEntry(path))
                output.write(it.readBytes())
                output.closeEntry()
            }
        }
        output.close()
    }
}

val genLoaderModDex = tasks.create("genLoaderModDex") {
    dependsOn(genLoaderMod, distTask)
    val library = distTask.outputs.files.singleFile
    val inFile = genLoaderMod.outputs.files.singleFile
    val outFile = temporaryDir.resolve("classes.dex")
//    val outFile = layout.buildDirectory.file("libs").get()
    inputs.file(inFile)
    outputs.file(outFile)
    doLast {
        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot == null || !File(sdkRoot).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        val d8Tool = File("$sdkRoot/build-tools/").listFiles()?.sortedDescending()
                ?.flatMap { dir -> (dir.listFiles().orEmpty()).filter { it.name.startsWith("d8") } }?.firstOrNull()
                ?: throw GradleException("No d8 found. Ensure that you have an Android platform installed.")
        val platformRoot = File("$sdkRoot/platforms/").listFiles()?.sortedDescending()?.firstOrNull { it.resolve("android.jar").exists() }
                ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        exec {
            commandLine("$d8Tool --lib ${platformRoot.resolve("android.jar")} --classpath $library --min-api 14 --output $temporaryDir $inFile".split(" "))
            workingDir(inFile.parentFile)
            standardOutput = System.out
            errorOutput = System.err
        }.assertNormalExitValue()
    }
}

val patchDex = tasks.create("patchDex") {
    dependsOn(genLoaderModDex)
    val inFile = genLoaderModDex.outputs.files.singleFile
    val outFile = temporaryDir.resolve("classes.dex")
    inputs.file(inFile)
    outputs.file(outFile)

    doLast {
        val file = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(14))
        val rewriter = DexRewriter(object : RewriterModule() {
            override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> = Rewriter {
                if (it.length > 20 && it.contains("ExternalSyntheticLambda")) {
                    return@Rewriter it.replace("ExternalSyntheticLambda", "Lambda")
                }
                it
            }
        })
        rewriter.dexFileRewriter.rewrite(file).let {
            DexFileFactory.writeDexFile(outFile.path, it)
        }
    }
}

val genLoaderModAll = tasks.create<Zip>("genLoaderModAll") {
    dependsOn(genLoaderMod, patchDex)
    archiveFileName.set("MindustryX.loader.dex.jar")
    from(zipTree(genLoaderMod.outputs.files.singleFile))
    from(patchDex.outputs.files.singleFile)
}