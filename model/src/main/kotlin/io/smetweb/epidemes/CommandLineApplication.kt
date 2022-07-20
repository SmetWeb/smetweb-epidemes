package io.smetweb.epidemes

import io.smetweb.epidemes.deme.DemeConfig
import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.xml.XmlConfig
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(scanBasePackages = ["io.smetweb"])
@EntityScan(basePackages = ["io.smetweb.uuid", "io.smetweb.time", "io.smetweb.sim.event"])
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableConfigurationProperties(ScenarioConfig::class, DemeConfig::class, XmlConfig::class)
class CommandLineApplication(
	private val scenarioConfig: ScenarioConfig,
	private val demeConfig: DemeConfig,
	private val xmlConfig: XmlConfig,
) {

	private val log = getLogger()

	/**
	 * 1. load CLI args
	 * 2. load YAML config
	 * 3. start/reset logger
	 * 4. load module config(s)
	 * 5. load sim config
	 * 6. setup inputs
	 * 7. setup outputs
	 * 8. start sim
	 */
	@Bean
	fun cliRunner(): CommandLineRunner = CommandLineRunner { args ->
		log.info("Started. args: {}, \nscenario: {}\ndeme: {}\nxml: {}", args, scenarioConfig, demeConfig, xmlConfig)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<CommandLineApplication>(*args) {
				setBannerMode(Banner.Mode.OFF)
			}
		}
	}

//            val argMap: MutableMap<String, String> = ConfigUtil.cliArgMap(args)
//            val confBase = argMap.computeIfAbsent(DemoConfig.CONFIG_BASE_KEY
//            ) { k: String? ->
//                System.getProperty(DemoConfig.CONFIG_BASE_KEY,
//                        DemoConfig.CONFIG_BASE_DIR)
//            }
//            val confFile = argMap.computeIfAbsent(DemoConfig.CONF_ARG
//            ) { confArg: String? ->
//                System.getProperty(DemoConfig.CONF_ARG,
//                        confBase + DemoConfig.CONFIG_YAML_FILE)
//            }
//
//            val config: DemoConfig = ConfigFactory.create(DemoConfig::class.java,  // CLI args added first: override config resource and defaults
//                    argMap,
//                    YamlUtil.flattenYaml(FileUtil.toInputStream(confFile)))
//
//            if (System.getProperty(
//                            ConfigurationFactory.CONFIGURATION_FILE_PROPERTY) == null) try {
//                FileUtil
//                        .toInputStream(config.configBase().toString() + "log4j2.yaml").use({ `is` ->
//                            // see https://stackoverflow.com/a/42524443
//                            val ctx: LoggerContext = LoggerContext.getContext(false)
//                            ctx.start(YamlConfiguration(ctx, ConfigurationSource(`is`)))
//                        })
//            } catch (ignore: IOException) {
//            }
//
//            val demeModule: Class<out PersonBroker?> = config.demeModule()
//            val demeConfig: JsonNode = (config.toJSON(DemoConfig.SCENARIO_BASE,
//                    DemoConfig.DEMOGRAPHY_BASE) as ObjectNode).put(DemoConfig.CONFIG_BASE_KEY, confBase)
////		LOG.debug( "Deme config: {}", JsonUtil.toJSON( demeConfig ) );
//
//            //		LOG.debug( "Deme config: {}", JsonUtil.toJSON( demeConfig ) );
//            val healthModule: Class<out HealthBroker?> = config
//                    .healthModule()
//            val healthConfig: JsonNode = (config.toJSON(DemoConfig.SCENARIO_BASE,
//                    DemoConfig.EPIDEMIOLOGY_BASE) as ObjectNode).put(DemoConfig.CONFIG_BASE_KEY, confBase)
////		LOG.debug( "Health config: {}", JsonUtil.toJSON( healthConfig ) );
//
//            //		LOG.debug( "Health config: {}", JsonUtil.toJSON( healthConfig ) );
//            val peerModule: Class<out PeerBroker?> = config.peerModule()
//            val peerConfig: JsonNode = (config.toJSON(DemoConfig.SCENARIO_BASE,
//                    DemoConfig.HESITANCY_BASE) as ObjectNode).put(DemoConfig.CONFIG_BASE_KEY, confBase)
////		LOG.debug( "Peer config: {}", JsonUtil.toJSON( peerConfig ) );
//
//            //		LOG.debug( "Peer config: {}", JsonUtil.toJSON( peerConfig ) );
//            val siteModule: Class<out SiteBroker?> = config.siteModule()
//            val siteConfig: JsonNode = (config.toJSON(DemoConfig.SCENARIO_BASE,
//                    DemoConfig.GEOGRAPHY_BASE) as ObjectNode).put(DemoConfig.CONFIG_BASE_KEY, confBase)
////		LOG.debug( "Site config: {}", JsonUtil.toJSON( siteConfig ) );
//
//            //		LOG.debug( "Site config: {}", JsonUtil.toJSON( siteConfig ) );
//            val societyModule: Class<out SocietyBroker?> = config
//                    .societyModule()
//            val societyConfig: JsonNode = (config.toJSON(DemoConfig.SCENARIO_BASE,
//                    DemoConfig.MOTION_BASE) as ObjectNode).put(DemoConfig.CONFIG_BASE_KEY, confBase)
////		LOG.debug( "Society config: {}", JsonUtil.toJSON( societyConfig ) );
//
//            //		LOG.debug( "Society config: {}", JsonUtil.toJSON( societyConfig ) );
//            val offset: ZonedDateTime = config.offset()
//                    .atStartOfDay(TimeUtil.NL_TZ)
//            val durationDays = Duration
//                    .between(offset, offset.plus(config.duration())).toDays()
//            val binderConfig: LocalConfig = LocalConfig.builder().withProvider(
//                    Scheduler::class.java, Dsol3Scheduler::class.java,
//                    MapBuilder.unordered()
//                            .put(SchedulerConfig.ID_KEY, "" + config.setupName())
//                            .put(SchedulerConfig.OFFSET_KEY, "" + offset)
//                            .put(SchedulerConfig.DURATION_KEY, "" + durationDays)
//                            .build())
//                    .withProvider(ProbabilityDistribution.Parser::class.java,
//                            DistributionParser::class.java) // add data layer: static caching
//                    .withProvider(DataLayer::class.java, DataLayer.StaticCaching::class.java) // add deme to create households/persons
//                    .withProvider(PersonBroker::class.java, demeModule, demeConfig) // add site broker for regions/sites/transmission
//                    .withProvider(SiteBroker::class.java, siteModule, siteConfig) // add society broker for groups/gatherings
//                    .withProvider(SocietyBroker::class.java, societyModule,
//                            societyConfig)
//                    .withProvider(PeerBroker::class.java, peerModule, peerConfig)
//                    .withProvider(HealthBroker::class.java, healthModule, healthConfig)
//                    .build()
//
//            // FIXME workaround until seed becomes configurable from coala
//
//            // FIXME workaround until seed becomes configurable from coala
//            val rng: PseudoRandom = MersenneTwisterFactory()
//                    .create(PseudoRandom.Config.NAME_DEFAULT,
//                            config.randomSeed())
//            val binder: LocalBinder = binderConfig.createBinder(MapBuilder
//                    .< Class <? >, Object>unordered<java.lang.Class<*>?, kotlin.Any?>()
//            .put(ProbabilityDistribution.Factory::class.java,
//                    Factory(rng))
//                    .build())
//
//            nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("Constructing model, seed: {}, config: {}", rng.seed(),
//                    JsonUtil.toJSON(binderConfig.toJSON()))
//            val model: DemoScenarioSimple = binder
//                    .inject(DemoScenarioSimple::class.java)
//
//            val hier: CbsRegionHierarchy
//            FileUtil
//                    .toInputStream(confBase + "data/83287NED.json" // 2016
//                    ).use({ `is` -> hier = JsonUtil.getJOM().readValue(`is`, CbsRegionHierarchy::class.java) })
//            val gmRegions: TreeMap<String?, EnumMap<CBSRegionType, String>> = hier
//                    .cityRegionsByType()
//
//            // TODO from config
//
//            // TODO from config
//            val seed: Long = rng.seed().longValue()
////		final long timestamp = System.currentTimeMillis();
//            //		final long timestamp = System.currentTimeMillis();
//            val totalsFile = "daily-$seed-sir-total.csv"
//            val deltasFile = "daily-$seed-sir-delta.csv"
//            val timing = "0 0 12 ? * *"
//            val n = 10
//            val sirCols: List<Compartment> = Arrays.asList<Compartment>(
//                    Compartment.SUSCEPTIBLE, Compartment.INFECTIVE,
//                    Compartment.RECOVERED, Compartment.VACCINATED)
//            val sortLogCol: Compartment = Compartment.INFECTIVE
//            val aggregationLevel: CBSRegionType = CBSRegionType.HEALTH_SERVICES
//
//            val gmChanges: Map<String?, String> = CbsRegionHistory.allChangesAsPer(
//                    LocalDate.of(2016, 1, 1), CbsRegionHistory.parse(
//                    confBase, "data/gm_changes_before_2018.csv"))
//            // TODO pick neighbor within region(s)
//            // TODO pick neighbor within region(s)
//            val gmFallback = "GM0363"
//
//            val configTree = config
//                    .toJSON(DemoConfig.SCENARIO_BASE)
//            configTree.with(DemoConfig.REPLICATION_BASE)
//                    .put(DemoConfig.RANDOM_SEED_KEY, seed)
//
//            val regNames = TreeMap<String, Set<String>>()
//            Observable.using<Any, FileWriter>({ FileWriter(totalsFile, false) },
//                    { fw: FileWriter ->
//                        model.atEach(timing).map({ self ->
//                            val totals: Map<String, EnumMap<Compartment?, Long?>?> = self
//                                    .exportRegionalSIRTotal()
//                            if (regNames.isEmpty()) {
//                                regNames.putAll(totals.keys.stream().collect(
//                                        Collectors.groupingBy<String, String?, TreeSet<String?>, Any, TreeMap<String, TreeSet<String>>>(java.util.function.Function { gmName: String? ->
//                                            gmRegions
//                                                    .computeIfAbsent(gmName, java.util.function.Function<String?, EnumMap<CBSRegionType, String>> { k: String? ->
//                                                        if (gmChanges.containsKey(k)) return@computeIfAbsent gmRegions[gmChanges[k]]
//                                                        nl.rivm.cib.epidemes.demo.impl.Main.LOG.warn("Aggregating {} as {}",
//                                                                gmName, gmFallback)
//                                                        gmRegions[gmFallback]
//                                                    })[aggregationLevel]
//                                        },
//                                                Supplier { TreeMap<String, TreeSet<String>>() },
//                                                Collectors.toCollection(
//                                                        Supplier { TreeSet<String?>() }))))
//                                fw.write(DemoConfig.toHeader(configTree, sirCols,
//                                        regNames))
//                            }
//                            fw.write(DemoConfig.toLine(sirCols,
//                                    model.scheduler().nowDT().toLocalDate().toString(),
//                                    regNames, totals))
//                            fw.flush()
//                            totals
//                        })
//                    }) { obj: FileWriter -> obj.close() }.subscribe(
//                    { homeSIR: Any? ->
//                        nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("t={} TOTAL-top{}:{ {} }",
//                                model.scheduler().nowDT(), n,
//                                Pretty.of({
//                                    DemoConfig.toLog(sirCols,
//                                            homeSIR, n, sortLogCol)
//                                }))
//                    },
//                    { e: Throwable? ->
//                        nl.rivm.cib.epidemes.demo.impl.Main.LOG.error("Problem writing $totalsFile", e)
//                        System.exit(1)
//                    }) {
//                nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("SIR totals written to {}",
//                        totalsFile)
//            }
//
//            val first = AtomicBoolean(true)
//            Observable.using<Any, FileWriter>({ FileWriter(deltasFile, false) },
//                    { fw: FileWriter ->
//                        model.atEach(timing).map({ self ->
//                            val deltas: Map<String?, EnumMap<Compartment?, Long?>?> = self
//                                    .exportRegionalSIRDelta()
//                            if (first.get()) {
//                                first.set(false)
//                                fw.write(DemoConfig.toHeader(configTree, sirCols,
//                                        regNames))
//                            }
//                            fw.write(DemoConfig.toLine(sirCols,
//                                    model.scheduler().nowDT().toLocalDate().toString(),
//                                    regNames, deltas))
//                            fw.flush()
//                            deltas
//                        })
//                    }) { obj: FileWriter -> obj.close() }.subscribe(
//                    { homeSIR: Any? ->
//                        nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("t={} DELTA-top{}:{ {} }",
//                                model.scheduler().nowDT(), n,
//                                Pretty.of({
//                                    DemoConfig.toLog(sirCols,
//                                            homeSIR, n, sortLogCol)
//                                }))
//                    },
//                    { e: Throwable? ->
//                        nl.rivm.cib.epidemes.demo.impl.Main.LOG.error("Problem writing $deltasFile", e)
//                        System.exit(1)
//                    }) {
//                nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("SIR deltas written to {}",
//                        deltasFile)
//            }
//
//            nl.rivm.cib.epidemes.demo.impl.Main.LOG.debug("Starting...")
//            model.run()        }
}