package Behaviours.Communication;

import Agents.Simple_Cognitif_Agent;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class Conversation extends SimpleBehaviour {


    private Simple_Cognitif_Agent myAgent;
    public boolean received  = false;

    public Conversation(Agent a, long period) {
        super(a);
        myAgent = (Simple_Cognitif_Agent) a;
    }

    public void sendKnowledge(){
        ACLMessage ping = new ACLMessage(ACLMessage.CFP);
        ping.setSender(myAgent.getAID());
        StringWriter stringWriter = new StringWriter();
        myAgent.Kb.beliefs.write(new PrintWriter(stringWriter));
        ping.setContent(stringWriter.toString());
        DFAgentDescription dfd = new DFAgentDescription();
        try {
            DFAgentDescription[] result = DFService.search(this.myAgent, dfd);
            for (int i=0;i<result.length;i++)
            {
                if(!result[i].getName().getLocalName().equals(myAgent.getLocalName())) {
                    ping.addReceiver(result[i].getName());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        myAgent.sendMessage(ping);
    }

    public void receiveKnowledge(){
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        ACLMessage msg = this.myAgent.receive(mt);
        if(msg != null){
            String sh = msg.getContent();
            //System.out.println("D'ailleurs le dictionnaire des id : "+this.myAgent.conversation_id);
            //System.out.println("Réception de quelque chose : ");
            //System.out.println("Ce que j'ai recu : "+sh);
            if(sh != null)
            {
                Model m = ModelFactory.createDefaultModel();
                StringReader stringReader = new StringReader(sh);
                //System.out.println("Content of the message : "+sh);
                //System.out.println("RECEPTION");
                m.read(stringReader,"http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#");
                m.write(System.out);
                this.myAgent.Kb.beliefs.add(m);
                this.myAgent.Kb.loadFromKnowledge();
                received = true;
            }

        }
    }



    @Override
    public void action() {
        sendKnowledge();
        receiveKnowledge();
    }


    @Override
    public boolean done() {
        return received;
    }

    @Override
    public int onEnd() {
        return 1;
    }
}
