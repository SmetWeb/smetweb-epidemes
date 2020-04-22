package io.smetweb.persist

import io.smetweb.domain.ontology.ResultKind
import io.smetweb.uuid.UuidNameRef
import java.util.*
import javax.persistence.*

@Entity
@Cacheable
@Table(name = "RESULT_KINDS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Suppress("JpaDataSourceORMInspection")
data class ResultKindEntity(

		/** [pk] is meant for database relations only, value has no bearing on [name] attribute */
		@Id
		@GeneratedValue(
				// GenerationType.IDENTITY strategy unsupported in Neo4J
				strategy = GenerationType.AUTO)
		@Column(name = "PK", nullable = false, updatable = false)
		protected var pk: Int? = null,

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "CREATED_TS", insertable = false, updatable = false,
				columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
		protected var created: Date? = null,

		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "NAME", updatable = false)
		var name: UuidNameRefEntity? = null,

		@ManyToOne
		@JoinColumn(name = "EXECUTOR_KIND_FK", nullable = false, updatable = false)
		var executorKind: UuidNameRefEntity? = null,

		@ManyToMany(targetEntity = UuidNameRefEntity::class)
		@JoinTable(name = "RESULT_INITIATOR_KINDS",
				joinColumns = [
					JoinColumn(name = "INITIATOR_KIND_FK", nullable = false, updatable = false)],
				inverseJoinColumns = [
					JoinColumn(name = "RESULT_KIND_FK", nullable = false, updatable = false)])
		var initiatorKinds: Collection<UuidNameRefEntity> = emptySet()

) {
	constructor(
			resultKind: ResultKind,

			// fetcher lambda's, should fetch from, or insert into database
			nameRefFetcher: (UuidNameRef) -> UuidNameRefEntity = { UuidNameRefEntity(nameRef = it) },

			// fetched attribute values
			nameRefEntity: UuidNameRefEntity = nameRefFetcher(resultKind.nameRef() as UuidNameRef),
			executorKindEntity: UuidNameRefEntity = nameRefFetcher(
					UuidNameRef(resultKind.getExecutorKind(), resultKind.getRootRef() as UUID)),
			initiatorKindEntities: Collection<UuidNameRefEntity> = resultKind.getInitiatorKinds().map { actorKind ->
				if(actorKind == resultKind.getExecutorKind())
					executorKindEntity
				else
					nameRefFetcher(UuidNameRef(actorKind, resultKind.getRootRef() as UUID))
			}
	): this(
			name = nameRefEntity,
			executorKind = executorKindEntity,
			initiatorKinds = initiatorKindEntities)

	fun toResultKind(): ResultKind = ResultKind.of(
			name = name!!.toUuidNameRef(),
			executorKind = executorKind!!.toActorKind(),
			initiatorKinds = initiatorKinds.map { it.toActorKind() })
}