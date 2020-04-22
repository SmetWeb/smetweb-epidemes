package io.smetweb

import io.smetweb.persist.SimFactEventRepository
import io.smetweb.sim.SimFactEvent
import io.smetweb.uuid.UuidNameRef
import io.smetweb.sim.dsol.DsolTimeRef
import io.smetweb.sim.SimFactExchange
import io.smetweb.domain.ontology.ResultKind
import io.smetweb.log.getLogger
import io.smetweb.persist.UuidNameRefRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

//@SpringBootTest(classes = [FactRepository::class])
@EnableAutoConfiguration
@ExtendWith(SpringExtension::class)
@DataJpaTest
@ContextConfiguration(classes = [SimFactEventRepository::class, UuidNameRefRepository::class])
@AutoConfigureTestDatabase // default is in-memory, otherwise set (replace=Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // default is rollback all, otherwise set
open class SimFactEventEntityTest {

	private val log = getLogger()

	@Autowired
	private lateinit var simFactEventRepository: SimFactEventRepository

	@Test
	fun `entity setup` () {
		val tick = DsolTimeRef.T_ZERO
		val root = UuidNameRef()
		val me = UuidNameRef("actor1", root)
		val them = UuidNameRef("actor2", root)
		val ctx = SimFactExchange(resultKind = ResultKind.of(UuidNameRef( "T01", root)),
				executorRef = me, initiatorRef = them)
		val fact = SimFactEvent(occur = tick, exchange = ctx)
		simFactEventRepository.save(fact)

		val fact2 = SimFactEvent(occur = tick, exchange = ctx, ordinal = fact.getOrdinal().next())
		assertNotEquals(fact2.getId().get(), fact.getId().get(), "generated new unique fact id")

		log.info(" Persisted {}", fact)
		log.info("Persisting {}", fact2)
		simFactEventRepository.save(fact2)
		assertNotEquals(fact2.getOrdinal(), fact.getOrdinal(), "generated new unique ctx ordinal")

		val created = simFactEventRepository.save(fact)
//		assertEquals(fact.getId(), created, "generated new private key")

		val removedCount = simFactEventRepository.delete(fact)
		assertEquals(1, removedCount, "just one record removed")

		val recreated = simFactEventRepository.save(fact)
		assertEquals(created, recreated, "persisted same values")
//		assertNotEquals(created.pk, recreated.pk, "generated new private key")

		val constraintViolation = simFactEventRepository.save(fact)
		assertEquals(recreated, constraintViolation, "unique constraint violation averted")

		// just one record removed
		val removedAgainCount = simFactEventRepository.delete(fact)
		assertEquals(1, removedAgainCount, "just one record removed again")


//			// TODO test fact expiration handling
//
//			// TODO test multilevel composition of business rules, e.g. via sub-goals?
//
//			// TODO test performance statistics aggregation
//
//			// TODO test on-the-fly adapting business rules
//			// e.g. parametric: "reorder-level: 300->400"
//			// or compositional: "product-lines: a[demand push->pull]"
//
//			// TODO test Jason or GOAL scripts for business rules
//
//			LOG.trace( "initializing..." );
//
//			final Actor<Fact> org1 = this.actors.create( "org1" );
//			LOG.trace( "initialized organization" );
//
//			final DateTime epoch = new DateTime(
//					scheduler().epoch().toInstant().toEpochMilli() );
//			LOG.trace( "initialized occurred and expired fact sniffing" );
//
//			org1.emitFacts().subscribe( fact ->
//			{
//				LOG.trace( "t={}, occurred: {}", now().prettify( epoch ),
//						fact );
//			}, e -> LOG.error( "Problem", e ) );
//
//			final AtomicInteger counter = new AtomicInteger( 0 );
//			final Procurement proc = org1.subRole( Procurement.class );
//			final Sales sales = org1.subRole( Sales.class );
//			sales.setTotalValue( 0 );
//			sales.emit( FactKind.REQUESTED ).subscribe(
//					rq -> after( Duration.of( 1, TimeUnits.DAYS ) ).call( t ->
//					{
//						final Sale st = sales.respond( rq, FactKind.STATED )
//								.with( "stParam",
//										"stValue" + counter.getAndIncrement() )
//								.typed();
//						sales.addToTotal( 1 );
//						LOG.trace( "{} responds: {} <- {}, total now: {}",
//								sales.id(), st.causeRef().prettyHash(),
//								st.getStParam(), sales.getTotalValue() );
//						st.commit( true );
//					} ), e -> LOG.error( "Problem", e ),
//					() -> LOG.trace( "sales/rq completed?" ) );
//			LOG.trace( "initialized business rule(s)" );
//
//			atEach( Timing.valueOf( "0 0 0 30 * ? *" ).iterate( scheduler() ),
//					t ->
//					{
//						// spawn initial transactions from/with self
//						final Sale rq = proc.initiate( sales.id(), t.add( 1 ) )
//								.withRqParam( t );
//
//						// de/serialization test
//						final String json = rq.toJSON();
//						LOG.trace( "de/serializing: {} as {} in {}", t,
//								JsonUtil.stringify( t ), json );
//						final String fact = this.binder
//								.injectMembers( // FIXME
//										Sale.fromJSON( json ).transaction() )
//								.toString();
//						LOG.trace( "{} initiates: {} => {}", proc.id(), json,
//								fact );
//						rq.commit();
//					} );
	}

}