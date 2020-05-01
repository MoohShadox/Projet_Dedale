package Behaviours.Communication;

import Agents.Simple_Cognitif_Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class conversationMode extends SimpleBehaviour {


    public Simple_Cognitif_Agent myAgent;
    public boolean exited = false;
    public long timeout  = 15;

    public conversationMode(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }



    public void answerDescribe(){
        MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage msg = this.myAgent.receive(tmp);
        if(msg != null ) {
            Dataset ds = DatasetFactory.create(myAgent.Kb.beliefs);
            String request = msg.getContent();
            ACLMessage answer = new ACLMessage(ACLMessage.INFORM);
            answer.setConversationId(msg.getConversationId());
            try (RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(myAgent.Kb.beliefs))) {
                Txn.executeWrite(conn, () -> {
                    Model rs = conn.queryDescribe(request);
                    //rs.write(System.out,"TURTLE");
                    StringWriter stringWriter = new StringWriter();
                    rs.write(new PrintWriter(stringWriter));
                    answer.setContent(stringWriter.toString());
                });
            }
            answer.setSender(myAgent.getAID());
            answer.addReceiver(msg.getSender());
            this.myAgent.sendMessage(answer);
        }
    }

    public void answerSelect(){
        MessageTemplate tmp = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
        ACLMessage msg = this.myAgent.receive(tmp);
        //System.out.println("Vérification de la présence d'une requète");
        if(msg != null ) {
            //System.out.println(myAgent.getLocalName() + "a reçu la requète suivante de la part de :" +myAgent.getLocalName()+ " avec l'id de conv suivant : "+msg.getConversationId());
            Dataset ds = DatasetFactory.create(myAgent.Kb.beliefs);
            String request = msg.getContent();
            ACLMessage answer = new ACLMessage(ACLMessage.INFORM);
            answer.setConversationId(msg.getConversationId());
            try (RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(myAgent.Kb.beliefs))) {
                Txn.executeWrite(conn, () -> {
                    ResultSet rs = conn.query(request).execSelect();
                    //System.out.println("Execution de la requète : "+request);
                    //rs.write(System.out,"TURTLE");
                    LinkedList<HashMap<String, String>> lstm = new LinkedList<>();
                    while(rs.hasNext())
                    {
                        QuerySolution qs = rs.next();
                        Iterator<String> it = qs.varNames();
                        HashMap<String, String> H = new HashMap();
                        while (it.hasNext())
                        {
                            String var = it.next();
                            H.put(var,qs.get(var).toString());
                        }
                        lstm.add(H);
                    }
                    try {
                        answer.setContentObject(lstm);
                        //System.out.println("Contenu de la réponse renvoyée : "+lstm);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            answer.setSender(myAgent.getAID());
            answer.addReceiver(msg.getSender());

            this.myAgent.sendMessage(answer);
        }
    }


    @Override
    public void action() {
        answerDescribe();
        answerSelect();
        timeout = timeout - 1;
    }

    @Override
    public boolean done() {
        if(timeout <=0)
            return true;
        return false;
    }

    public int onEnd(){
        return 0;
    }
}
