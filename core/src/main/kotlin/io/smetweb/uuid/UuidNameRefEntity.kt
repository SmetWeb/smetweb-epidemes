package io.smetweb.uuid

import jakarta.persistence.*
import com.fasterxml.jackson.annotation.JsonIgnore
import io.smetweb.fact.ActorKind
import java.util.*

/**
 * [UuidNameRefEntity] is a data access object for persisting [UuidNameRef] objects, with respective
 * JPA attributes specified in the generated [UuidNameRefEntity_] meta-model.
 */
@Entity
@Cacheable
@Table(name = UuidNameRefEntity.TABLE_NAME, uniqueConstraints = [
	// NOTE: multi-column constraints unsupported in Neo4J
	UniqueConstraint( columnNames = [UuidNameRefEntity.CONTEXT_COLUMN_NAME,
		UuidNameRefEntity.VALUE_COLUMN_NAME] ),
	UniqueConstraint( columnNames = [UuidNameRefEntity.CONTEXT_COLUMN_NAME,
		UuidNameRefEntity.PARENT_COLUMN_NAME] ) ] )
@Inheritance(
		// SINGLE_TABLE preferred, see https://en.wikibooks.org/wiki/Java_Persistence/Inheritance
		strategy = InheritanceType.SINGLE_TABLE )
data class UuidNameRefEntity(

		@Id
		@GeneratedValue( // strategy = GenerationType.IDENTITY unsupported in Neo4J
				strategy = GenerationType.AUTO)
		@Column(name = "PK", nullable = false, updatable = false,
				insertable = false)
		var pk: Int? = null,

		@JsonIgnore
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "CREATED_TS", insertable = false, updatable = false,
				columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
		var created: Date? = null,

		@Column(name = VALUE_COLUMN_NAME, nullable = false, updatable = false)
		var value: String? = null,

		@JsonIgnore
		@Basic // for meta-model, see http://stackoverflow.com/q/27333779
		@Column(name = CONTEXT_COLUMN_NAME, nullable = false, updatable = false,
				length = UuidJpaConverter.SQL_LENGTH, columnDefinition = UuidJpaConverter.SQL_DEFINITION)
		@Convert(converter = UuidJpaConverter::class)
		var contextRef: UUID? = null,

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = PARENT_COLUMN_NAME, nullable = true, updatable = false)
		var parentRef: UuidNameRefEntity? = null
) {

	constructor(
			nameRef: UuidNameRef,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },

			// fetched attribute values
			parentRefEntity: UuidNameRefEntity? = nameRef.parentRef?.parentRef?.let {
				nameRefFetcher(nameRef.parentRef as UuidNameRef)
			}
	): this(
			value = nameRef.get().toString(),
			contextRef = nameRef.rootRef(),
			parentRef = parentRefEntity)

	constructor(contextRef: UUID, javaType: Class<*>): this(
			contextRef = contextRef,
			value = javaType.javaClass.name)

	fun toActorKind(): Class<out ActorKind> = Class.forName(this.value)
			.asSubclass(ActorKind::class.java)

	fun toUuidNameRef(
			parentRef: UuidNameRef? = this.parentRef?.toUuidNameRef()
					?: UuidNameRef(this.contextRef ?: error("Attribute not loaded?"))
	) = UuidNameRef(parentRef = parentRef, value = this.value ?: error("Attribute not loaded?"))

	companion object {

		const val TABLE_NAME = "NAMES"

		/** for [Table] column constraint specification */
		const val VALUE_COLUMN_NAME = "VALUE"

		/** for [Table] column constraint specification */
		const val PARENT_COLUMN_NAME = "PARENT_FK"

		/** for [Table] column constraint specification */
		const val CONTEXT_COLUMN_NAME = "CONTEXT"

	}
}