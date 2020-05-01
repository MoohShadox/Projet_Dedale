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

public class perceptionExchanging extends SimpleBehaviour {

    public Simple_Cognitif_Agent myAgent;
    public boolean exited = false;
    public boolean received = false;
    public long timeout  = 15;

    public perceptionExchanging(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }



    public void getExited(){
        MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
        ACLMessage msg = this.myAgent.receive(mt);
        if(msg == null){
            exited = true;
        }
    }

    public void askForMapInPart(){
        //System.out.println("Asking for map . . . ");
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setSender(myAgent.getAID());
        msg.setConversationId("MiniMapExchange");
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
                "WHERE    { ?n rdf:type :Node . ?agent rdf:type :Agent . ?agent :indicateStenches ?n" +
                "OPTIONAL { ?n :knownBy ?a . " +
                "?a rdf:type :Agent . " +
                "?a rdfs:label '%a' . ".replace("%a",myAgent.getLocalName()) +
                "}\n" +
                "FILTER ( !bound(?a) )".replace("%a",myAgent.getLocalName()) +
                "}";
        msg.setContent(request);
        this.myAgent.sendMessage(msg);
    }

    public void getMapSended(){
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("MiniMapExchange"),MessageTemplate.MatchPerformative(ACLMessage.INFORM));
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
                received = true;
            }
            sendExit(msg.getSender());
        }
    }

    public void askForPerceptions(){
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.setSender(myAgent.getAID());
        msg.setConversationId("Perceptions");

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
         String request ="PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                 "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                 "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                 "SELECT ?lab1 \n" +
                 "WHERE    { "+
                 " ?n1 rdf:type :Node . " +
                 " ?a1 :indicateStenche ?n1 . " +
                 " ?a1 rdf:type :Agent ." +
                 " ?n1 rdfs:label ?lab1 . " +
                 "}";
        msg.setContent(request);
        //System.out.println(myAgent.getLocalName()+ " is Asking for perceptions . . with request : ");
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


    public void getAgentKnowledge(){
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("AgentKnowledge"),MessageTemplate.MatchPerformative(ACLMessage.INFORM));
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
                this.myAgent.Kb.knowledgOtherAgents(sh);
                received = true;
            }
            sendExit(msg.getSender());
        }
    }


    public void getPerceptions(){
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("Perceptions"),MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msg = myAgent.receive(tmp);
        //System.out.println(myAgent.getLocalName()+" is Checking answers for perceptions . . ");
        if(msg != null){
            //System.out.println("ah j'ai reçu une réponse de perception ! ");
            LinkedList<HashMap<String, String>> sh = null;
            try {
                sh = (LinkedList<HashMap<String, String>> ) msg.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            if(sh != null)
            {
                LinkedList<String> nodes = new LinkedList<>();
                for (HashMap<String,String> result:sh)
                {
                    String lab = result.get("lab1");
                    nodes.add(lab);
                }
                if(!nodes.isEmpty())
                {
                    LinkedList<Integer> N = new LinkedList<>();
                    for (String s:nodes)
                        N.add(Integer.valueOf(s));
                    myAgent.Kb.addStenche(msg.getSender().getLocalName(),N);
                }
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
        askForMapInPart();
        getMapSended();
        askForPerceptions();
        getPerceptions();
        askForAgentsKnowledge();
        getAgentKnowledge();
        getExited();
    }

    @Override
    public boolean done() {
        return false;
    }

    public int onEnd(){
        return 0;
    }

}
