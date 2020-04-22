package io.smetweb.persist

import io.smetweb.domain.ontology.FactLink
import io.smetweb.domain.ontology.ResultKind
import io.smetweb.sim.SimFactEvent
import io.smetweb.jpa.PersistentDao
import io.smetweb.uuid.UuidRef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
@Transactional
open class SimFactEventRepository(
		@Autowired
		val uuidNameRefRepository: UuidNameRefRepository
): PersistentDao<SimFactEvent, UuidRef> {

	@Value("\${sim.time.epoch:2000-01-01T00:00:00Z}") // TODO use Configuration bean
	private lateinit var epoch: Instant

	@PersistenceContext
	private lateinit var em: EntityManager

	private fun findOrCreateResultKind(resultKind: ResultKind): ResultKindEntity {
		val entity = ResultKindEntity(resultKind = resultKind,
				nameRefFetcher = this.uuidNameRefRepository::findOrCreate)
		return PersistentDao.findOrCreate(this.em,
				{ PersistentDao.findByAttribute(this.em, ResultKindEntity_.name, entity.name).firstOrNull() },
				{ entity })
	}

	private fun findOrCreateFactLink(factLink: FactLink): SimFactLinkEntity {
		val entity = SimFactLinkEntity(factLink = factLink,
				factFetcher = ::findOrCreateFact,
				resultKindFetcher = ::findOrCreateResultKind)
//		TODO ("query if link exists")
		return PersistentDao.findOrCreate(this.em,
				{ PersistentDao.findByAttribute(this.em, SimFactLinkEntity_.link, entity.link).firstOrNull() },
				{ entity })
	}

	private fun findOrCreateFact(fact: SimFactEvent): SimFactEventEntity {
		return PersistentDao.findOrCreate(this.em,
				{ PersistentDao.findByAttribute(this.em, SimFactEventEntity_.id, fact.getId().get()).firstOrNull() },
				{ SimFactEventEntity(fact = fact,
						epoch = this.epoch,
						nameRefFetcher = this.uuidNameRefRepository::findOrCreate,
						resultKindFetcher = ::findOrCreateResultKind,
						factLinkFetcher = ::findOrCreateFactLink) })
	}

	override fun save(entry: SimFactEvent) {
		if(PersistentDao.existAttributeValues(this.em, SimFactEventEntity_.id, entry.getId().get()).isEmpty()) {
			val entity = SimFactEventEntity(
					fact = entry,
					epoch = epoch,
					nameRefFetcher = this.uuidNameRefRepository::findOrCreate,
					resultKindFetcher = ::findOrCreateResultKind,
					factLinkFetcher = ::findOrCreateFactLink)
			this.em.merge(entity)
		}
	}

	override fun delete(entry: SimFactEvent): Int =
			PersistentDao.deleteByAttribute(this.em, SimFactEventEntity_.id, entry.getId().get())

	override fun findByKey(key: UuidRef): SimFactEvent? =
			PersistentDao.findByAttribute(this.em, SimFactEventEntity_.id, key.get())
					.firstOrNull()
					?.toSimFactEvent()

}