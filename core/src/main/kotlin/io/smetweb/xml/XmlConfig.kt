package io.smetweb.xml

import io.reactivex.rxjava3.core.Observable
import io.smetweb.log.getLogger
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval
import org.joda.time.Period
import org.slf4j.Logger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.context.properties.bind.Name
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

@ConstructorBinding
@ConfigurationProperties
data class XmlConfig(

	/**
	 * @return the JAXP implementation type of [DatatypeFactory]
	 * @see DatatypeFactory#DATATYPEFACTORY_IMPLEMENTATION_CLASS
	 */
	@Name(DatatypeFactory.DATATYPEFACTORY_PROPERTY)
	@DefaultValue("com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl")
	val datatypeFactoryType: Class<out DatatypeFactory>,

	@DefaultValue("" + true)
	val isNamespaceAware: Boolean,

//	@Name("javax.xml.stream.XMLInputFactory") // XMLInputFactory::class.java.name
//	@DefaultValue( "com.ctc.wstx.stax.WstxInputFactory" )
//	@DefaultValue( "com.sun.xml.internal.stream.XMLInputFactoryImpl" )
//	val xmlInputFactoryImpl: Class<out XMLInputFactory>,

	@Name(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)
	@DefaultValue("" + false)
	val isReplacingEntityReferences: Boolean,

	@Name(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)
	@DefaultValue("" + false)
	val isSupportingExternalEntities: Boolean,

	@Name(XMLInputFactory.IS_COALESCING)
	@DefaultValue("" + false)
	val isCoalescing: Boolean,

	) {

	private val log: Logger = getLogger()

	/**
	 * a cached StAX [XMLInputFactory]
	 * @see javax.xml.stream.XMLInputFactory.newFactory
	 */
	// TODO use @Bean?
	private val inputFactory: XMLInputFactory by lazy {
		val result = XMLInputFactory.newInstance()
		log.trace("Using StAX {} implementation: {}", XMLInputFactory::class.java.name, result.javaClass.name)
		result.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, isReplacingEntityReferences)
		result.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, isSupportingExternalEntities)
		result.setProperty(XMLInputFactory.IS_COALESCING, isCoalescing)
		result
	}

	/** a cached JAXP [DatatypeFactory] */
	// TODO use @Bean?
	private val datatypeFactory: DatatypeFactory by lazy {
//		DatatypeFactory.newDefaultInstance()
		DatatypeFactory.newInstance(datatypeFactoryType.name, Thread.currentThread().contextClassLoader)
	}

	/** a cached JAXP [DocumentBuilderFactory] */
	// TODO use @Bean?
	private val domFactory: DocumentBuilderFactory by lazy {
		val result = DocumentBuilderFactory.newInstance()
		result.isNamespaceAware = isNamespaceAware
		result
	}

	/**
	 * @return a new (JAXP) [DocumentBuilderFactory] instance
	 * @see javax.xml.parsers.DocumentBuilderFactory#newInstance()
	 */
	// TODO use @Bean?
	fun newDocumentBuilderFactory(): DocumentBuilderFactory {
		val result: DocumentBuilderFactory = DocumentBuilderFactory.newInstance();
		result.isNamespaceAware = isNamespaceAware;
		return result;
	}


	/**
	 * @param is the XML [InputStream]
	 * @param elemPath the element path to match
	 * @return a [Single] observable [XMLStreamReader]
	 */
	fun matchElementPath(stream: InputStream, elemPath: List<String>? = null): Observable<XMLStreamReader> =
		stream.matchElementPath(inputFactory, elemPath)

	/**
	 * @param is the XML [InputStream]
	 * @param elemPath the element path to match, or `null`
	 */
	fun matchElementPath(stream: InputStream, vararg elemPath: String): Observable<XMLStreamReader> =
		stream.matchElementPath(inputFactory, elemPath.asList())

	/**
	 * @param dt a JSR-310 [ZonedDateTime]
	 * @return a JAXP [XMLGregorianCalendar]
	 */
	fun toXML(dt: ZonedDateTime): XMLGregorianCalendar =
		datatypeFactory.newXMLGregorianCalendar(
			BigInteger.valueOf(dt.year.toLong()), dt.monthValue,
			dt.dayOfMonth, dt.hour, dt.minute,
			dt.second, if (dt.nano == 0) BigDecimal.ZERO else BigDecimal.valueOf(dt.nano.toLong(), 9),
			TimeZone.getTimeZone(dt.zone).rawOffset / 60000
		)

	/**
	 * @param calendar
	 * @return a JAXP [XMLGregorianCalendar]
	 */
	fun toXML(calendar: Calendar): XMLGregorianCalendar =
		toXML(DateTime(calendar))

	/**
	 * @param dateUtc
	 * @return a JAXP [XMLGregorianCalendar]
	 */
	fun toXML(dateUtc: Date, timeZone: DateTimeZone = DateTimeZone.getDefault()): XMLGregorianCalendar =
		toXML(DateTime(dateUtc, timeZone))

	/**
	 * @param date
	 * @return a JAXP [XMLGregorianCalendar]
	 */
	fun toXML(date: DateTime): XMLGregorianCalendar {
		val result: XMLGregorianCalendar = datatypeFactory.newXMLGregorianCalendar()
		result.year = date.year
		result.month = date.monthOfYear
		result.day = date.dayOfMonth
		result.setTime(
			date.hourOfDay, date.minuteOfHour,
			date.secondOfMinute, date.millisOfSecond
		)
		result.timezone = date.zone.toTimeZone().rawOffset / 1000 / 60
		// result.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		return result
	}

	/**
	 * @param interval
	 * @return a JAXP [Duration]
	 */
	fun toXML(interval: Interval): Duration =
		toXML(interval.toPeriod())

	/**
	 * @param period
	 * @return a JAXP [Duration]
	 */
	fun toXML(period: Period): Duration =
		datatypeFactory.newDuration(
			true, period.years,
			period.months, period.days, period.hours,
			period.minutes, period.seconds
		)

	/**
	 * @param duration the [org.joda.time.Duration] to convert
	 * @return a JAXP [Duration]
	 */
	fun toXML(duration: org.joda.time.Duration): Duration =
		toXML(duration.millis)

	/**
	 * @param millis
	 * @return a JAXP [Duration]
	 */
	fun toXML(millis: Long): Duration =
		datatypeFactory.newDuration(millis)

}