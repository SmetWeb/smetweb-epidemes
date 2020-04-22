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
	val h2Version: String by project
	val hibernateVersion: String by project

	api(project(":core"))
	api(project(":model"))

	// persist (JPA)
	api(group = "org.springframework.boot", name = "spring-boot-starter-data-jpa")
	kapt(group = "org.hibernate", name = "hibernate-jpamodelgen", version = hibernateVersion)

	// test
	testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test")
	testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api")
	testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine")
	testRuntimeOnly(group = "com.h2database", name = "h2", version = h2Version)
}
