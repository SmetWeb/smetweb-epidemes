package io.smetweb.sim.event

import io.smetweb.fact.ResultKind
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidNameRefEntity
import io.smetweb.uuid.UuidRef
import java.util.UUID
import javax.persistence.*

/**
 * [UuidResultExchangeEmbedded] is a data access object for persisting [UuidResultExchange] objects,
 * with respective JPA attributes specified in the generated [UuidResultExchangeEmbedded_] meta-model.
 */
@Embeddable
data class UuidResultExchangeEmbedded (
		@Basic
		@Column(name = CTX_REF_COLUMN_NAME, nullable = false, updatable = false,
				// unique = true, -- NOT UNIQUE id when embedded in fact table, however : unique ordinal + id
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var id: UUID? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "RESULT_KIND_FK", updatable = false)
		var resultKind: UuidResultKindEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "EXECUTOR_FK", updatable = false)
		var executorRef: UuidNameRefEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "INITIATOR_FK", updatable = false)
		var initiatorRef: UuidNameRefEntity? = executorRef

) {

	constructor(
			exchange: UuidResultExchange,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },
			resultKindFetcher: (ResultKind) -> UuidResultKindEntity = { UuidResultKindEntity(resultKind = it) },

			// fetched attribute values
			resultKindEntity: UuidResultKindEntity = resultKindFetcher(exchange.resultKind),
			executorRefEntity: UuidNameRefEntity = nameRefFetcher(exchange.executorRef),
			initiatorRefEntity: UuidNameRefEntity = when(exchange.selfInitiated()) {
				true -> executorRefEntity
				else -> nameRefFetcher(exchange.initiatorRef)
			}
	): this(
			id = exchange.id.get(),
			resultKind = resultKindEntity,
			executorRef = executorRefEntity,
			initiatorRef = initiatorRefEntity)

	fun toSimFactContext(executorRef: UuidNameRef = this.executorRef!!.toUuidNameRef()) =
			UuidResultExchange(
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
