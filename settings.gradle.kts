// 项目根目录 settings.gradle.kts（完整配置）
pluginManagement {
    repositories {
        // 国内镜像优先（解决谷歌仓库访问问题）
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/jcenter")
        // 官方仓库兜底
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // 关键：禁止项目级仓库配置
    repositories {
        // 同样添加国内镜像（与 pluginManagement 一致）
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/jcenter")
        google()
        mavenCentral()
    }
}

rootProject.name = "MMAAMS_Exp_Equip" // 你的项目名称，保持不变
include(":app") // 你的模块，保持不变