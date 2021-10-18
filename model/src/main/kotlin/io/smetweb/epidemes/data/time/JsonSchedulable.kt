package io.smetweb.epidemes.data.time

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.smetweb.json.readValue
import io.smetweb.time.TimeRef
import jdk.nashorn.internal.runtime.Timing
import org.ujmp.core.util.JsonUtil
import java.time.Duration
import java.time.ZonedDateTime
import java.util.SortedMap
import java.util.function.Function
import java.util.stream.Collectors

interface JsonSchedulable
{
	/** [SeriesTiming] bean specifies (K,V) update rule configurations  */
	class SeriesTiming {
		@JsonProperty(OCCURRENCE_PROPERTY)
		var occurrence: String? = null

		@JsonProperty(PERIOD_PROPERTY)
		var period: Period? = null

		/** an integer-to-value mapping due to array flattening in YamlUtil  */
		var series: Map<String, SortedMap<Int, JsonNode>>? = null
	}

	fun <K, V> ArrayNode.iterate(
		keyType: Class<K>, valueType: Class<V>
	): Observable<Map<K, V>> =
		Observable.fromIterable(this)
			.flatMap { it.iterate(keyType, valueType) }

	fun <K, V> JsonNode.iterate(
		keyType: Class<K>,
		valueType: Class<V>
	): Observable<Map<K, V>> {
		return if (isArray)
			Observable.fromIterable(this)
				.flatMap { it.iterate(keyType, valueType) }
		else
			iterate(readValue(SeriesTiming::class.java), keyType, valueType)
	}

	fun <K, V> iterate(
		item: SeriesTiming,
		keyType: Class<K>?, valueType: Class<V>?
	): Observable<Map<K, V>> {
		return Observable.create { sub ->
			try {
				val now: TimeRef = now()
				val timing: Iterable<TimeRef> = Timing(item.occurrence)
					.offset(now.toJava8(scheduler().offset()))
					.iterate(now)
				val series = item.series!!.entries
					.parallelStream()
					.collect(
						Collectors.toMap<Any, Any, Any>(
							Function<Any, Any> { (key): Map.Entry<String, SortedMap<Int?, JsonNode?>?> ->
								JsonUtil.valueOf(
									'"' + key + '"',
									keyType
								)
							},
							Function<Any, Any> { (_, value): Map.Entry<String?, SortedMap<Int?, JsonNode?>> ->
								value.values.stream().map(
									Function<JsonNode?, Any> { v: JsonNode? ->
										JsonUtil.valueOf(
											v,
											valueType
										)
									})
									.collect(Collectors.toList())
							})
					)
				atEach(
					timing
				) { t -> scheduleSeries(sub, item.period, series, 0) }
					.subscribe({ exp -> }, sub::onError)
			} catch (e: Exception) {
				sub.onError(e)
			}
		}
	}

	/** helper-method repeatedly (re)schedules until the series are complete  */
	fun <K, V> scheduleSeries(
		sub: ObservableEmitter<Map<K, V>?>,
		period: Period?, series: Map<K, List<V>>, index: Int
	) {
		if (series.isEmpty() || sub.isDisposed)
			return
		val values = series.entries.parallelStream()
			.filter { (_, value): Map.Entry<K, List<V>> -> index < value.size }
			.collect(
				Collectors.toMap(
					{ (key): Map.Entry<K, List<V>> -> key },
					{ (_, value): Map.Entry<K, List<V>> ->
						value[index]
					})
			)

		// continue this series incidence for current index only if non-empty
		if (values.isEmpty())
			return
		sub.onNext(values)
		val now: ZonedDateTime = now().toJava8(scheduler().offset())
		after(
			Duration.between(now, now.plus(period)).toMillis(),
			TimeUnits.MILLIS
		).call { scheduleSeries(sub, period, series, index + 1) }
	}

	companion object {
		const val OCCURRENCE_PROPERTY = "occurrence"
		const val PERIOD_PROPERTY = "period"
	}
}