package io.smetweb.sim.event

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.smetweb.ref.IdRef
import io.smetweb.ref.IdRef.IntRef
import io.smetweb.ref.Ref
import io.smetweb.fact.*
import io.smetweb.json.TreeNodeJpaConverter
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.json.OBJECT_MAPPER
import io.smetweb.sim.dsol.DsolTimeRef
import io.smetweb.time.TimeRefEmbedded
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidNameRefEntity
import io.smetweb.uuid.UuidRef
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * [UuidFactEntity] is a representation for persisting and accessing [UuidFact] objects,
 * with respective JPA attributes specified in a generated meta-model: [SimFactEventEntity_].
 *
 * This representative object suits [UuidFact] that have:
 * - [UUID]-based identifiers (for [id], [UuidResultExchangeEmbedded.id],
 *   [UuidResultExchangeEmbedded.executorRef], [UuidResultExchangeEmbedded.initiatorRef]);
 * - JSON convertible [details] (key, value)-entries (i.e. Jackson [TreeNode]-compatible); and
 * - unique [ordinal] values per [exchange], as facts follow a certain sequence and, because of their
 *   inter-subjective nature, facts of one context can't be ordered differently for the same participants (really?)
 */
@Entity
@Table(name = "FACTS", uniqueConstraints = [
	UniqueConstraint(columnNames = [
		UuidResultExchangeEmbedded.CTX_REF_COLUMN_NAME,
		UuidFactEntity.CTX_ORD_COLUMN_NAME] ) ] )
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Suppress("JpaDataSourceORMInspection")
data class UuidFactEntity(

		/** [pk] is meant for database relations only, value has no bearing on [id] attribute */
		@Id
		@GeneratedValue(
				// GenerationType.IDENTITY strategy unsupported in Neo4J
				strategy = GenerationType.AUTO)
		@Column(name = "PK", nullable = false, updatable = false)
		protected var pk: Int? = null,

		/** [created] is meant for database auditing only, temporal value has no relation to [occur] attribute */
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "CREATED_TS", insertable = false, updatable = false,
				columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
		protected var created: Date? = null,

		@Basic
		@Column(name = "ID", nullable = false, updatable = false, unique = true,
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var id: UUID? = null,

		@Basic
		@Column(name = "KIND", nullable = false, updatable = false)
		var kind: FactKind? = null,

		@Embedded
		var exchange: UuidResultExchangeEmbedded? = null,

		@Basic
		@Column(name = CTX_ORD_COLUMN_NAME, updatable = false, nullable = false)
		var ordinal: Int? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "CREATOR_FK", updatable = false)
		var creatorRef: UuidNameRefEntity? = null,

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "RESPONDER_FK", updatable = false)
		var responderRef: UuidNameRefEntity? = null,

		@OneToMany(cascade = [CascadeType.PERSIST], targetEntity = UuidFactLinkEntity::class)
		var linkRefs: Collection<UuidFactLinkEntity> = emptySet(),

		@Embedded
		var occur: TimeRefEmbedded? = null,

		@Basic
		@Column(name = "DETAILS", nullable = true, updatable = false)
		@Convert(converter = TreeNodeJpaConverter::class)
		var details: TreeNode? = null

) {

	constructor(
			fact: UuidFact,
			epoch: Instant = Instant.EPOCH,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },
			resultKindFetcher: (ResultKind) -> UuidResultKindEntity = { UuidResultKindEntity(resultKind = it) },
			factLinkFetcher: (FactLink) -> UuidFactLinkEntity = { UuidFactLinkEntity(factLink = it) },

			// fetched attribute values
			contextEmbedded: UuidResultExchangeEmbedded = UuidResultExchangeEmbedded(
					exchange = fact.exchange,
					resultKindFetcher = resultKindFetcher,
					nameRefFetcher = nameRefFetcher),
			creatorRefEntity: UuidNameRefEntity = if(fact.creatorRef() == fact.exchange.initiatorRef)
				contextEmbedded.initiatorRef!!
			else
				contextEmbedded.executorRef!!,
			responderRefEntity: UuidNameRefEntity = if(fact.responderRef() == fact.exchange.initiatorRef)
				contextEmbedded.initiatorRef!!
			else
				contextEmbedded.executorRef!!,
			links: Collection<UuidFactLinkEntity> = fact.links.map(factLinkFetcher),
			occur: TimeRefEmbedded = TimeRefEmbedded(fact.occur, epoch),
			details: TreeNode? = OBJECT_MAPPER.valueToTree(fact.details)
	): this(
			id = fact.id.get(),
			kind = fact.kind,
			exchange = contextEmbedded,
			ordinal = fact.ordinal.get(),
			creatorRef = creatorRefEntity,
			responderRef = responderRefEntity,
			linkRefs = links,
			occur = occur,
			details = details)

	fun toSimFactEvent(): UuidFact = UuidFact(
            id = UuidRef(this.id!!),
            kind = this.kind!!,
            exchange = this.exchange!!.toSimFactContext(),
            ordinal = this.ordinal!!.let(::IntRef),
            occur = this.occur!!.exact!!.let(::DsolTimeRef),
            links = this.linkRefs.map(UuidFactLinkEntity::toFactLink),
            details = this.details
                    ?.let { OBJECT_MAPPER.treeToValue<Map<in IdRef<*, *>, Ref<*>>>(it) }
                    ?: emptyMap())

	/** for [Table] column constraint specification */
	companion object {
		const val CTX_ORD_COLUMN_NAME = "ORD"
	}
}