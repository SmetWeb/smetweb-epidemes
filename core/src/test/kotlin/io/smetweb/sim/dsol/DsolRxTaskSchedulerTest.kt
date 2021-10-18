package io.smetweb.sim.dsol

import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.time.ClockService
import org.awaitility.kotlin.await
import org.djutils.event.EventTypeInterface
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

const val REPORT_INTERVAL = 100L

private val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS ZZZ")

@SpringBootTest(classes = [SchedulableTarget::class, TestConfig::class])
class DsolRxTaskSchedulerTest {

	private val log: Logger = getLogger()

	@Autowired
	private lateinit var scheduler: DsolRxTaskScheduler

	@SpyBean
	private lateinit var tasks: SchedulableTarget

	@Test
	fun `scheduling with Spring` () {
		assertNotNull(scheduler)
		// add some introspection
		scheduler.model().statusSource.subscribe(
				{ s: EventTypeInterface -> log.debug("{} emitted", s) },
				{ e: Throwable -> log.error("Simulator failed: {}", e.message, e) },
				{ log.debug("Simulation complete") } )
		scheduler.model().timeSource.subscribe(
				{ t: DsolTimeRef -> log.debug("t={}", t) },
				{ e: Throwable -> log.error("Simulator failed: {}", e.message, e) },
				{ log.debug("t=T") } )

		assertNotNull(tasks)
		val n = 11
		val dt = Duration.ofSeconds(1)
		log.debug("Verifying exactly {} scheduled calls occur within max {} (wall-clock time)...", n, dt)

		scheduler.start()
		// will poll every 100ms, and kill the test on first success, or times out (failure)
		await.atMost(dt).untilAsserted {
			verify(tasks, times(n)).reportCurrentTime()
		}
	}

}

@Component
open class SchedulableTarget(
	private val clockService: ClockService
) {

	private val log = getLogger()

	@Scheduled(fixedRate = REPORT_INTERVAL)
	open fun reportCurrentTime() {
		val utc: Instant = this.clockService.instant()
		val localTime: String = DATE_FORMAT.format(this.clockService.date())
		log.info("Managed time is: {} (UTC/GMT), or local: {} ({})", utc, localTime, this.clockService.zone())
	}
}

@TestConfiguration
@EnableScheduling
open class TestConfig: SchedulingConfigurer {

	override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
		taskRegistrar.setScheduler(taskScheduler())
	}

	@Primary
	@Bean(destroyMethod = "shutdown")
	open fun taskScheduler(): DsolRxTaskScheduler = DsolRxTaskScheduler(
		ScenarioConfig(setupName = "dsolTest", durationPeriod = "PT1S"))

}