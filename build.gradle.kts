// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  id("com.android.application") version "9.0.0-alpha06" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
  id("com.diffplug.spotless") version "8.6.0"
}

allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
      target("**/*.kt")
      ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
      target("**/*.gradle.kts")
      ktfmt()
    }
  }

  // 绑定到 Android 模块的 preBuild 生命周期，实现自动注册 Git Hook
  plugins.withId("com.android.application") {
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(":registerGitHooks") }
  }
  plugins.withId("com.android.library") {
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(":registerGitHooks") }
  }
}

// 注册复制 Git Hooks 的任务
tasks.register<Copy>("registerGitHooks") {
  description = "Copies the git hooks from scripts to the .git folder"
  group = "git hooks"
  from(file("scripts/pre-commit"))
  into(file(".git/hooks"))
  filePermissions {
    user {
      read = true
      write = true
      execute = true
    }
    group {
      read = true
      execute = true
    }
    other {
      read = true
      execute = true
    }
  }
}
