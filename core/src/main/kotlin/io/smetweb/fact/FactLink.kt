package io.smetweb.fact

import com.fasterxml.jackson.annotation.JsonIgnore
import io.smetweb.ref.IdRef

interface FactLink {

	val fact: CoordinationFact

	val linkRef: IdRef<*, *>

	val linkResultKind: ResultKind

	val link: CoordinationFact?
		@JsonIgnore
		get() = null

	companion object {

		@JvmStatic
		fun of(fact: CoordinationFact, link: CoordinationFact) =
				object: FactLink {
					override val fact: CoordinationFact = fact
					override val link: CoordinationFact? = link
					override val linkRef: IdRef<*, *> = link.id
					override val linkResultKind: ResultKind = link.exchange.resultKind
				}

		@JvmStatic
		fun of(fact: CoordinationFact, linkRef: IdRef<*, *>, linkResultKind: ResultKind,
			   linkFetcher: () -> CoordinationFact? = { null }) =
				object: FactLink {
					override val fact: CoordinationFact = fact
					override val link = linkFetcher()
					override val linkRef: IdRef<*, *> = linkRef
					override val linkResultKind: ResultKind = linkResultKind
				}
	}
}