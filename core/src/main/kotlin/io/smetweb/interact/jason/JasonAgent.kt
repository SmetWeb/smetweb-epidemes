package io.smetweb.interact.jason

import jason.asSemantics.Agent
import java.util.*

/**
 * [JasonAgent] maps the [Jason](https://github.com/jason-lang/jason)'s
 * [KQML](https://en.wikipedia.org/wiki/Knowledge_Query_and_Manipulation_Language)
 * and [FIPA-ACL](https://en.wikipedia.org/wiki/Agent_Communications_Language)
 * performatives onto respective [io.smetweb.fact.FactKind] types of the
 * [DEMO](https://en.wikipedia.org/wiki/Design_%26_Engineering_Methodology_for_Organizations)
 * enterprise ontology, all of which are inspired by the
 * [language/action perspective](https://en.wikipedia.org/wiki/Language/action_perspective).
 */
open class JasonAgent: Agent() {


    // KQML performatives not available in FIPA-ACL
    val UNTELL = 1001
    val ASKALL = 1002
    val UNACHIEVE = 1003
    val TELLHOW = 1004
    val UNTELLHOW = 1005
    val ASKHOW = 1006

//    private val serialVersionUID = 1L

//    protected var logger: Logger = jade.util.Logger.getMyLogger(this.javaClass.name)

    private var rwid = 0 // reply-with counter

    protected var running = true

    protected var conversationIds: MutableMap<String, String> = HashMap()

//    fun doDelete() {
//        running = false
//        super.doDelete()
//    }
//
//    fun isRunning(): Boolean {
//        return running
//    }
//
//    fun incReplyWithId(): Int {
//        return rwid++
//    }
//
//    @Throws(Exception::class)
//    fun sendMsg(m: Message) {
//        val acl: ACLMessage = jasonToACL(m)
//        acl.addReceiver(AID(m.getReceiver(), AID.ISLOCALNAME))
//        if (m.getInReplyTo() != null) {
//            val convid = conversationIds[m.getInReplyTo()]
//            if (convid != null) {
//                acl.setConversationId(convid)
//            }
//        }
//        if (logger.isLoggable(Level.FINE)) logger.fine("Sending message: $acl")
//        send(acl)
//    }
//
//    fun broadcast(m: Message) {
//        addBehaviour(object : OneShotBehaviour() {
//            private val serialVersionUID = 1L
//            fun action() {
//                try {
//                    val acl: ACLMessage = jasonToACL(m)
//                    addAllAgsAsReceivers(acl)
//                    send(acl)
//                } catch (e: Exception) {
//                    logger.log(Level.SEVERE, "Error in broadcast of $m", e)
//                }
//            }
//        })
//    }
//
//    fun putConversationId(replyWith: String, mId: String) {
//        conversationIds[replyWith] = mId
//    }
//
//    protected fun ask(m: ACLMessage): ACLMessage? {
//        try {
//            val waitingRW = "id" + incReplyWithId()
//            m.setReplyWith(waitingRW)
//            send(m)
//            val r: ACLMessage = blockingReceive(MessageTemplate.MatchInReplyTo(waitingRW), 5000)
//            if (r != null) return r else logger.warning("ask timeout for " + m.getContent())
//        } catch (e: Exception) {
//            logger.log(Level.SEVERE, "Error waiting message.", e)
//        }
//        return null
//    }
//
//
//    @Throws(Exception::class)
//    fun addAllAgsAsReceivers(m: ACLMessage) {
//        // get all agents' name
//        val template = DFAgentDescription()
//        val sd = ServiceDescription()
//        sd.setType("jason")
//        sd.setName(JadeAgArch.dfName)
//        template.addServices(sd)
//        val ans: Array<DFAgentDescription> = DFService.search(this, template)
//        for (i in ans.indices) {
//            if (!ans[i].getName().equals(getAID())) {
//                m.addReceiver(ans[i].getName())
//            }
//        }
//    }
//
//    @Throws(IOException::class)
//    protected fun jasonToACL(m: Message): ACLMessage {
//        val acl: ACLMessage = kqmlToACL(m.getIlForce())
//        // send content as string if it is a Term/String (it is better for interoperability)
//        if (m.getPropCont() is Term || m.getPropCont() is String) {
//            acl.setContent(m.getPropCont().toString())
//        } else {
//            acl.setContentObject(m.getPropCont() as Serializable)
//        }
//        acl.setReplyWith(m.getMsgId())
//        acl.setLanguage("AgentSpeak")
//        if (m.getInReplyTo() != null) {
//            acl.setInReplyTo(m.getInReplyTo())
//        }
//        return acl
//    }
//
//    fun kqmlToACL(p: String): ACLMessage {
//        if (p == "tell") {
//            return ACLMessage(ACLMessage.INFORM)
//        } else if (p == "askOne") {
//            return ACLMessage(ACLMessage.QUERY_REF)
//        } else if (p == "achieve") {
//            return ACLMessage(ACLMessage.REQUEST)
//        } else if (p.toLowerCase() == "accept_proposal") {
//            return ACLMessage(ACLMessage.ACCEPT_PROPOSAL)
//        } else if (p.toLowerCase() == "reject_proposal") {
//            return ACLMessage(ACLMessage.REJECT_PROPOSAL)
//        } else if (p.toLowerCase() == "query_if") {
//            return ACLMessage(ACLMessage.QUERY_IF)
//        } else if (p.toLowerCase() == "inform_if") {
//            return ACLMessage(ACLMessage.INFORM_IF)
//        }
//        val perf: Int = ACLMessage.getInteger(p)
//        if (perf == -1 || perf == ACLMessage.NOT_UNDERSTOOD) {
//            val m = ACLMessage(ACLMessage.INFORM_REF)
//            m.addUserDefinedParameter("kqml-performative", p)
//            return m
//        }
//        return ACLMessage(perf)
//    }
//
//    fun aclPerformativeToKqml(m: ACLMessage): String? {
//        when (m.getPerformative()) {
//            ACLMessage.INFORM -> return "tell"
//            ACLMessage.QUERY_REF -> return "askOne"
//            ACLMessage.REQUEST -> return "achieve"
//            ACLMessage.INFORM_REF -> {
//                val kp: String = m.getUserDefinedParameter("kqml-performative")
//                if (kp != null) {
//                    return kp
//                }
//            }
//        }
//        return ACLMessage.getPerformative(m.getPerformative()).toLowerCase().replaceAll("-", "_")
//    }

}