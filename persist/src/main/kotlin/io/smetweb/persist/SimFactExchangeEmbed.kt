package io.smetweb.persist

import io.smetweb.domain.ontology.ResultKind
import io.smetweb.sim.SimFactExchange
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidRef
import java.util.UUID
import javax.persistence.*

/**
 * [SimFactExchangeEmbed] is a data access object for persisting [SimFactExchange] objects,
 * with respective JPA attributes specified in the generated [SimFactExchangeEmbed_] meta-model.
 */
@Embeddable
data class SimFactExchangeEmbed (
		@Basic
		@Column(name = CTX_REF_COLUMN_NAME, nullable = false, updatable = false,
				// unique = true, -- NOT UNIQUE id when embedded in fact table, however : unique ordinal + id
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var id: UUID? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "RESULT_KIND_FK", updatable = false)
		var resultKind: ResultKindEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "EXECUTOR_FK", updatable = false)
		var executorRef: UuidNameRefEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "INITIATOR_FK", updatable = false)
		var initiatorRef: UuidNameRefEntity? = executorRef

) {

	constructor(
			exchange: SimFactExchange,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },
			resultKindFetcher: (ResultKind) -> ResultKindEntity = { ResultKindEntity(resultKind = it) },

			// fetched attribute values
			resultKindEntity: ResultKindEntity = resultKindFetcher(exchange.getResultKind()),
			executorRefEntity: UuidNameRefEntity = nameRefFetcher(exchange.getExecutorRef() as UuidNameRef),
			initiatorRefEntity: UuidNameRefEntity = when(exchange.isSelfInitiated()) {
				true -> executorRefEntity
				else -> nameRefFetcher(exchange.getInitiatorRef() as UuidNameRef)
			}
	): this(
			id = exchange.getId().get(),
			resultKind = resultKindEntity,
			executorRef = executorRefEntity,
			initiatorRef = initiatorRefEntity)

	fun toSimFactContext(executorRef: UuidNameRef = this.executorRef!!.toUuidNameRef()) =
			SimFactExchange(
					id = UuidRef(id!!),
					resultKind = resultKind!!.toResultKind(),
					executorRef = executorRef,
					initiatorRef = if (this.executorRef == this.initiatorRef)
						executorRef
					else
						initiatorRef!!.toUuidNameRef())

	/** for [Table] column constraint specification */
	companion object {
		const val CTX_REF_COLUMN_NAME = "CTX_REF"
	}
}
