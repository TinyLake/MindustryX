package mindustryX

import java.io.ByteArrayOutputStream

tasks {
    // 配置
    val kaptGenDir = layout.buildDirectory.dir("generated/source/kapt/main")
    val backupDir = layout.buildDirectory.dir("tmp/kapt_manual_edits") // 临时备份位置
    val patchOutputFile = rootDir.resolve("../patches/generated.patch") // 最终生成的 patch

// 1. Pre: 备份你手动修改过的代码
    val backupKaptChanges by tasks.registering(Copy::class) {
        description = "Backs up manually modified kapt sources."

        // 只有当目录存在时才备份
        onlyIf { kaptGenDir.get().asFile.exists() }

        from(kaptGenDir)
        into(backupDir)

        // 每次运行前清理旧备份
        doFirst {
            if (backupDir.get().asFile.exists()) {
                backupDir.get().asFile.deleteRecursively()
            }
        }
    }

    val generateKaptDiff by tasks.registering(Exec::class) {
        group = "mdtx"
        dependsOn("kaptKotlin", backupKaptChanges)
        workingDir = kaptGenDir.get().asFile

        commandLine(
            "git",
            "-c", "core.safecrlf=false",
            "diff",
            "--no-index", "--no-prefix",
            ".",
            backupDir.get().asFile.absolutePath,
        )
        isIgnoreExitValue = true

        val stdout = ByteArrayOutputStream()
        standardOutput = stdout
        doLast {
            patchOutputFile.parentFile.mkdirs()
            patchOutputFile.writeText(buildString {
                val lines = stdout.toByteArray().toString(Charsets.UTF_8).lines()
                var last = ""
                for (line in lines) {
                    if (line.startsWith("diff ") || line.startsWith("index "))
                        continue
                    if (line.startsWith("--- "))
                        last = line
                    if (line.startsWith("+++ ")) {
                        appendLine(last.replace("--- ", "+++ "))
                        continue
                    }
                    appendLine(line)
                }
            })
        }
    }

    val patchGeneratedSources by registering(Exec::class) {
        group = "mdtx"
        dependsOn("kaptKotlin")
        workingDir = kaptGenDir.get().asFile
        environment("GIT_DIR", "NOT_GIT")
        commandLine = listOf("git", "apply", "--ignore-space-change", "--ignore-whitespace", rootDir.resolve("../patches/generated.patch").absolutePath)
    }

    afterEvaluate {
        named("kaptKotlin") {
            mustRunAfter(backupKaptChanges)
        }
        named("compileJava") {
            dependsOn(patchGeneratedSources)
            inputs.file(patchOutputFile).withPathSensitivity(PathSensitivity.NONE)
        }
    }
}