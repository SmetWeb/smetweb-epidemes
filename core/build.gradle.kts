plugins {
	kotlin("jvm")
}

// project classpath
dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val rxJavaVersion: String by project
	val rxKotlinVersion: String by project

	// dependencySets, deprecated terms:  (see https://stackoverflow.com/a/44453461)
	//    `compile` -> `implementation` or `api` (not exposed to dependents, prevents leaking)
	//   `provided` -> `compileOnly`
	//        `apk` -> `runtimeOnly`

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))

	api(group = "ch.qos.logback", name = "logback-classic")
	api(group = "org.slf4j", name = "jul-to-slf4j")
	api(group = "org.slf4j", name = "log4j-over-slf4j")

	// units of measurement (JSR-363)
	api(group = "systems.uom", name = "systems-common-java8", version = "1.0")

	// arbitrary precision floating point calculations (degrees from/to radians, factorials, etc.)
	api(group = "org.apfloat", name = "apfloat", version = "1.9.1")

	// persistence (JPA, multiple JSRs)
	compileOnly(group = "javax.persistence", name = "javax.persistence-api", version = "2.2")

	// managed concurrency (JSR-236)
	api(group = "javax.enterprise.concurrent", name = "javax.enterprise.concurrent-api", version = "1.1")

	// RxKotlin and RxJava, see https://www.baeldung.com/rxkotlin
	api(group = "io.reactivex.rxjava2", name = "rxkotlin", version = rxKotlinVersion)
	api(group = "io.reactivex.rxjava2", name = "rxjava", version = rxJavaVersion)

	// Joda Time (for parsing ISO durations, etc.)
	compileOnly(group = "joda-time", name = "joda-time", version = "2.10.5")

	// Jackson for JSON de/serialization
	api(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jdk8")
	api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")

	// (Spring-converter for using) NIC-based universally unique identifiers (UUID)
	api(group= "com.fasterxml.uuid", name = "java-uuid-generator", version= "3.2.0")
	compileOnly(group = "org.springframework", name = "spring-context")

	// test
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")

}
