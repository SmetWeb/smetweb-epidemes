plugins {
	kotlin("jvm")
	id("java")
}

dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val dsolVersion: String by project
	val h2Version: String by project

	api(project(":domain"))

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))

	// D-SOL simulator
	api(group = "dsol", name = "dsol-core", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-web", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-demo", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-zmq", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-build-tools", version = dsolVersion)

	// Jason agentspeak
//	api(group = "net.sf.jason", name = "jason", version = "2.3")

	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")

	// persist (JPA)
	compileOnly(group = "javax.persistence", name = "javax.persistence-api", version = "2.2")

	// test
	testImplementation("com.h2database:h2:$h2Version")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
}
