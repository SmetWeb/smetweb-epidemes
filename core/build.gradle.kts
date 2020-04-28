plugins {
	kotlin("jvm")
	kotlin("kapt")
	kotlin("plugin.allopen")
}

// as per https://spring.io/guides/tutorials/spring-boot-kotlin/
allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.Embeddable")
	annotation("javax.persistence.MappedSuperclass")
}

// project classpath
dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val persistenceApiVersion: String by project
	val concurrentApiVersion: String by project
	val uomVersion: String by project
	val rxJavaVersion: String by project
	val rxKotlinVersion: String by project
	val apFloatVersion: String by project
	val javaUuidGeneratorVersion: String by project
	val jodaTimeVersion: String by project
	val dsolVersion: String by project
	val hibernateVersion: String by project
	val h2Version: String by project

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))

	api(group = "ch.qos.logback", name = "logback-classic")
	api(group = "org.slf4j", name = "jul-to-slf4j")
	api(group = "org.slf4j", name = "log4j-over-slf4j")

	// units of measurement (JSR-363)
	api(group = "systems.uom", name = "systems-common-java8", version = uomVersion)

	// arbitrary precision floating point calculations (degrees from/to radians, factorials, etc.)
	api(group = "org.apfloat", name = "apfloat", version = apFloatVersion)

	// persistence (JPA, multiple JSRs)
	compileOnly(group = "javax.persistence", name = "javax.persistence-api", version = persistenceApiVersion)

	// managed concurrency (JSR-236)
	api(group = "javax.enterprise.concurrent", name = "javax.enterprise.concurrent-api", version = concurrentApiVersion)

	// RxKotlin and RxJava, see https://www.baeldung.com/rxkotlin
	api(group = "io.reactivex.rxjava3", name = "rxkotlin", version = rxKotlinVersion)
	api(group = "io.reactivex.rxjava3", name = "rxjava", version = rxJavaVersion)

	// Joda Time (for parsing ISO durations, etc.)
	api(group = "joda-time", name = "joda-time", version = jodaTimeVersion)

	// Jackson for JSON de/serialization
	api(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jdk8")
	api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")

	// (Spring-converter for using) NIC-based universally unique identifiers (UUID)
	api(group= "com.fasterxml.uuid", name = "java-uuid-generator", version= javaUuidGeneratorVersion)

	// D-SOL simulator, see https://simulation.tudelft.nl
	api(group = "dsol", name = "dsol-core", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-web", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-demo", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-zmq", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-build-tools", version = dsolVersion)

	// Jason agentspeak, see https://github.com/jason-lang/jason (latest as of 2020 is v2.4 not in mvn repo)
//	api(group = "net.sf.jason", name = "jason", version = "2.3")

//	compileOnly(group = "org.springframework", name = "spring-context")
	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	kapt(group = "org.hibernate", name = "hibernate-jpamodelgen", version = hibernateVersion)
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

	// test
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test") {
		exclude(module = "junit")
		exclude(group = "org.junit.vintage")
	}
	testImplementation("com.h2database:h2:$h2Version")
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")

}
