import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {

	dependencies {
//		classpath("no.nils:wsdl2java:0.12")
//		classpath("no.nils:xsd2java:0.12")
//		classpath("com.github.bjornvester:xjc:1.3")
	}
}

plugins {
	kotlin("jvm")

	id("org.jetbrains.kotlin.plugin.spring")

	// execute a 'mainClassName', see https://docs.gradle.org/current/userguide/application_plugin.html
    // application

	//	id("com.github.bjornvester.xjc") version "1.3"
}

// project classpath
dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val rxJavaVersion: String by project
	val rxKotlinVersion: String by project
	val h2Version: String by project

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))

	compile(project(":core"))

	// units of measurement (JSR-363)
	api(group = "systems.uom", name = "systems-common-java8", version = "1.0")

	// persistence (JPA, multiple JSRs)
	compileOnly(group = "javax.persistence", name = "javax.persistence-api", version = "2.2")

	// spring (task scheduling)
	compileOnly(group = "org.springframework", name = "spring-context")

	// managed concurrency (JSR-236)
	api(group = "javax.enterprise.concurrent", name = "javax.enterprise.concurrent-api", version = "1.1")

	// Jackson for JSON de/serialization
	api(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jdk8")
	api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")

	// RxKotlin and RxJava, see https://www.baeldung.com/rxkotlin
	api(group = "io.reactivex.rxjava2", name = "rxkotlin", version = rxKotlinVersion)
	api(group = "io.reactivex.rxjava2", name = "rxjava", version = rxJavaVersion)

	// make deployable and executable, see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/#packaging-executable-wars-deployable
//	runtimeOnly("org.springframework.boot:spring-boot-starter-tomcat")

//	api("no.nils:wsdl2java:0.12")
//	api("no.nils:xsd2java:0.12")

	// jUnit
	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")

	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")
	testImplementation("com.h2database:h2:$h2Version")
}

//application {
//	mainClassName = "cli.Main"
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

// see https://stackoverflow.com/a/35366220/1418999
//tasks.withType<Wsdl2JavaTask> {
//	locale = Locale.FRANCE
//	cxfVersion = "2.5.1"
//	cxfPluginVersion = "2.4.0"
//	encoding = "utf-8"
//	xsdsToGenerate = [
//		["src/main/resources/xsd/CustomersAndOrders.xsd", "no.nils.xsd2java.sample", mapOf("header" to false)] /* optional map */]
//	]
//	generatedXsdDir = file("generatedsources/xsd2java")
//	wsdl2java {
//		wsdlDir = file("$projectDir/src/main/resources/wsdl")
//		wsdlsToGenerate = listOf(
//				listOf("-p", "com.acme.mypackage",
//				"-autoNameResolution",
//				"$projectDir/src/main/resources/wsdl/stockqoute.wsdl"))
//
//	}
//}

// TODO run Apache CXF's xsd2java plugin, see https://stackoverflow.com/a/35366220
//        or https://github.com/gmateo/apache-cxf-example/blob/master/build.gradle
