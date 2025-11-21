// 项目根目录 build.gradle.kts（最终修复版）
buildscript {
    // 关键：为 buildscript 单独添加仓库（下载 AGP 和 Kotlin 插件用）
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2") // AGP 插件
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22") // Kotlin 插件
    }
}

// 无需添加 allprojects 代码块（由 settings 管理）

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}