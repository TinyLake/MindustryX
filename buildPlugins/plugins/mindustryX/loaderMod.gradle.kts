package mindustryX

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.rewriter.DexRewriter
import com.android.tools.smali.dexlib2.rewriter.Rewriter
import com.android.tools.smali.dexlib2.rewriter.RewriterModule
import com.android.tools.smali.dexlib2.rewriter.Rewriters
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("de.undercouch.download")
}

tasks {
    val downloadOriginJar by registering(de.undercouch.gradle.tasks.download.Download::class) {
        outputs.cacheIf { true }
        val upstreamBuild = project.properties["upstreamBuild"] as String?
        val output = temporaryDir.resolve("v$upstreamBuild.jar")
        inputs.property("upstreamBuild", upstreamBuild)

        src("https://github.com/Anuken/Mindustry/releases/download/v$upstreamBuild/Mindustry.jar")
        dest(output)
        overwrite(false)
    }
    val distTask = provider { getByPath("::desktop:dist") }
    val genLoaderMod by registering {
        outputs.cacheIf { true }
        val androidTask = findByPath("::android:compileReleaseJavaWithJavac")
        dependsOn(downloadOriginJar, distTask)
        if (androidTask != null)
            dependsOn(androidTask)
        val outputF = layout.buildDirectory.file("libs/Mindustry.loader.jar")
        inputs.files(distTask)
        outputs.file(outputF)
        doLast {
            val input = ZipFile(distTask.get().outputs.files.singleFile)
            val base = ZipFile(downloadOriginJar.get().outputFiles.single())
            val output = ZipOutputStream(outputF.get().asFile.outputStream())
            val baseMap = base.entries().asSequence().associateBy { it.name }

            for (entry in input.entries()) {
                if (entry.name.startsWith("sprites/") || entry.name == "version.properties") continue
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
                output.putNextEntry(entry)
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

    val genLoaderModDex by registering(Exec::class) {
        outputs.cacheIf { true }
        dependsOn(genLoaderMod, distTask)
        inputs.files(files(genLoaderMod))
        val outFile = temporaryDir.resolve("classes.dex")
        outputs.file(outFile)

        commandLine(provider {
            val library = distTask.get().outputs.files.singleFile
            val inFile = genLoaderMod.get().outputs.files.singleFile
            val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
            if (sdkRoot == null || !File(sdkRoot).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

            val d8Tool = File("$sdkRoot/build-tools/").listFiles()?.sortedDescending()
                ?.flatMap { dir -> (dir.listFiles().orEmpty()).filter { it.name.startsWith("d8") } }?.firstOrNull()
                ?: throw GradleException("No d8 found. Ensure that you have an Android platform installed.")
            val platformRoot = File("$sdkRoot/platforms/").listFiles()?.sortedDescending()?.firstOrNull { it.resolve("android.jar").exists() }
                ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")
            "$d8Tool --lib ${platformRoot.resolve("android.jar")} --classpath $library --min-api 14 --output $temporaryDir $inFile".split(" ")
        })
        workingDir(provider { genLoaderMod.get().outputs.files.singleFile.parentFile })
        standardOutput = System.out
        errorOutput = System.err

        //fix ExternalSyntheticLambda
        doLast {
            val file = DexFileFactory.loadDexFile(outFile, Opcodes.forApi(14))
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

    val genLoaderModAll by registering(Zip::class) {
        dependsOn(genLoaderMod, genLoaderModDex)
        archiveFileName.set("MindustryX.loader.dex.jar")
        from(zipTree(genLoaderMod.get().outputs.files.singleFile))
        from(genLoaderModDex)
    }
}