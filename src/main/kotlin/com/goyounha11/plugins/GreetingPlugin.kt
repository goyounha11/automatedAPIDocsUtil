package com.goyounha11.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class GreetingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val generateDocsTask = project.tasks.register("generateDocs") {
            it.group = "documentation"  // Task group 명
            it.description = "Convert to RestDocs"
        }

        // 'generateDocs' 태스크가 'moveSwaggerFiles' 태스크에 의존하도록 설정
        generateDocsTask.configure {
            it.dependsOn("moveSwaggerFiles")
        }
    }
}