plugins {
    id("com.android.application")
}

android {
    namespace = "es.chiteroman.playintegrityfix"
    compileSdk = 35
    ndkVersion = "27.0.11902837"
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "es.chiteroman.playintegrityfix"
        minSdk = 26
        targetSdk = 35
        versionCode = 16500
        versionName = "v16.6"
        multiDexEnabled = false

        packaging {
            resources {
                excludes += "META-INF/**"
            }
            jniLibs {
                excludes += "**/liblog.so"
                excludes += "**/libdobby.so"
            }
        }

        externalNativeBuild {
            ndk {
                jobs = Runtime.getRuntime().availableProcessors()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }
}

tasks.register("updateModuleProp") {
    doLast {
        val versionName = project.android.defaultConfig.versionName
        val versionCode = project.android.defaultConfig.versionCode

        val modulePropFile = project.rootDir.resolve("module/module.prop")

        var content = modulePropFile.readText()

        content = content.replace(Regex("version=.*"), "version=$versionName")
        content = content.replace(Regex("versionCode=.*"), "versionCode=$versionCode")

        modulePropFile.writeText(content)
    }
}


tasks.register("copyFiles") {
    dependsOn("updateModuleProp")

    doLast {
        val moduleFolder = project.rootDir.resolve("module")
        val dexFile =
            project.layout.buildDirectory.get().asFile.resolve("intermediates/dex/release/minifyReleaseWithR8/classes.dex")
        val soDir =
            project.layout.buildDirectory.get().asFile.resolve("intermediates/stripped_native_libs/release/stripReleaseDebugSymbols/out/lib")

        dexFile.copyTo(moduleFolder.resolve("classes.dex"), overwrite = true)

        soDir.walk().filter { it.isFile && it.extension == "so" }.forEach { soFile ->
            val abiFolder = soFile.parentFile.name
            val destination = moduleFolder.resolve("zygisk/$abiFolder.so")
            soFile.copyTo(destination, overwrite = true)
        }
    }
}

tasks.register<Zip>("zip") {
    dependsOn("copyFiles")

    archiveFileName.set("PlayIntegrityFix_${project.android.defaultConfig.versionName}.zip")
    destinationDirectory.set(project.rootDir.resolve("out"))

    from(project.rootDir.resolve("module"))
}

afterEvaluate {
    tasks["assembleRelease"].finalizedBy("updateModuleProp", "copyFiles", "zip")
}