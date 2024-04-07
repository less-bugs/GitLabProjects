plugins {
    id("org.jetbrains.intellij") version "1.17.3"
    id("io.franzbecker.gradle-lombok") version "5.0.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
}

intellij {
    version.set("IC-2024.1")
    plugins.set(listOf("vcs-git"))
    pluginName.set("GitLabProjects")
}

lombok {
    version = "1.18.22"
    sha256 = ""
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("commons-io:commons-io:2.7")
    implementation("org.gitlab:java-gitlab-api:4.0.0")
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        version.set("${project.version}")
        sinceBuild.set("241")
        untilBuild.set("241.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}
