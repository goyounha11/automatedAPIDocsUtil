package com.goyounha11.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class GenerateDocsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.hidetake.swagger.generator")

        project.extensions.configure<SwaggerSources>("swaggerSources") {
            it.create("openapi3") {
                // 여기에 필요한 설정을 추가...
            }
        }


        // 'generateDocs' 태스크 정의
        val generateDocsTask = project.tasks.register("generateDocs") {
            it.group = "documentation"
            it.description = "Convert to RestDocs"
        }

        // 'moveSwaggerFiles' 태스크 정의 및 설정
        project.tasks.register("moveSwaggerFiles", Copy::class.java) {
            it.dependsOn("generateSwaggerUIConvert")
            it.from("${project.buildDir}/swagger-ui-convert")
            it.into("${project.buildDir}/resources/main/static/docs/")
        }

        // 'generateDocs' 태스크가 'moveSwaggerFiles'에 의존하도록 설정
        generateDocsTask.configure {
            it.dependsOn("moveSwaggerFiles")
        }
    }
}