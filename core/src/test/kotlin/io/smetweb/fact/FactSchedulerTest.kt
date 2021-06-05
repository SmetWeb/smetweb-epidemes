package io.smetweb.fact

import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.sim.dsol.DsolRxTaskScheduler
import io.smetweb.time.ClockService
import io.smetweb.time.ManagedClockService
import io.smetweb.time.ManagedRxTaskScheduler
import io.smetweb.time.TimeRef
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@SpringBootTest(classes = [FactSchedulerTest.TestModel::class, FactSchedulerTest.TestConfig::class])
class FactSchedulerTest {

	@TestConfiguration
	@EnableScheduling
	open class TestConfig: SchedulingConfigurer {

		override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
			taskRegistrar.setScheduler(taskScheduler())
		}

		@Primary
		@Bean(destroyMethod = "shutdown")
		open fun taskScheduler(): DsolRxTaskScheduler = DsolRxTaskScheduler(ScenarioConfig(
			setupName = "testSim",
			durationPeriod = "P12M",
			offsetDate = "2021-01-01",
			randomSeed = 123L,
			analyst = "testAnalyst",
			description = "testDescription"))

	}

	@Component
	open class TestModel(
		private val clockService: ClockService
	) {

		private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS ZZZ")

		private val localZone = ZoneId.systemDefault()

		private val log = getLogger()

		@Scheduled(fixedDelay = Long.MAX_VALUE) // don't repeat
		open fun initialize() {
			log.info( "initializing..." )

			val utc: Instant = this.clockService.instant()
			val localTime: String = this.dateFormat.format(this.clockService.date())
			log.info("Managed time is: {} (UTC/GMT), or local: {} ({})", utc, localTime, this.localZone)

//			final Actor<Fact> org1 = this.actors.create( "org1" );
//			LOG.trace( "initialized organization" );
//
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

		}
	}

	private val log: Logger = getLogger()

	@Autowired
	private lateinit var scheduler: ManagedRxTaskScheduler

	@SpyBean
	private lateinit var model: TestModel

	@Test
	fun `scheduling with Spring` () {
		assertNotNull(model) // this is NOT the (D-SOL) scheduler.model() stub, but should it?

		assertNotNull(scheduler)

		// add some introspection
		scheduler.statusSource.subscribe(
				{ s: ManagedClockService.ClockStatus -> log.debug("{} emitted", s) },
				{ e: Throwable -> log.error("Simulator failed: {}", e.message, e) },
				{ log.debug("Simulation complete") } )
		scheduler.timeSource.subscribe(
				{ t: TimeRef -> log.debug("t={}", t) },
				{ e: Throwable -> log.error("Simulator failed: {}", e.message, e) },
				{ log.debug("t=T") } )

		scheduler.start() // spawns separate simulator-thread

		await.atMost(Duration.ofMillis(1000)).untilAsserted {
			verify(model, times(1)).initialize()
		}
	}

}

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
//								.injectMembers(
//										Sale.fromJSON( json ).transaction() )
//								.toString();
//						LOG.trace( "{} initiates: {} => {}", proc.id(), json,
//								fact );
//						rq.commit();
//					} );

//	interface MyJPAConfig: HikariHibernateJPAConfig
//	{
//		@DefaultValue( "fact_test_pu" ) // match persistence.xml
//		@Key( JPA_UNIT_NAMES_KEY )
//		String[] jpaUnitNames();
//
////		@DefaultValue( "jdbc:mysql://localhost/testdb" )
////		@DefaultValue( "jdbc:neo4j:bolt://192.168.99.100:7687/db/data" )
////		@DefaultValue( "jdbc:hsqldb:file:target/testdb" )
//		@DefaultValue( "jdbc:hsqldb:mem:mymemdb" )
//		@Key( AvailableSettings.URL )
//		URI jdbcUrl();
//	}

//	/**
//	 * {@link Valuable} super-interface to test {@link ReflectUtil#invokeAsBean}
//	 * coping with http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4275879
//	 */
//	interface Valuable
//	{
//		/**
//		 * test {@link ReflectUtil#invokeAsBean} called by {@link Actor#proxyAs}
//		 */
//		fun getTotalValue(): Int
//
//		/**
//		 * test {@link ReflectUtil#invokeAsBean} called by {@link Actor#proxyAs}
//		 */
//		fun setTotalValue(totalValue: Int);
//
//		/**
//		 * test {@link ReflectUtil#invokeDefaultMethod} called by
//		 * {@link Actor#proxyAs}
//		 */
//		fun addToTotal(increment: Int) = setTotalValue(getTotalValue() + increment)
//	}

////	@Singleton
//	class World: Proactive
//	{
//		interface Sales: Actor<Sale>, Valuable
//		{
//		}
//
//		interface Procurement: Actor<Sale>, Valuable
//		{
//		}
//
//		/**
//		 * {@link Sale} custom fact kind
//		 */
//		interface Sale: Fact
//		{
//			@JsonIgnore
//			Instant getRqParam(); // get "rqParam" bean property
//
//			void setRqParam( Instant value ); // set "rqParam" bean property
//
//			default Sale withRqParam( Instant value ) // test default method
//			{
//				setRqParam( value );
//				return this;
//			}
//
//			@JsonIgnore
//			String getStParam(); // get "stParam" bean property
//
//			static Sale fromJSON( final String json ) // test de/serialization
//			{
//				return Fact.fromJSON( json, Sale.class );
//			}
//		}
//
//		private final Scheduler scheduler;
//
//		@Inject
//		private LocalBinder binder;
//
//		@Inject
//		private Actor.Factory actors;
//
//		@Inject
//		public World( final Scheduler scheduler )
//		{
//			this.scheduler = scheduler;
//			scheduler.onReset( this::initScenario );
//		}
//
//		@Override
//		public Scheduler scheduler()
//		{
//			return this.scheduler;
//		}
//
//		/**
//		 * @param scheduler
//		 * @throws Exception
//		 */
//		public void initScenario() throws Exception
//		{
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
//			LOG.trace( "intialized TestFact initiation" );
//
//			LOG.trace( "initialization complete!" );