import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

buildscript {

	// configured in `gradle.properties`
	val kotlinVersion: String by System.getProperties()
	val springBootVersion: String by System.getProperties()
	val hibernateVersion: String by System.getProperties()
	val spotBugsVersion: String by System.getProperties()

	repositories {
		jcenter()
	}

	// gradle/build classpath, see https://docs.gradle.org/current/userguide/tutorial_using_tasks.html#sec:build_script_external_dependencies
	dependencies {

		// see https://gradle.org/kotlin/ and https://kotlinlang.org/docs/reference/using-gradle.html
		classpath(kotlin("gradle-plugin", version = kotlinVersion))
//		"classpath"(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = kotlinVersion)

		// see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
		classpath(group = "org.springframework.boot", name = "spring-boot-gradle-plugin", version = springBootVersion)

		// required by kapt, see https://kotlinlang.org/docs/reference/kapt.html
		classpath(group = "org.hibernate", name = "hibernate-jpamodelgen", version = hibernateVersion)

		// see https://spotbugs.github.io/
		classpath(group = "gradle.plugin.com.github.spotbugs", name = "spotbugs-gradle-plugin", version = spotBugsVersion)
	}
}

// plug-in DSL, see https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block
// for configuring allProjects, see also https://stackoverflow.com/a/26240341
plugins {
	val kotlinVersion: String by System.getProperties()
	val springBootVersion: String by System.getProperties()

	kotlin("kapt") version kotlinVersion apply false

	id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion apply false
	id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion apply false
	id("org.springframework.boot") version springBootVersion apply false

	// replaces findBugs, see https://spotbugs.readthedocs.io/en/latest/gradle.html
	// TODO fix logging and rule violation issue(s)
//	val spotBugsVersion: String by System.getProperties()
//	id("com.github.spotbugs") version spotBugsVersion apply false
}

val lastTask = Action<Task> {
	println("I'm ${this.project.name}")
}

// see https://docs.gradle.org/current/userguide/multi_project_builds.html
// see https://spring.io/guides/gs/multi-module/
allprojects {

	group = "io.smetweb"
	version = "0.0.1-SNAPSHOT"

	repositories {
		jcenter()
		mavenCentral()
		maven(url = "https://simulation.tudelft.nl/maven")
		maven(url = "https://djutils.org/maven")
		maven(url = "https://djunits.org/maven")
	}

	// adds build lifecycle tasks, see https://docs.gradle.org/current/userguide/base_plugin.html
//	apply(plugin = "base")
	// Java compilation tasks, see https://docs.gradle.org/current/userguide/java_plugin.html
//	apply(plugin = "java")
	// calculate code coverage, see https://docs.gradle.org/current/userguide/jacoco_plugin.html
	apply(plugin = "jacoco")
	// deploy to Maven repositories, see https://docs.gradle.org/current/userguide/maven_plugin.html
	apply(plugin = "maven-publish")
	// manage versions, see https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
	apply(plugin = "io.spring.dependency-management")
	// generate IDEA module file, see https://docs.gradle.org/current/userguide/idea_plugin.html
	apply(plugin = "idea")

	val kotlinVersion: String by System.getProperties()
	the<DependencyManagementExtension>().apply {
		imports {
			mavenBom(SpringBootPlugin.BOM_COORDINATES) {
				// see https://github.com/Kotlin/kotlinx.coroutines/issues/1300
				// and https://youtrack.jetbrains.com/issue/KT-27994#focus=streamItem-27-3148043.0-0
				bomProperty("kotlin.version", kotlinVersion)
			}
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform() // see https://stackoverflow.com/a/50326943
	}

	tasks.register("hello") {
		doLast(lastTask)
	}

// 'kotlin-jvm' plugin config
	tasks.withType<KotlinCompile> {
		// see https://kotlinlang.org/docs/reference/using-gradle.html#compiler-options
		kotlinOptions {
			suppressWarnings = true
			jvmTarget = "1.8"
			freeCompilerArgs = listOf(
					"-Xjsr305=strict"
			)
		}
	}
}

//dependencies {
//	// Make the root project archives configuration depend on every sub-project
//	subprojects.forEach {
//		archives(it)
//	}
//}