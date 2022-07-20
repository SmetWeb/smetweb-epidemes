package io.smetweb.xml

import io.reactivex.rxjava3.core.Observable
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval
import org.joda.time.Period
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

/**
 * See also XSLT streaming transformation (STX), e.g. with
 * [SAXON HE](http://www.saxonica.com/)
 *
 * `<dependency>
 * <groupId>net.sf.saxon</groupId>
 * <artifactId>Saxon-HE</artifactId>
 * <version>9.5.1-5</version>
 * </dependency> `
 *
 * TODO allow wild-cards, caseor xPath 1, 2, etc. ?
 *
 * @param inputFactory the [XMLInputFactory] for creating an [XMLStreamReader] from this [InputStream]
 * @param targetPath the element path to match, or `null` for all root elements
 * @return Observable
 */
fun InputStream.matchElementPath(
	inputFactory: XMLInputFactory = XMLInputFactory.newInstance(),
	targetPath: List<String>? = null,
): Observable<XMLStreamReader> =
	Observable.using({ inputFactory.createXMLStreamReader(this) }, { xmlReader ->
		Observable.create { sub ->
			try {
				val currentPath: Deque<String> = LinkedList()
				var lastErrorOffset = Int.MIN_VALUE
				var lastErrorMessage: String? = null
				var match = targetPath.isNullOrEmpty()
				while (xmlReader.hasNext()) {
					try {
						when (StAXEventType.valueOf(xmlReader.next())) {
							StAXEventType.START_ELEMENT -> {
								currentPath.offerLast(xmlReader.name.localPart)
								match = match || currentPath == targetPath
//							LOG.trace("+{}:{} --> {} -> {}", xmlReader.name.prefix, xmlReader.name.localPart, path, match);
								if (match)
									sub.onNext(xmlReader)
							}
							StAXEventType.END_ELEMENT -> {
								currentPath.pollLast()
								match = targetPath.isNullOrEmpty() || (match && currentPath.size >= targetPath.size)
							}
							else -> {
								// empty
							}
						}
					} catch (e: XMLStreamException) {
						if (e.nestedException is IOException) {
							sub.onError(e.nestedException)
							break
						}
						val offset = xmlReader.location.characterOffset
						if (offset == lastErrorOffset && e.message == lastErrorMessage) {
//							LOG.error("End XML parsing: repeating error", e.getNestedException());
							sub.onError(e.nestedException)
							break // stop when I/O error or same error twice
						}
//						LOG.warn("Ignoring error at offset: {}", offset, e.nestedException)
						lastErrorMessage = e.message
						lastErrorOffset = offset
					}
				}
			} catch (e: Throwable) {
				sub.onError(e)
			}
		}
	}, XMLStreamReader::close)

/**
 * @return a UTC [Date] from this JAXP [XMLGregorianCalendar]
 */
fun XMLGregorianCalendar.toDate(): Date =
	toDateTime().toDate()

/**
 * @return a Joda [DateTime] from this JAXP [XMLGregorianCalendar]
 */
fun XMLGregorianCalendar.toDateTime(
	timeZone: DateTimeZone = when (timezone) {
		DatatypeConstants.FIELD_UNDEFINED -> DateTimeZone.UTC
		else -> DateTimeZone.forOffsetMillis(timezone * 60 * 1000)
	}
): DateTime =
	DateTime(year, month, day, hour, minute, second, millisecond, timeZone)

/**
 * @return a JSR-310 [java.time.Duration] from this JAXP [Duration]
 */
fun Duration.toDuration(
	start: Instant = Instant.now(),
): java.time.Duration =
	getTimeInMillis(Date.from(start)).let { millis ->
		val seconds = millis / 1000L
		val nanos = 1_000_000L * millis % 1000
		java.time.Duration.ofSeconds(seconds, nanos)
	}

/**
 * @return a JSR-310 [ZonedDateTime] from this JAXP [XMLGregorianCalendar]
 */
fun XMLGregorianCalendar.toZonedDateTime(
	nanoOfSecond: Int = 1_000_000 * millisecond,
	timeZone: ZoneOffset = when (timezone) {
		DatatypeConstants.FIELD_UNDEFINED -> ZoneOffset.UTC
		else -> ZoneOffset.ofTotalSeconds(timezone * 60)
	}
): ZonedDateTime =
	ZonedDateTime.of(year, month, day, hour, minute, second, nanoOfSecond, timeZone)

/**
 * @param offset a JAXP [XMLGregorianCalendar]
 * @return a [org.joda.time.Duration] from this JAXP [Duration]
 */
fun Duration.toJoda(offset: XMLGregorianCalendar): org.joda.time.Duration =
	toJoda(offset).toDuration()

/**
 * @param offset a JAXP [XMLGregorianCalendar]
 * @return an [Interval] from this JAXP [Duration]
 */
fun Duration.toInterval(offset: XMLGregorianCalendar): Interval =
	toInterval(offset.toDateTime())

/**
 * @param offset
 * @return an [Interval] from this JAXP [Duration]
 */
fun Duration.toInterval(offset: DateTime): Interval =
	Interval(offset, offset.plus(getTimeInMillis(offset.toDate())))

/**
 * @return the number of millis since UTC epoch, as [Long]
 */
fun Duration.toMillis(): Long =
	getTimeInMillis(Date(0))

/**
 * @return a JAXP [XMLGregorianCalendar] from this JSR-310 [ZonedDateTime]
 */
fun ZonedDateTime.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): XMLGregorianCalendar =
	datatypeFactory.newXMLGregorianCalendar(
		year.toBigInteger(), monthValue, dayOfMonth, hour, minute, second,
		if (nano == 0) BigDecimal.ZERO else BigDecimal.valueOf(nano.toLong(), 9),
		TimeZone.getTimeZone(zone).rawOffset / 60000
	)

/**
 * @return a JAXP [XMLGregorianCalendar] from this [Calendar]
 */
fun Calendar.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): XMLGregorianCalendar =
	DateTime(this).toXML(datatypeFactory)

/**
 * @return a JAXP [XMLGregorianCalendar] from this [Date]
 */
fun Date.toXML(
	timeZone: DateTimeZone = DateTimeZone.getDefault(),
	datatypeFactory: DatatypeFactory = DATATYPE_FACTORY
): XMLGregorianCalendar =
	DateTime(this, timeZone).toXML(datatypeFactory)

/**
 * @return a JAXP [XMLGregorianCalendar] from this [DateTime]
 */
fun DateTime.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): XMLGregorianCalendar =
	datatypeFactory.newXMLGregorianCalendar().also {
		it.year = year
		it.month = monthOfYear
		it.day = dayOfMonth
		it.setTime(hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond)
		it.timezone = zone.toTimeZone().rawOffset / 1000 / 60
		// it.setTimezone(DatatypeConstants.FIELD_UNDEFINED)
	}

/**
 * @return a JAXP [Duration] from this [Interval]
 */
fun Interval.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): Duration =
	toPeriod().toXML(datatypeFactory)

/**
 * @return a JAXP [Duration] from thie [Period]
 */
fun Period.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): Duration =
	datatypeFactory.newDuration(true, years, months, days, hours, minutes, seconds)

private val DATATYPE_FACTORY = DatatypeFactory.newDefaultInstance()

/**
 * @return a JAXP [Duration] from this [org.joda.time.Duration]
 */
fun org.joda.time.Duration.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): Duration =
	millis.toXML(datatypeFactory)

/**
 * @return a JAXP [Duration] from this [Long] (representing millis since the UTC epoch)
 */
fun Long.toXML(datatypeFactory: DatatypeFactory = DATATYPE_FACTORY): Duration =
	datatypeFactory.newDuration(this)
