import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm")

	// execute a 'mainClassName', see https://docs.gradle.org/current/userguide/application_plugin.html
//	application

	id("org.jetbrains.kotlin.plugin.spring")
}

//application {
//	mainClassName = "io.xchain.demo.Main"
//}

// separate boot-jar, see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/#packaging-executable-wars
tasks.withType<BootJar> {
	@Suppress("DEPRECATION")
	classifier = "boot"
//	archiveClassifier = "boot" ??

	// same for Kotlin? see https://stackoverflow.com/a/44293970
	mainClassName = "io.xchain.demo.MainKt"
	manifest {
		attributes("Start-Class" to "io.xchain.demo.DemoApplicationKt")
	}
	launchScript {
		properties(mapOf("logFilename" to "example-app.log"))
	}
}

dependencies {
	val h2Version: String by project
	val springfoxVersion: String by project

	api(project(":persist"))
	api("io.springfox:springfox-data-rest:$springfoxVersion")
	api("io.springfox:springfox-swagger2:$springfoxVersion")
	api("io.springfox:springfox-swagger-ui:$springfoxVersion")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "junit", module = "junit")
	}
	testImplementation("com.h2database:h2:$h2Version")

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")

	// make deployable and executable, see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/#packaging-executable-wars-deployable
	runtimeOnly("org.springframework.boot:spring-boot-starter-tomcat")
}