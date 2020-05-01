package Behaviours.Communication;

import Agents.Simple_Cognitif_Agent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;

public class mapExchanging extends SimpleBehaviour {

    public Simple_Cognitif_Agent myAgent;
    public boolean exited = false;
    public boolean received = false;
    public long timeout  = 15;

    public mapExchanging(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }



    public void getExited(){
        MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
        ACLMessage msg = this.myAgent.receive(mt);
        if(msg == null){
            exited = true;
        }
    }


    public void askForMap(){
        //System.out.println("Asking for map . . . ");
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setSender(myAgent.getAID());
        msg.setConversationId("MapExchanging");
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

        String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "DESCRIBE ?n \n" +
                "WHERE    { ?n rdf:type :Node . ?agent rdf:type :Agent . " +
                "OPTIONAL { ?n :knownBy ?a . " +
                "?a rdf:type :Agent . " +
                "?a rdfs:label '%a' . ".replace("%a",myAgent.getLocalName()) +
                "}\n" +
                "FILTER ( !bound(?a) )".replace("%a",myAgent.getLocalName()) +
                "}";
        msg.setContent(request);
        this.myAgent.sendMessage(msg);
    }


    public void askForAgentsKnowledge(){
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.setSender(myAgent.getAID());
        msg.setConversationId("AgentKnowledge");
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

        String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?agent ?p ?o\n" +
                "WHERE    { ?agent rdf:type :Agent . ?agent ?p ?o " +
                "}";
        msg.setContent(request);
        this.myAgent.sendMessage(msg);
    }

    public void getMapSended(){
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("MapExchanging"),MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ;
        ACLMessage msg = myAgent.receive(tmp);
        if(msg != null){
            String sh = msg.getContent();
            //System.out.println("D'ailleurs le dictionnaire des id : "+this.myAgent.conversation_id);

            //System.out.println("Ce que j'ai recu : "+sh);
            if(sh != null)
            {
                Model m = ModelFactory.createDefaultModel();
                StringReader stringReader = new StringReader(sh);
                //System.out.println("Content of the message : "+sh);
                m.read(stringReader,"http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#");
                //m.write(System.out);
                this.myAgent.Kb.beliefs.add(m);
                this.myAgent.Kb.removeDuplicatedCountor();
                this.myAgent.Kb.loadFromKnowledge();
                myAgent.received_map.put(msg.getSender().toString(),10);
                received = true;
            }
            sendExit(msg.getSender());
        }
    }


    public void getAgentKnowledge(){
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("AgentKnowledge"),MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ;
        ACLMessage msg = myAgent.receive(tmp);
        if(msg != null){
            LinkedList<HashMap<String, String>>  sh = null;
            try {
                sh = (LinkedList<HashMap<String, String>> ) msg.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            if(sh != null)
            {
                Model m = ModelFactory.createDefaultModel();
                //System.out.println("Content of the message : "+sh);
                this.myAgent.Kb.knowledgOtherAgents(sh);
                received = true;
            }
            sendExit(msg.getSender());
        }
    }

    public void sendExit(AID receiver){
        ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
        msg.setSender(myAgent.getAID());
        msg.addReceiver(receiver);
    }

    @Override
    public void action() {
        timeout = timeout - 1;
        //System.out.println("Timeout = "+timeout);
        askForMap();


        askForAgentsKnowledge();
        getMapSended();
        getAgentKnowledge();
        getExited();
    }

    @Override
    public boolean done() {
        if(timeout <=0)
        {
            //System.out.println("Done renvoie true ! ");
            //myAgent.removeBehaviour(this);
            return true;
        }
        return exited ;
    }

    public int onEnd(){
        return 1;
    }


}
