plugins {
    kotlin("multiplatform") version "1.8.0"
}

group = "io.ygdrasil"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    val nativeTarget = when (System.getProperty("os.name")) {
        "Mac OS X" -> macosX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"

            }

        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:multik-core:0.2.0")
            }
        }

        val nativeMain by getting
        val nativeTest by getting
    }
}
