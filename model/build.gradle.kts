import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm")
	kotlin("kapt")

	id("org.jetbrains.kotlin.plugin.spring")
}

val mainClass: String = "io.smetweb.sim.CommandLineApplication"

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClassName = mainClass.toString()
	manifest {
		attributes("Start-Class" to mainClass)
	}
}

dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val dsolVersion: String by project
	val h2Version: String by project

	api(project(":core"))

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))

	// D-SOL simulator
	api(group = "dsol", name = "dsol-core", version = dsolVersion)

	// Spring
//	api(group = "org.springframework.boot", name = "spring-boot-starter") {
//		exclude(module = "spring-boot-starter-logging")
//		exclude(module = "org.apache.logging.log4j", group = "log4j-to-slf4j")
//	}
	api(group = "org.springframework.boot", name = "spring-boot-starter-log4j2")
	api(group = "com.fasterxml.jackson.core", name = "jackson-databind") // required by log4j2
	api(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml") // required by log4j2
	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

	// test
	testImplementation(group = "com.h2database", name = "h2", version = h2Version)
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test") {
		exclude(module = "junit")
		exclude(group = "org.junit.vintage")
		exclude(module = "mockito-core")
	}
	testImplementation("com.ninja-squad:springmockk:1.1.3")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")
}
