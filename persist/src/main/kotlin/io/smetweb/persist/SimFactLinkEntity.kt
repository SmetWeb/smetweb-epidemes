package io.smetweb.persist

import io.smetweb.sim.SimFactEvent
import io.smetweb.domain.ontology.FactLink
import io.smetweb.domain.ontology.ResultKind
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.uuid.UuidRef
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "FACT_LINKS", uniqueConstraints = [
	UniqueConstraint(columnNames = [
		SimFactLinkEntity.FACT_COLUMN_NAME,
		SimFactLinkEntity.LINK_REF_COLUMN_NAME] ) ] )
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Suppress("JpaDataSourceORMInspection")
data class SimFactLinkEntity(

		/** [pk] is meant for database relations only, value has no bearing on [fact] + [linkRef] attributes */
		@Id
		@GeneratedValue(
				// GenerationType.IDENTITY strategy unsupported in Neo4J
				strategy = GenerationType.AUTO)
		@Column(name = "PK", nullable = false, updatable = false)
		var pk: Int? = null,

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = FACT_COLUMN_NAME, updatable = false)
		var fact: SimFactEventEntity? = null,

		/** [linkRef] refers to a directly causal 'agendum', conditional, or otherwise related [SimFactEvent] */
		@Basic
		@Column(name = LINK_REF_COLUMN_NAME, updatable = false,
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var linkRef: UUID? = null,

		/** [link] may link to another (e.g. causal or conditional) fact only iff persisted locally  */
		@ManyToOne(optional = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "LINK_FK", updatable = false)
		var link: SimFactEventEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "LINK_RESULT_KIND_FK", updatable = false)
		var linkResultKind: ResultKindEntity? = null
) {
	constructor(
			factLink: FactLink,

			// fetcher lambda's, should fetch from, or insert into database
			factFetcher: (SimFactEvent) -> SimFactEventEntity = { SimFactEventEntity(fact = it) },
			resultKindFetcher: (ResultKind) -> ResultKindEntity = { ResultKindEntity(resultKind = it) },

			// fetched attribute values
			factEntity: SimFactEventEntity = factFetcher(factLink.getFact() as SimFactEvent),
			linkEntity: SimFactEventEntity? = factLink.getLink()?.let { factFetcher(it as SimFactEvent) },
			linkResultKindEntity: ResultKindEntity = linkEntity?.exchange?.resultKind
					?: resultKindFetcher(factLink.getLinkResultKind())
	): this(
			fact = factEntity,
			linkRef = (factLink.getLinkRef() as UuidRef).get(),
			link = linkEntity,
			linkResultKind = linkResultKindEntity
	)

	fun toFactLink(): FactLink = FactLink.of(
			fact = this.fact!!.toSimFactEvent(),
			linkFetcher = { this.link?.toSimFactEvent() },
			linkRef = UuidRef(this.linkRef!!),
			linkResultKind = this.linkResultKind!!.toResultKind() )

	/** for [Table] column constraint specification */
	companion object {
		const val FACT_COLUMN_NAME = "FACT_FK"
		const val LINK_REF_COLUMN_NAME = "LINK_REF"
	}
}