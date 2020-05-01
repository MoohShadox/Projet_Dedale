package Behaviours.Communication;

import Agents.Simple_Cognitif_Agent;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.jena.ext.com.google.common.base.Ticker;
import org.apache.jena.tdb.store.Hash;

import java.util.HashMap;

public class callingBehaviour extends SimpleBehaviour {

    public Simple_Cognitif_Agent myAgent;
    public boolean answered = false;
    public boolean getAnswer = false;

    public callingBehaviour(Agent a) {
        myAgent = (Simple_Cognitif_Agent) a;

    }

    public void sendCall(){
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        msg.setSender(this.myAgent.getAID());
        DFAgentDescription dfd = new DFAgentDescription();
        try {
            DFAgentDescription[] result = DFService.search(this.myAgent, dfd);
            for (int i=0;i<result.length;i++)
            {
                if(!result[i].getName().getLocalName().equals(myAgent.getLocalName())) {
                    msg.addReceiver(result[i].getName());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        this.myAgent.sendMessage(msg);
    }


    public void answerCall(){
        MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        ACLMessage msg = myAgent.receive(tmp);
        if(msg != null){
            if(myAgent.conversation_id.containsKey(msg.getSender().getLocalName()))
                return;
            String identifiant = msg.getSender().getLocalName() + "->" + this.myAgent.getLocalName();
            myAgent.conversation_id.put(msg.getSender().getLocalName(),identifiant);
            ACLMessage response = new ACLMessage(ACLMessage.CONFIRM);
            response.setConversationId(identifiant);
            response.setSender(this.myAgent.getAID());
            response.addReceiver(msg.getSender());
            answered = true;
        }
    }

    public void getAnswers(){
        MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
        ACLMessage msg = myAgent.receive(tmp);
        if(msg != null){
            if(myAgent.conversation_id.containsKey(msg.getSender().getLocalName()))
                return;
            String identifiant = this.myAgent.getLocalName() + "->" + msg.getSender().getLocalName() ;
            myAgent.conversation_id.put(msg.getSender().getLocalName(),identifiant);
            getAnswer = true;
        }
    }

    @Override
    public int onEnd() {
        int val = (getAnswer || answered) ? 1 : 0;
        if(getAnswer || answered)
        {
            //System.out.println("On end de 1 avec getanswer="+ getAnswer+" answered = "+answered);

        }
        return (getAnswer || answered) ? 1 : 0;
    }

    @Override
    public void action() {
        if(myAgent.conversation_id == null)
            myAgent.conversation_id = new HashMap<>();
        myAgent.conversation_id.clear();
        sendCall();
        answerCall();
        getAnswers();
    }

    @Override
    public boolean done() {
        return getAnswer || answered;
    }

}
