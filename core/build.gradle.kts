plugins {
	kotlin("jvm")
	kotlin("kapt")
	kotlin("plugin.allopen")
}

// as per https://spring.io/guides/tutorials/spring-boot-kotlin/
allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.Embeddable")
	annotation("jakarta.persistence.MappedSuperclass")
}

// project classpath
dependencies {
	// `project` configuration bean reads values from file `gradle.properties`
	val kotlinVersion: String by System.getProperties()
	val coroutinesVersion: String by project
	val persistenceApiVersion: String by project
	val concurrentApiVersion: String by project
	val ucumVersion: String by project
	val uomLibVersion: String by project
	val math3Version: String by project
	val coltVersion: String by project
	val ujmpVersion: String by project
	val rxJavaVersion: String by project
	val rxKotlinVersion: String by project
	val apFloatVersion: String by project
	val javaUuidGeneratorVersion: String by project
	val jodaTimeVersion: String by project
	val dsolVersion: String by project
	val jeromqVersion: String by project
	val hibernateVersion: String by System.getProperties()
	val h2Version: String by project
	val indriyaVersion: String by project

	api(kotlin("stdlib-jdk8", version = kotlinVersion))
	api(kotlin("reflect", version = kotlinVersion))
	api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-rx3", version = coroutinesVersion)

//	api(group = "ch.qos.logback", name = "logback-classic")
	api(group = "org.apache.logging.log4j", name = "log4j", version = "2.13.3")
	api(group = "org.slf4j", name = "jul-to-slf4j")
//	api(group = "org.slf4j", name = "log4j-over-slf4j")

	// units of measurement (JSR-363, replacing JSR-275)
	api(group = "systems.uom", name = "systems-ucum", version = ucumVersion)
	api(group = "tech.uom.lib", name = "uom-lib-jackson", version = uomLibVersion)

	// units of measurement 2.0 (JSR-385, extending JSR-363 and JSR-275)
	api(group = "tech.units", name = "indriya", version = indriyaVersion)

	// arbitrary-precision floating point calculations (degrees from/to radians, factorials, etc.)
	api(group = "org.apfloat", name = "apfloat", version = apFloatVersion)

	// Universal Java Matrix Package, incorporating dense matrix classes from Apache commons math (2015)
	api(group = "org.ujmp", name = "ujmp-commonsmath", version = ujmpVersion)

	// commons-math3, including RNGs, distributions, algorithms, etc. (2016)
	compileOnly(group = "org.apache.commons", name = "commons-math3", version = math3Version)

	// kafka (2021)
//	compileOnly(group = "org.apache.kafka", name = "kafka-streams", version = kafkaVersion)

	// Efficient Java Matrix Library for linear algebra on real/complex/dense/sparse matrices (2020)
//	compileOnly(group = "org.ejml", name = "ejml-all", version = ejmlVersion)

	// ND4J: Scientific Computing on the JVM (2021)
//	compileOnly(group = "org.nd4j", name = "nd4j-api", version = nd4jVersion)

	// persistence (JPA, multiple JSRs)
	compileOnly(group = "jakarta.persistence", name = "jakarta.persistence-api", version = persistenceApiVersion)

	// managed concurrency (JSR-236)
	api(group = "jakarta.enterprise.concurrent", name = "jakarta.enterprise.concurrent-api", version = concurrentApiVersion)

	// RxKotlin and RxJava, see https://www.baeldung.com/rxkotlin
	api(group = "io.reactivex.rxjava3", name = "rxkotlin", version = rxKotlinVersion)
	api(group = "io.reactivex.rxjava3", name = "rxjava", version = rxJavaVersion)

	// Joda Time (for parsing ISO durations, etc.)
	implementation(group = "joda-time", name = "joda-time", version = jodaTimeVersion)
	// RFC 5545 and 2445 (iCal) recurrence rule (RRULE) - https://mvnrepository.com/artifact/org.dmfs/lib-recur
	implementation(group = "org.dmfs", name = "lib-recur", version = "0.12.2")
	// Quartz implementation of CRON-like scheduler https://mvnrepository.com/artifact/org.quartz-scheduler/quartz
	implementation(group = "org.quartz-scheduler", name = "quartz", version = "2.3.2")

	// Jackson for JSON de/serialization
	implementation(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jdk8")
	implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin")

	// (Spring-converter for using) NIC-based universally unique identifiers (UUID)
	implementation(group= "com.fasterxml.uuid", name = "java-uuid-generator", version= javaUuidGeneratorVersion)

	// D-SOL simulator, see https://simulation.tudelft.nl
	compileOnly(group = "dsol", name = "dsol-core", version = dsolVersion)
	compileOnly(group = "dsol", name = "dsol-zmq", version = dsolVersion)
	compileOnly(group = "org.zeromq", name = "jeromq", version = jeromqVersion)
//	runtimeOnly(group = "dsol", name = "dsol-web", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-demo", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-build-tools", version = dsolVersion)

	// Jason agentspeak, see https://github.com/jason-lang/jason (latest not yet in mvn repo)
	compileOnly(group = "net.sf.jason", name = "jason", version = "2.3")

//	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	compileOnly(group = "org.springframework.boot", name = "spring-boot") // only requiring @ConfigurationProperties
	compileOnly(group = "org.springframework.data", name = "spring-data-jpa")
	kapt(group = "org.hibernate.orm", name = "hibernate-jpamodelgen", version = hibernateVersion)
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

	// used in XML binding XJC plugin-enabled OmitNullsToStringStyle
	api("org.apache.commons:commons-lang3:3.12.0")

	// test
	testImplementation(group = "dsol", name = "dsol-core", version = dsolVersion)
	testImplementation(group = "org.zeromq", name = "jeromq", version = jeromqVersion)
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-log4j2")
	testImplementation(platform("org.apache.logging.log4j:log4j-bom:2.17.1"))
	testImplementation(group = "com.fasterxml.jackson.core", name = "jackson-databind") // required by log4j2
	testImplementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml") // required by log4j2
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test") {
		exclude(module = "junit")
		exclude(group = "org.junit.vintage")
		exclude(group = "ch.qos.logback", module = "logback-classic")
	}
	testImplementation(group = "com.h2database", name = "h2", version = h2Version)
	testImplementation(group = "org.awaitility", name = "awaitility-kotlin", version = "4.0.2")

	// CERN's prng
	testImplementation(group = "colt", name = "colt", version = coltVersion)

	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")

}
