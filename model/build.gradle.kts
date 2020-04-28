import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm")
	kotlin("kapt")

	id("org.jetbrains.kotlin.plugin.spring")
}

val mainClass: String = "io.smetweb.sim.CommandLineApplication"

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClassName = mainClass
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
