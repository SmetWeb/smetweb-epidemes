package io.smetweb.fact

import com.fasterxml.jackson.annotation.JsonIgnore
import io.smetweb.ref.IdRef

interface FactLink {

	fun getFact(): CoordinationFact

	fun getLinkRef(): IdRef<*, *>

	fun getLinkResultKind(): ResultKind

	@JsonIgnore
	fun getLink(): CoordinationFact?

	companion object {

		@JvmStatic
		fun of(fact: CoordinationFact, link: CoordinationFact) =
				object: FactLink {
					override fun getFact(): CoordinationFact = fact
					override fun getLink(): CoordinationFact? = link
					override fun getLinkRef(): IdRef<*, *> = link.getId()
					override fun getLinkResultKind(): ResultKind = link.getExchange().getResultKind()
				}

		@JvmStatic
		fun of(fact: CoordinationFact, linkRef: IdRef<*, *>, linkResultKind: ResultKind,
               linkFetcher: () -> CoordinationFact? = { null }) =
				object: FactLink {
					override fun getFact(): CoordinationFact = fact
					override fun getLink() = linkFetcher()
					override fun getLinkRef(): IdRef<*, *> = linkRef
					override fun getLinkResultKind(): ResultKind = linkResultKind
				}
	}
}