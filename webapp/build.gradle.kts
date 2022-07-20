//import org.jetbrains.kotlin.backend.wasm.lower.excludeDeclarationsFromCodegen
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	application
	kotlin("jvm")
	kotlin("kapt")
	kotlin("plugin.spring")

	// see https://unbroken-dome.github.io/projects/gradle-xjc-plugin/
	id("org.unbroken-dome.xjc") version "2.0.0"
	id("com.github.johnrengelman.processes") version "0.5.0"
//	id("org.springdoc.openapi-gradle-plugin") version "1.3.0"

}

val appClass: String = "io.smetweb.web.SmetWebApplication.Companion"

application {
	mainClass.set(appClass)
}

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClass.set(appClass)
	manifest {
		attributes("Start-Class" to appClass)
	}
}

// see https://github.com/springdoc/springdoc-openapi-gradle-plugin#customization
//openApi {
////	apiDocsUrl.set("https://localhost:8080/api-docs")
//	outputDir.set(file("$buildDir/docs"))
//	outputFileName.set("swagger.json")
//	waitTimeInSeconds.set(10)
////	forkProperties.set("-Dspring.profiles.active=special")
//}

dependencies {
//	val springVersion: String by System.getProperties()
	val h2Version: String by project
	val springdocVersion: String by project
//	val ktorVersion: String by project
	val vertxVersion: String by project

	api(project(":model"))

	// see https://springdoc.org/
	api(group = "org.springdoc", name = "springdoc-openapi-kotlin", version = springdocVersion)
//	api(group = "org.springdoc", name = "springdoc-openapi-ui", version = springdocVersion)
//	api(group = "org.springdoc", name = "springdoc-openapi-data-rest", version = springdocVersion)
	api(group = "org.springdoc", name = "springdoc-openapi-webflux-ui", version = springdocVersion)

	api(group = "com.h2database", name = "h2", version = h2Version)
	runtimeOnly(group = "org.springframework.boot", name = "spring-boot-starter-log4j2")
	runtimeOnly(group = "org.fusesource.jansi", name = "jansi", version = "1.18")
	// jANSI for log4j2 on Windows, see http://logging.apache.org/log4j/2.x/manual/layouts.html#enable-jansi

	api(group = "org.springframework.boot", name = "spring-boot-starter-webflux")
	api(group = "io.projectreactor.addons", name = "reactor-adapter", version = "3.4.3") // reactiveX <-> rxKotlin
	api(group = "io.projectreactor.kotlin", name = "reactor-kotlin-extensions", version = "1.1.3")
	api(group = "org.springframework.boot", name = "spring-boot-starter-security")
	api(group = "org.springframework.boot", name = "spring-boot-starter-data-rest") {
		exclude(module = "spring-boot-starter-tomcat") // replace tomcat with Vert.x
	}
	api(group = "org.springframework", name = "spring-context") {
		exclude(module = "spring-aop")
	}
//	api("io.projectreactor.netty:reactor-netty")

	// all this stuff for standard validation, really?
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")
	api(group = "org.glassfish", name = "javax.el", version = "3.0.0")
	api(group = "org.hibernate", name = "hibernate-validator", version = "6.2.0.Final")

	// Vert.x
	api(group = "io.vertx", name = "vertx-lang-kotlin-coroutines", version = vertxVersion)
	api(group = "io.vertx", name = "vertx-web", version = vertxVersion)
	api(group = "io.vertx", name = "vertx-rx-java2", version = vertxVersion)
//	api(group = "io.ktor", name = "ktor-server-netty", version = ktorVersion)

	// OData V3
	// for code generated from XSD by XJC (with gradle plugin), but also in hibernate-core (by spring-boot-starter-data-jpa)
	implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
	implementation("com.fasterxml.woodstox:woodstox-core:6.2.8")

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.springframework.security", name = "spring-security-test")
	testImplementation(group = "io.projectreactor", name = "reactor-test")
	testImplementation(group = "io.vertx", name = "vertx-junit5", version = vertxVersion)
}