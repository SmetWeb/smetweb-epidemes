package io.smetweb.sim.event

import io.smetweb.fact.FactLink
import io.smetweb.fact.ResultKind
import io.smetweb.uuid.UuidJpaConverter
import io.smetweb.uuid.UuidRef
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "FACT_LINKS", uniqueConstraints = [
	UniqueConstraint(columnNames = [
		UuidFactLinkEntity.FACT_COLUMN_NAME,
		UuidFactLinkEntity.LINK_REF_COLUMN_NAME] ) ] )
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Suppress("JpaDataSourceORMInspection")
data class UuidFactLinkEntity(

		/** [pk] is meant for database relations only, value has no bearing on [fact] + [linkRef] attributes */
		@Id
		@GeneratedValue(
				// GenerationType.IDENTITY strategy unsupported in Neo4J
				strategy = GenerationType.AUTO)
		@Column(name = "PK", nullable = false, updatable = false)
		var pk: Int? = null,

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = FACT_COLUMN_NAME, updatable = false)
		var fact: UuidFactEntity? = null,

		/** [linkRef] refers to a directly causal 'agendum', conditional, or otherwise related [UuidFact] */
		@Basic
		@Column(name = LINK_REF_COLUMN_NAME, updatable = false,
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var linkRef: UUID? = null,

		/** [link] may link to another (e.g. causal or conditional) fact only iff persisted locally  */
		@ManyToOne(optional = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "LINK_FK", updatable = false)
		var link: UuidFactEntity? = null,

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "LINK_RESULT_KIND_FK", updatable = false)
		var linkResultKind: UuidResultKindEntity? = null
) {
	constructor(
			factLink: FactLink,

			// fetcher lambda's, should fetch from, or insert into database
			factFetcher: (UuidFact) -> UuidFactEntity = { UuidFactEntity(fact = it) },
			resultKindFetcher: (ResultKind) -> UuidResultKindEntity = { UuidResultKindEntity(resultKind = it) },

			// fetched attribute values
			factEntity: UuidFactEntity = factFetcher(factLink.fact as UuidFact),
			linkEntity: UuidFactEntity? = factLink.link?.let { factFetcher(it as UuidFact) },
			linkResultKindEntity: UuidResultKindEntity = linkEntity?.exchange?.resultKind
					?: resultKindFetcher(factLink.linkResultKind)
	): this(
			fact = factEntity,
			linkRef = (factLink.linkRef as UuidRef).get(),
			link = linkEntity,
			linkResultKind = linkResultKindEntity
	)

	fun toFactLink(): FactLink = FactLink.of(
			fact = this.fact?.toSimFactEvent() ?: error("Attribute not loaded"),
			linkFetcher = { this.link?.toSimFactEvent() },
			linkRef = UuidRef(this.linkRef ?: error("Attribute not loaded")),
			linkResultKind = this.linkResultKind?.toResultKind() ?: error("Attribute not loaded"))

	/** for [Table] column constraint specification */
	companion object {
		const val FACT_COLUMN_NAME = "FACT_FK"
		const val LINK_REF_COLUMN_NAME = "LINK_REF"
	}
}