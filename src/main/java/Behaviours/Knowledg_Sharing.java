package Behaviours;

import Agents.Simple_Cognitif_Agent;
import Knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.base.Sys;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDF2;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.system.Txn;
import org.graphstream.graph.implementations.SingleGraph;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.Scanner;

public class Knowledg_Sharing extends TickerBehaviour {

    private Simple_Cognitif_Agent myAgent;

    public Knowledg_Sharing(Agent a, long period) {
        super(a, period);
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
            System.out.println("Réception de quelque chose : ");
            Model m = ModelFactory.createDefaultModel();
            StringReader stringReader = new StringReader(sh);
            m.read(stringReader,"http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#");
            //m.write(System.out);
            this.myAgent.Kb.beliefs.union(m);
            this.myAgent.Kb.loadFromKnowledge();
        }
    }


    @Override
    protected void onTick() {
        sendKnowledge();
        receiveKnowledge();
    }
}
