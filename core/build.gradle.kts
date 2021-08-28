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
	val kotlinxVersion: String by project
	val persistenceApiVersion: String by project
	val concurrentApiVersion: String by project
//	val kafkaVersion: String by project
	val ucumVersion: String by project
	val uomLibVersion: String by project
	val math3Version: String by project
	val coltVersion: String by project
	val ujmpVersion: String by project
//	val ejmlVersion: String by project
//	val nd4jVersion: String by project
	val rxJavaVersion: String by project
	val rxKotlinVersion: String by project
	val apFloatVersion: String by project
	val javaUuidGeneratorVersion: String by project
	val jodaTimeVersion: String by project
	val dsolVersion: String by project
	val jeromqVersion: String by project
	val hibernateVersion: String by project
	val h2Version: String by project
	val indriyaVersion: String by project

	api(kotlin("stdlib-jdk8"))
	api(kotlin("reflect"))
	api(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-rx3", version = kotlinxVersion)

//	api(group = "ch.qos.logback", name = "logback-classic")
	api(group = "org.apache.logging.log4j", name = "log4j", version = "2.13.3")
	api(group = "org.slf4j", name = "jul-to-slf4j")
//	api(group = "org.slf4j", name = "log4j-over-slf4j")

	// units of measurement (JSR-363, replacing JSR-275)
	api(group = "systems.uom", name = "systems-ucum", version = ucumVersion)
	api(group = "tech.uom.lib", name = "uom-lib-jackson", version = uomLibVersion)

	// units of measurement 2.0 (JSR-385, extending JSR-363)
	api(group = "tech.units", name = "indriya", version = indriyaVersion)

	// arbitrary-precision floating point calculations (degrees from/to radians, factorials, etc.)
	api(group = "org.apfloat", name = "apfloat", version = apFloatVersion)

	// Universal Java MAtrix Package, incorporating dense matrix classes from Apache commons math (2015)
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
	api(group = "dsol", name = "dsol-zmq", version = dsolVersion)
	api(group = "org.zeromq", name = "jeromq", version = jeromqVersion)
//	runtimeOnly(group = "dsol", name = "dsol-web", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-demo", version = dsolVersion)
//	runtimeOnly(group = "dsol", name = "dsol-build-tools", version = dsolVersion)

	// Jason agentspeak, see https://github.com/jason-lang/jason (latest not yet in mvn repo)
	compileOnly(group = "net.sf.jason", name = "jason", version = "2.3")

//	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	compileOnly(group = "org.springframework.boot", name = "spring-boot") // only requiring @ConfigurationProperties
	api(group = "org.springframework.data", name = "spring-data-jpa")
	kapt(group = "org.hibernate", name = "hibernate-jpamodelgen", version = hibernateVersion)
	kapt(group = "org.springframework.boot", name = "spring-boot-configuration-processor")

	// test
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-log4j2")
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
