plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.lojia.pos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lojia.pos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        warning.add("MissingTranslation")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.google.material)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0")
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register("verifyStringLocalization") {
    description = "Verifies that all values-*/strings.xml files contain all keys from values/strings.xml with zero missing or extra keys."
    group = "verification"

    doLast {
        val resDir = file("src/main/res")
        val baseFile = File(resDir, "values/strings.xml")
        if (!baseFile.exists()) {
            throw GradleException("Base strings.xml not found at ${baseFile.absolutePath}")
        }

        fun extractKeys(file: File): List<String> {
            val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(file)
            val nodeList = doc.getElementsByTagName("string")
            val keys = mutableListOf<String>()
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                val nameAttr = node.attributes?.getNamedItem("name")?.nodeValue
                if (nameAttr != null) {
                    keys.add(nameAttr)
                }
            }
            return keys
        }

        val baseKeysList = extractKeys(baseFile)
        val baseKeysSet = baseKeysList.toSet()

        val localeDirs = resDir.listFiles { f: File -> f.isDirectory && f.name.startsWith("values-") }?.sortedBy { it.name } ?: emptyList()
        val errors = mutableListOf<String>()

        for (dir in localeDirs) {
            val locFile = File(dir, "strings.xml")
            if (!locFile.exists()) continue

            val locKeysList = extractKeys(locFile)
            val locKeysSet = locKeysList.toSet()

            val missing = baseKeysSet - locKeysSet
            val extra = locKeysSet - baseKeysSet

            if (missing.isNotEmpty()) {
                errors.add("Locale '${dir.name}' is missing ${missing.size} key(s): ${missing.take(10).joinToString(", ")}${if (missing.size > 10) "..." else ""}")
            }
            if (extra.isNotEmpty()) {
                errors.add("Locale '${dir.name}' has ${extra.size} extra key(s): ${extra.take(10).joinToString(", ")}${if (extra.size > 10) "..." else ""}")
            }
        }

        if (errors.isNotEmpty()) {
            val errorMsg = "String Localization Verification Failed:\n" + errors.joinToString("\n")
            throw GradleException(errorMsg)
        } else {
            println("✓ String localization verified: Base (${baseKeysSet.size} keys) strictly matches across all ${localeDirs.size} locale files.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("verifyStringLocalization")
}

