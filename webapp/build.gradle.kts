import org.springframework.boot.gradle.tasks.bundling.BootJar

val mainClass: String = "io.smetweb.web.SmetWebApplication"

plugins {
	kotlin("jvm")

	id("org.jetbrains.kotlin.plugin.spring")
	id("com.github.johnrengelman.processes") version "0.5.0"
	id("org.springdoc.openapi-gradle-plugin") version "1.0.0"
}

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClassName = mainClass
	manifest {
		attributes("Start-Class" to mainClass)
	}
}

dependencies {
	val h2Version: String by project
	val springdocVersion: String by project
//	val ktorVersion: String by project

	api(project(":model"))

	// see https://springdoc.org/
	api(group = "org.springdoc", name = "springdoc-openapi-kotlin", version = springdocVersion)
	api(group = "org.springdoc", name = "springdoc-openapi-ui", version = springdocVersion)
	api(group = "org.springdoc", name = "springdoc-openapi-data-rest", version = springdocVersion)

	api(group = "org.springframework.boot", name = "spring-boot-starter-web")
	api(group = "org.springframework.boot", name = "spring-boot-starter-actuator")
	api(group = "com.h2database", name = "h2", version = h2Version)
//	api(group = "io.ktor", name = "ktor-server-netty", version = ktorVersion)

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test") {
		exclude(module = "junit")
		exclude(group = "org.junit.vintage")
		exclude(module = "mockito-core")
	}
}