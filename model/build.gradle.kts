import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	kotlin("jvm")
	kotlin("kapt")
	kotlin("plugin.spring")

	// add code generation task (xjcGenerate), see https://unbroken-dome.github.io/projects/gradle-xjc-plugin/
	id("org.unbroken-dome.xjc") version "2.0.0"
}

val appClass: String = "io.smetweb.sim.CommandLineApplication"

// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
tasks.withType<BootJar> {
	mainClass.set(appClass)
	manifest {
		attributes("Start-Class" to appClass)
	}
}

xjc {
	extraArgs.addAll(
		"-XautoNameResolution",

		// use XJC plugin: org.jvnet.jaxb2_commons:jaxb2-basics
		"-Xequals",
		"-XhashCode",
//		"-XtoString",

		// use XJC plugin: org.jvnet.jaxb2_commons:jaxb2-fluent-api (adds .with...() methods)
		"-Xfluent-api",

		// use XJC plugin: org.jvnet.jaxb2_commons:jaxb2-default-value
		"-Xdv",

		// use XJC plugin: org.apache.cxf.xjcplugins:cxf-xjc-ts
		"-Xts:style:io.smetweb.xml.OmitNullsToStringStyle.INSTANCE",
	)
}

dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val cxfVersion: String by project
	val h2Version: String by project

	api(project(":core"))

	// Spring Boot (logging, persistence, configuration)
	api(group = "org.springframework.boot", name = "spring-boot-starter-log4j2")
	implementation(platform("org.apache.logging.log4j:log4j-bom:2.17.1")) // due to RCE vulnerability, see https://spring.io/blog/2021/12/10/log4j2-vulnerability-and-spring-boot
	api(group = "com.fasterxml.jackson.core", name = "jackson-databind") // required by log4j2
	api(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml") // required by log4j2
	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

	// Datasource
	api(group = "com.h2database", name = "h2", version = h2Version)

	// XJC plugin, see https://unbroken-dome.github.io/projects/gradle-xjc-plugin/#_specifying_the_plugin_classpath
	implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
	"xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-value-constructor:3.0")
	"xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-fluent-api:3.0")
	"xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-basics:1.11.1")
	"xjcClasspath"("org.jvnet.jaxb2_commons:jaxb2-default-value:1.1")
	"xjcClasspath"("org.apache.cxf.xjcplugins:cxf-xjc-ts:$cxfVersion")
	"xjcClasspath"("org.apache.cxf.xjcplugins:cxf-xjc-dv:$cxfVersion")
	api("org.jvnet.jaxb2_commons:jaxb2-basics-runtime:1.11.1")

	// test
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test") {
		exclude(module = "junit")
		exclude(group = "org.junit.vintage")
		exclude(module = "mockito-core")
	}
	testImplementation("com.ninja-squad:springmockk:3.1.1")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")
}
