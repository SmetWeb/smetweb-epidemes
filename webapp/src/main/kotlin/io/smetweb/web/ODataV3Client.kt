//package io.smetweb.web
//
//import io.smetweb.log.getLogger
//import org.springframework.boot.Banner
//import org.springframework.boot.CommandLineRunner
//import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.boot.runApplication
//import org.springframework.context.annotation.Bean
//import java.io.InputStream
//import java.net.URL
//
//
//// TODO create custom OData V3 client for CBS data sources, since Apache Olingo only supports V2 or V4
//
//@SpringBootApplication(scanBasePackages = ["io.smetweb"])
////@EntityScan(basePackages = ["io.smetweb.uuid", "io.smetweb.time", "io.smetweb.sim.event"])
////@EnableScheduling
////@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
////@EnableConfigurationProperties(ScenarioConfig::class, DemeConfig::class)
//class ODataV3Client {
//
//	private val log = getLogger()
//
//	private val edmOF = com.microsoft.schemas.ado._2009._11.edm.ObjectFactory()
//	private val appOF = org.w3._2007.app.ObjectFactory()
//
//	@Bean
//	fun odata4jRunner(): CommandLineRunner = CommandLineRunner { args ->
//		log.info("Started. HAHAHAH args: {}", args)
//		log.info("{}", appOF.createService(appOF.createAppServiceType()))
//		log.info("{}", edmOF.createEntityContainer().withName("myEntityContainer"))
//
//		val serviceUrl = "https://opendata.cbs.nl/ODataApi/odata/37230ned"
//		val input: InputStream = URL(serviceUrl).openStream()
//
//
////		val xmlInputFactory = XMLInputFactory.newInstance()
////		val reader: XMLEventReader = xmlInputFactory.createXMLEventReader(FileInputStream(path))
////		while (reader.hasNext()) {
////			var nextEvent = reader.nextEvent()
////			if (nextEvent.isStartElement) {
////				val startElement = nextEvent.asStartElement()
////				when (startElement.name.localPart) {
////					"website" -> {
////						website = WebSite()
////						val url: Attribute? = startElement.getAttributeByName(QName("url"))
////						if (url != null) {
////							website.setUrl(url.getValue())
////						}
////					}
////					"name" -> {
////						nextEvent = reader.nextEvent()
////						website.setName(nextEvent.asCharacters().data)
////					}
////					"category" -> {
////						nextEvent = reader.nextEvent()
////						website.setCategory(nextEvent.asCharacters().data)
////					}
////					"status" -> {
////						nextEvent = reader.nextEvent()
////						website.setStatus(nextEvent.asCharacters().data)
////					}
////				}
////			}
////			if (nextEvent.isEndElement) {
////				val endElement = nextEvent.asEndElement()
////				if (endElement.name.localPart == "website") {
////					websites.add(website)
////				}
////			}
////		}
//		// list category names
////		consumer.getEntities("Categories").execute().forEach {
////			val categoryName = it.getProperty("Name", String::class.java).value
////			log.info("Category name: $categoryName")
////		}
//	}
//
//	companion object {
//
//		@JvmStatic
//		fun main(args: Array<String>) {
//			runApplication<ODataV3Client>(*args) {
//				setBannerMode(Banner.Mode.OFF)
//			}
//		}
//	}
//
//}