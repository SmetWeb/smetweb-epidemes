repositories {
    mavenCentral()
}

plugins {
	kotlin("jvm")
	// see https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlin-jupyter-api
	kotlin("jupyter.api") version "0.10.3-29"
}

dependencies {
    implementation "com.github.holgerbrandl:krangl:0.17"
}
