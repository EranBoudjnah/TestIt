import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete

plugins {
    id("com.github.ben-manes.versions") version "0.52.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

buildscript {
    repositories {
        google()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val kotlinVersion: String = libs.versions.kotlin.get()

configurations.all {
    resolutionStrategy.eachDependency {
        val requestedDep = this.requested
        if (requestedDep.group == "org.jetbrains.kotlin" && requestedDep.name == "kotlin-reflect") {
            useVersion(kotlinVersion)
        }
        if (requestedDep.group == "com.pinterest" && requestedDep.name == "ktlint") {
            useVersion("0.48.0")
        }
    }
}

tasks.register<Delete>("clean") {
    rootProject.layout.buildDirectory.asFile.get().deleteRecursively()
}

private fun Project.installGitHookFromAutomation() {
    val projectRootDirectory = rootProject.rootDir
    val gitDirectory = File(projectRootDirectory, ".git")
    val gitHooksDirectory = File(gitDirectory, "hooks")
    val suffix = if (Os.isFamily(Os.FAMILY_WINDOWS)) "windows" else "macos"
    val preCommitScriptFile = File(projectRootDirectory, "automation/scripts/pre-commit-$suffix")

    if (!gitDirectory.exists()) {
        logger.lifecycle("⚠\uFE0F Skipping git hook installation: .git directory not found at ${gitDirectory.absolutePath}")
        return
    }
    if (!preCommitScriptFile.exists()) {
        logger.lifecycle("⚠\uFE0F Skipping git hook installation: script not found at ${preCommitScriptFile.absolutePath}")
        return
    }

    if (!gitHooksDirectory.exists()) gitHooksDirectory.mkdirs()
    val scriptDestinationFile = File(gitHooksDirectory, "pre-commit")
    preCommitScriptFile.copyTo(scriptDestinationFile, overwrite = true)
    logger.lifecycle("✅ Installed git pre-commit hook -> ${scriptDestinationFile.absolutePath}")
    if (!scriptDestinationFile.setExecutable(true)) {
        logger.lifecycle("⚠\uFE0F Failed to make pre-commit git hook script executable")
    }
}

private fun Project.installCodeStyleFromAutomation() {
    val projectRootDirectory = rootProject.rootDir
    val projectIdeaCodeStylesDirectory = File(projectRootDirectory, ".idea/codeStyles")
    val projectIdeaCodeStylesFile = File(projectIdeaCodeStylesDirectory, "Project.xml")
    val codeStyleFile = File(projectRootDirectory, "automation/codeStyles/Project.xml")

    if (!projectIdeaCodeStylesDirectory.exists()) projectIdeaCodeStylesDirectory.mkdirs()
    codeStyleFile.copyTo(projectIdeaCodeStylesFile, overwrite = true)
    logger.lifecycle("✅ Installed code style -> ${codeStyleFile.absolutePath}")
}

tasks {
    register<Copy>("installGitHook") {
        group = "automation"
        doLast {
            project.installGitHookFromAutomation()
        }
    }

    register<Copy>("installCodeStyle") {
        group = "automation"
        doLast {
            project.installCodeStyleFromAutomation()
        }
    }
}

gradle.projectsEvaluated {
    project.installGitHookFromAutomation()
    project.installCodeStyleFromAutomation()
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
