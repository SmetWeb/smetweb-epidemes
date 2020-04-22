package io.smetweb.persist

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.smetweb.domain.IdRef
import io.smetweb.domain.IdRef.IntRef
import io.smetweb.domain.Ref
import io.smetweb.domain.ontology.*
import io.smetweb.sim.SimFactEvent
import io.smetweb.json.OBJECT_MAPPER
import io.smetweb.json.TreeNodeJpaConverter
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.sim.dsol.DsolTimeRef
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidRef
import java.time.Instant
import java.util.*
import javax.persistence.*

/**
 * [SimFactEventEntity] is a representation for persisting and accessing [SimFactEvent] objects,
 * with respective JPA attributes specified in a generated meta-model: [SimFactEventEntity_].
 *
 * This representative object suits [SimFactEvent] that have:
 * - [UUID]-based identifiers (for [id], [SimFactExchangeEmbed.id],
 *   [SimFactExchangeEmbed.executorRef], [SimFactExchangeEmbed.initiatorRef]);
 * - JSON convertible [details] (key, value)-entries (i.e. Jackson [TreeNode]-compatible); and
 * - unique [ordinal] values per [exchange], as facts follow a certain sequence and, because of their
 *   inter-subjective nature, facts of one context can't be ordered differently for the same participants (really?)
 */
@Entity
@Table(name = "FACTS", uniqueConstraints = [
	UniqueConstraint(columnNames = [
		SimFactExchangeEmbed.CTX_REF_COLUMN_NAME,
		SimFactEventEntity.CTX_ORD_COLUMN_NAME] ) ] )
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Suppress("JpaDataSourceORMInspection")
data class SimFactEventEntity(

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
		var exchange: SimFactExchangeEmbed? = null,

		@Basic
		@Column(name = CTX_ORD_COLUMN_NAME, updatable = false, nullable = false)
		var ordinal: Int? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "CREATOR_FK", updatable = false)
		var creatorRef: UuidNameRefEntity? = null,

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "RESPONDER_FK", updatable = false)
		var responderRef: UuidNameRefEntity? = null,

		@OneToMany(cascade = [CascadeType.PERSIST], targetEntity = SimFactLinkEntity::class)
		var linkRefs: Collection<SimFactLinkEntity> = emptySet(),

		@Embedded
		var occur: TimeRefEmbed? = null,

		@Basic
		@Column(name = "DETAILS", nullable = true, updatable = false)
		@Convert(converter = TreeNodeJpaConverter::class)
		var details: TreeNode? = null

) {

	constructor(
			fact: SimFactEvent,
			epoch: Instant = Instant.EPOCH,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },
			resultKindFetcher: (ResultKind) -> ResultKindEntity = { ResultKindEntity(resultKind = it) },
			factLinkFetcher: (FactLink) -> SimFactLinkEntity = { SimFactLinkEntity(factLink = it) },

			// fetched attribute values
			contextEmbed: SimFactExchangeEmbed = SimFactExchangeEmbed(
					exchange = fact.getExchange(),
					resultKindFetcher = resultKindFetcher,
					nameRefFetcher = nameRefFetcher),
			creatorRefEntity: UuidNameRefEntity = if(fact.creatorRef() == fact.getExchange().getInitiatorRef())
				contextEmbed.initiatorRef!!
			else
				contextEmbed.executorRef!!,
			responderRefEntity: UuidNameRefEntity = if(fact.responderRef() == fact.getExchange().getInitiatorRef())
				contextEmbed.initiatorRef!!
			else
				contextEmbed.executorRef!!,
			links: Collection<SimFactLinkEntity> = fact.getLinks().map(factLinkFetcher),
			occur: TimeRefEmbed = TimeRefEmbed(fact.getOccur(), epoch),
			details: TreeNode? = OBJECT_MAPPER.valueToTree(fact.getDetails())
	): this(
			id = fact.getId().get(),
			kind = fact.getKind(),
			exchange = contextEmbed,
			ordinal = fact.getOrdinal().get(),
			creatorRef = creatorRefEntity,
			responderRef = responderRefEntity,
			linkRefs = links,
			occur = occur,
			details = details)

	fun toSimFactEvent(): SimFactEvent = SimFactEvent(
			id = UuidRef(this.id!!),
			kind = this.kind!!,
			exchange = this.exchange!!.toSimFactContext(),
			ordinal = this.ordinal!!.let(::IntRef),
			occur = this.occur!!.exact!!.let(::DsolTimeRef),
			links = this.linkRefs.map(SimFactLinkEntity::toFactLink),
			details = this.details
					?.let { OBJECT_MAPPER.treeToValue<Map<in IdRef<*, *>, Ref<*>>>(it) }
					?: emptyMap())

	/** for [Table] column constraint specification */
	companion object {
		const val CTX_ORD_COLUMN_NAME = "ORD"
	}
}