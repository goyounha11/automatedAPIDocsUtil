import org.hidetake.gradle.swagger.generator.GenerateSwaggerUI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
    id("com.epages.restdocs-api-spec") version "0.18.2"
    id("org.hidetake.swagger.generator") version "2.19.2"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
}

group = "com.goyounha11"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation ("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.18.2")

    swaggerCodegen("io.swagger.codegen.v3:swagger-codegen-cli:3.0.44")
    swaggerUI("org.webjars:swagger-ui:4.1.3")

    compileOnly("org.springframework.boot:spring-boot-starter-test")
    compileOnly ("org.springframework.restdocs:spring-restdocs-mockmvc")
    compileOnly("com.epages:restdocs-api-spec-mockmvc:0.18.2")
}


val snippetsDir by extra {
    file("${layout.buildDirectory}/generated-snippets")
}

val swaggerDir by extra {
    file("${layout.buildDirectory}/swagger-ui-convert")
}

val activeProfile = project.findProperty("spring.profiles.active") as? String ?: "local"

openapi3 {
    this.setServer(getClientServerUrl(activeProfile))
    title = "MapleBoss Api Document"
    description = "MapleBoss API"
    version = "0.1.1"
    format = "json"
}

swaggerSources {
    create("convert") {
        println("asd")
        println("$layout.buildDirectory")
        setInputFile(file("$buildDir/api-spec/openapi3.json"))
        code.language = "html"

        ui.doLast {
            copy {
                from("../config/index.html")
                into(swaggerDir)
                rename { fileName ->
                    fileName.replace("index.html", "index.html")
                }
            }
        }
    }
}

tasks {
    withType<GenerateSwaggerUI> {
        dependsOn("openapi3")
    }

    test {
        outputs.dir(snippetsDir)
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }

    register<Copy>("moveSwaggerFiles") {
        dependsOn("generateSwaggerUIConvert")
        from("${layout.buildDirectory}/swagger-ui-convert")
        into("${layout.buildDirectory}/resources/main/static/docs/")
    }
}

fun getClientServerUrl(profile: String): String {
    return when (profile) {
        "local" -> "http://localhost:8080"
        "dev" -> "https://dev-api.mapleboss.io"
        else -> "http://localhost:8000"
    }
}