import org.jetbrains.kotlin.backend.wasm.lower.excludeDeclarationsFromCodegen
import org.springframework.boot.gradle.tasks.bundling.BootJar

val mainClass: String = "io.smetweb.web.SmetWebApplication.Companion"

plugins {
	application
	kotlin("jvm")
	kotlin("kapt")

	id("org.jetbrains.kotlin.plugin.spring")
	id("com.github.johnrengelman.processes") version "0.5.0"
//	id("org.springdoc.openapi-gradle-plugin") version "1.3.0"
}

application {
	mainClassName = "$mainClass"
}

// see https://github.com/springdoc/springdoc-openapi-gradle-plugin#customization
//openApi {
////	apiDocsUrl.set("https://localhost:8080/api-docs")
//	outputDir.set(file("$buildDir/docs"))
//	outputFileName.set("swagger.json")
//	waitTimeInSeconds.set(10)
////	forkProperties.set("-Dspring.profiles.active=special")
//}

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClassName = mainClass.toString()
	manifest {
		attributes("Start-Class" to mainClass)
	}
}

dependencies {
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
		exclude(module = "spring-boot-starter-tomcat")
	}
	api("org.springframework:spring-context") {
		exclude(module = "spring-aop")
	}
//	api("io.projectreactor.netty:reactor-netty")

	// all this stuff for standard validation, really?
	kapt("org.springframework.boot:spring-boot-configuration-processor")
	api(group = "javax.validation", name = "validation-api", version = "2.0.1.Final")
	api(group = "javax.el", name = "javax.el-api", version = "3.0.0")
	api(group = "org.glassfish.web", name = "javax.el", version = "2.2.6")
	api(group = "org.hibernate", name = "hibernate-validator", version = "6.1.5.Final")

	api(group = "io.vertx", name = "vertx-lang-kotlin-coroutines", version = vertxVersion)
	api(group = "io.vertx", name = "vertx-web", version = vertxVersion)
	api(group = "io.vertx", name = "vertx-rx-java2", version = vertxVersion)
//	api(group = "io.ktor", name = "ktor-server-netty", version = ktorVersion)

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.springframework.security", name = "spring-security-test")
	testImplementation(group = "io.projectreactor", name = "reactor-test")
	testImplementation(group = "io.vertx", name = "vertx-junit5", version = vertxVersion)
}