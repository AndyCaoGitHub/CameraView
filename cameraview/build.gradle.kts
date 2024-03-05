import io.deepmedia.tools.publisher.common.License
import io.deepmedia.tools.publisher.common.Release
import io.deepmedia.tools.publisher.common.GithubScm

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("io.deepmedia.tools.publisher")
    id("jacoco")
}

android {
    compileSdk = property("compileSdkVersion") as Int
    defaultConfig {
        minSdk = property("minSdkVersion") as Int
        targetSdk = property("targetSdkVersion") as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["filter"] = "" +
                "com.otaliastudios.cameraview.tools.SdkExcludeFilter," +
                "com.otaliastudios.cameraview.tools.SdkIncludeFilter"
    }
    buildTypes["debug"].isTestCoverageEnabled = true
    buildTypes["release"].isMinifyEnabled = false
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    api("androidx.exifinterface:exifinterface:1.3.7")
    api("androidx.lifecycle:lifecycle-common:2.7.0")
    api("com.google.android.gms:play-services-tasks:18.1.0")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("com.otaliastudios.opengl:egloo:0.6.1")
}

// Publishing

// 设置项目描述
publisher {
    project.description = "A well documented, high-level Android interface that makes capturing " +
            "pictures and videos easy, addressing all of the common issues and needs. " +
            "Real-time filters, gestures, watermarks, frame processing, RAW, output of any size."

    // 设置项目的 artifactId，通常与项目名相同
    project.artifact = "cameraview"

    // 设置项目的 group ID，用于在仓库中组织项目
    project.group = "com.otaliastudios"

    // 设置项目的 URL，通常是项目的 GitHub 或其他托管服务的链接
    project.url = "https://github.com/natario1/CameraView"

    // 设置项目的 SCM（Source Control Management）信息，包括 GitHub 用户名和项目名
    project.scm = GithubScm("natario1", "CameraView")

    // 添加项目许可证信息，这里使用的是 Apache License 2.0
    project.addLicense(License.APACHE_2_0)

    // 添加开发者信息，包括开发者的 GitHub 用户名和电子邮件
    project.addDeveloper("natario1", "mat.iavarone@gmail.com")

    // 设置 release 版本的一些自动配置选项
    release.sources = Release.SOURCES_AUTO
    release.docs = Release.DOCS_AUTO

    // 设置 release 版本的版本号
    release.version = "2.7.2"

    // 创建一个目录，这可能是为了组织构建输出
    directory()

    // 配置 Sonatype 仓库的认证信息
    sonatype {
        // Sonatype 用户名
        auth.user = "SONATYPE_USER"
        // Sonatype 密码
        auth.password = "SONATYPE_PASSWORD"
        // GPG 签名密钥
        signing.key = "SIGNING_KEY"
        // GPG 签名密码
        signing.password = "SIGNING_PASSWORD"
    }

    // 配置 Sonatype 快照仓库的发布信息
    sonatype("snapshot") {
        // 快照仓库的 repository ID
        repository = io.deepmedia.tools.publisher.sonatype.Sonatype.OSSRH_SNAPSHOT_1
        // 快照版本的版本号，通常使用 latest-SNAPSHOT 表示最新的快照版本
        release.version = "latest-SNAPSHOT"
        // 快照仓库的认证信息
        auth.user = "SONATYPE_USER"
        auth.password = "SONATYPE_PASSWORD"
        // GPG 签名密钥和密码，用于快照版本
        signing.key = "SIGNING_KEY"
        signing.password = "SIGNING_PASSWORD"
    }
}


// Code Coverage
val buildDir = project.buildDir.absolutePath
val coverageInputDir = "$buildDir/coverage_input" // changing? change github workflow
val coverageOutputDir = "$buildDir/coverage_output" // changing? change github workflow

// Run unit tests, with coverage enabled in the android { } configuration.
// Output will be an .exec file in build/jacoco.
tasks.register("runUnitTests") { // changing name? change github workflow
    dependsOn("testDebugUnitTest")
    doLast {
        copy {
            from("$buildDir/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
            into("$coverageInputDir/unit_tests") // changing? change github workflow
        }
    }
}

// Run android tests with coverage.
tasks.register("runAndroidTests") { // changing name? change github workflow
    dependsOn("connectedDebugAndroidTest")
    doLast {
        copy {
            from("$buildDir/outputs/code_coverage/debugAndroidTest/connected")
            include("*coverage.ec")
            into("$coverageInputDir/android_tests") // changing? change github workflow
        }
    }
}

// Merge the two with a jacoco task.
jacoco { toolVersion = "0.8.5" }
tasks.register("computeCoverage", JacocoReport::class) {
    dependsOn("compileDebugSources") // Compile sources, needed below
    executionData.from(fileTree(coverageInputDir))
    sourceDirectories.from(android.sourceSets["main"].java.srcDirs)
    additionalSourceDirs.from("$buildDir/generated/source/buildConfig/debug")
    additionalSourceDirs.from("$buildDir/generated/source/r/debug")
    classDirectories.from(fileTree("$buildDir/intermediates/javac/debug") {
        // Not everything here is relevant for CameraView, but let's keep it generic
        exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "android/**",
                "androidx/**",
                "com/google/**",
                "**/*\$ViewInjector*.*",
                "**/Dagger*Component.class",
                "**/Dagger*Component\$Builder.class",
                "**/*Module_*Factory.class",
                // We don"t test OpenGL filters.
                "**/com/otaliastudios/cameraview/filters/**.*"
        )
    })
    reports.html.required.set(true)
    reports.xml.required.set(true)
    reports.html.outputLocation.set(file("$coverageOutputDir/html"))
    reports.xml.outputLocation.set(file("$coverageOutputDir/xml/report.xml"))
}