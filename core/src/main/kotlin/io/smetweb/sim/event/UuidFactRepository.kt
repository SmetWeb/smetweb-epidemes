package io.smetweb.sim.event

import io.smetweb.db.*
import io.smetweb.fact.FactLink
import io.smetweb.fact.ResultKind
import io.smetweb.sim.ScenarioConfig
import io.smetweb.uuid.UuidNameRefRepository
import io.smetweb.uuid.UuidRef
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

// TODO merge with Spring's JpaRepository<UuidFactEntity, Int>

@Repository
@Transactional
open class UuidFactRepository(
		private val scenarioConfig: ScenarioConfig,
		private val uuidNameRefRepository: UuidNameRefRepository
): PersistentDao<UuidFact, UuidRef> {

	@PersistenceContext
	private lateinit var em: EntityManager

	private fun findOrCreateResultKind(resultKind: ResultKind): UuidResultKindEntity {
		val entity = UuidResultKindEntity(resultKind = resultKind,
				nameRefFetcher = this.uuidNameRefRepository::findOrCreate)
		return this.em.findOrCreate(
				{ this.em.findByAttribute(UuidResultKindEntity_.name, entity.name).firstOrNull() },
				{ entity })
	}

	private fun findOrCreateFactLink(factLink: FactLink): UuidFactLinkEntity {
		val entity = UuidFactLinkEntity(factLink = factLink,
				factFetcher = ::findOrCreateFact,
				resultKindFetcher = ::findOrCreateResultKind)
//		TODO ("query whether link exists")
		return this.em.findOrCreate(
				{ this.em.findByAttribute(UuidFactLinkEntity_.link, entity.link).firstOrNull() },
				{ entity })
	}

	private fun findOrCreateFact(fact: UuidFact): UuidFactEntity {
		return this.em.findOrCreate(
				{ this.em.findByAttribute(UuidFactEntity_.id, fact.id.get()).firstOrNull() },
				{
					UuidFactEntity(fact = fact,
							epoch = this.scenarioConfig.epoch,
							nameRefFetcher = this.uuidNameRefRepository::findOrCreate,
							resultKindFetcher = ::findOrCreateResultKind,
							factLinkFetcher = ::findOrCreateFactLink)
				})
	}

	override fun save(entry: UuidFact) {
		if(this.em.existAttributeValues(UuidFactEntity_.id, entry.id.get()).isEmpty()) {
			val entity = UuidFactEntity(
					fact = entry,
					epoch = this.scenarioConfig.epoch,
					nameRefFetcher = this.uuidNameRefRepository::findOrCreate,
					resultKindFetcher = ::findOrCreateResultKind,
					factLinkFetcher = ::findOrCreateFactLink)
			this.em.merge(entity)
		}
	}

	override fun delete(entry: UuidFact): Int =
			this.em.deleteByAttribute(UuidFactEntity_.id, entry.id.get())

	override fun findByKey(key: UuidRef): UuidFact? =
			this.em.findByAttribute(UuidFactEntity_.id, key.get())
					.firstOrNull()
					?.toUuidFact()

}