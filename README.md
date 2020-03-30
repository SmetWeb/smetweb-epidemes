# smetweb-epidemes

This library contains an adapted version of the [epidemes v0.2.0 modeling and mic**R<sub>0</sub>**simulation framework](https://github.com/krevelen/epidemes/releases/tag/v0.2.0) delivered in 2017 for ZonMW research project 522002008: [**From individual perception of vaccination risks and benefits to compliance, costs and effects of vaccination programmes: a pilot study for influenza, HPV and measles**](https://www.zonmw.nl/nl/onderzoek-resultaten/gezondheidsbescherming/programmas/project-detail/infectieziektebestrijding-2014-2017/from-individual-perception-of-vaccination-risks-and-benefits-to-compliance-costs-and-effects-of-vac/verslagen/), as part of their programme [*Infectieziektebestrijding 2014-2017*](https://www.zonmw.nl/nl/onderzoek-resultaten/gezondheidsbescherming/programmas/programma-detail/infectieziektebestrijding-2014-2017/).

The delivered **epidemes** framework was co-funded by [research project Smet WEB](https://www.rivm.nl/rivm/kennis-en-kunde/strategisch-programma-rivm/spr-2015-2018/wiskundige-modellering-ziekten/smet-web) (part of the [Strategic Programme of RIVM (SPR) of 2015-2018](https://www.rivm.nl/rivm/kennis-en-kunde/strategisch-programma-rivm/spr-2015-2018)) to support related projects, for instance to provide [SPR project MORPHINE](https://www.rivm.nl/rivm/kennis-en-kunde/strategisch-programma-rivm/spr-2015-2018/wiskundige-modellering-ziekten/morphine) with a [sample synthetic population with vaccination hesitancy](https://github.com/JHoogink/morphine).

Adaptions include: a transition of build system (Maven &rarr; Gradle), ported programming language (Java 8 &rarr; Kotlin 1.3/Java 11), dependency injector ([Coala/Guice](https://github.com/krevelen/coala-binder) &rarr; Spring-Boot), etc.

## Synthethic Population

![Epidemes Ecosystem Organisation Construction Diagram](./doc/img/epidemes-ecosystem-ocd.png)

### Modules
As noted in this [(draft) report of ZonMW project 522002008](https://github.com/krevelen/epidemes/releases/download/v0.2.0/zonmw-522002008-report-draft.pdf), the epidemes ecosystem consists of several separate "modules" (actor kinds and transaction kinds, as per the [DEMO methodology](https://en.wikipedia.org/wiki/Design_%26_Engineering_Methodology_for_Organizations) common in [enterprise engineering](http://www.ee-institute.org/en)) to facilitate customization of your synthetic population.

#### Person
Representing a citizen of the synthetic population, each `Person` (O01) actor is organised into various roles describing their responsibility for executing several results, including:

|  | Person Actor Role |     | Result Kind     | Description |
| --- | -------------- | --- | --------------- | ----------- |
| A10 | `Participater` | T10 | `Participation` | whether to participate in some gathering
| A11 | `Infector`     | T11 | `Infection`     | whether exposure leads to infection
| A12 | `Disruptor`    | T12 | `Disruption`    | whether to migrate, expand family, etc.
| A13 | `Expresser`    | T13 | `Expression`    | whether to express his current opinion
| A14 | `Impresser`    | T14 | `Impression`    | whether to adopt another opinion
| A15 | `Director`     | T15 | `Redirection`   | whether to change routines, etc.

where the `Director` role (A15) can _self-initiate_ its own `Redirection` results, thus rendering the `Person` a **proactive** agent (as per terminology from [agent-based or individual-based modeling](https://en.wikipedia.org/wiki/Agent-based_model) and [collective intelligence](https://www.cs.vu.nl/~schut/pubs/Schut/2010.pdf) research fields). 

This organisation describes how various events or coordination facts between actors can occur.
For instance, upon invitation (T10/rq) for some `Gathering`, the `Participater` role of the `Person` actor decides whether to evade (T10/dc) or to attend (T10/st), and thereby affect the `Occupancy` of its venue (`Site`).
                                                    
#### Site

:

#### Deme

:

#### Adviser

:

#### Immunizer

:

#### Society

:
