package Behaviours.Communication;

import Agents.Simple_Cognitif_Agent;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class crierAuWumpus extends TickerBehaviour {


    Simple_Cognitif_Agent myAgent;
    public crierAuWumpus(Simple_Cognitif_Agent a) {
        super(a, 100);
        myAgent = a;
        System.out.println("J'ai crée Crie au Wumpus");
    }

    public void ecouterPositions() throws UnreadableException {
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("Wumpus"),MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE));
        ACLMessage msg = this.myAgent.receive(tmp);
        if(msg != null)
        {
            LinkedList<Object> L = (LinkedList<Object>) msg.getContentObject();
            String s = (String) L.get(0);
            LinkedList<Integer> stenches = (LinkedList<Integer>) L.get(1);
            String agent = msg.getSender().getLocalName();
            this.myAgent.odeurs.put(agent,new HashSet<Integer>(stenches));
            this.myAgent.positions_agents.put(agent, Integer.valueOf(s));
            //System.out.println(getLocalName() + " > Liste reçu : "+L);
            if(((Integer) L.get(2) )!= -1)
            {
                if(this.myAgent.wumpusPresume == null)
                    this.myAgent.wumpusPresume = new HashMap<>();
                this.myAgent.wumpusPresume.put(msg.getSender().getLocalName(),(Integer) L.get(2));
                System.out.println("J'entend que le wumpus aurait bougé : "+this.myAgent.wumpusPresume);
            }
            HashMap<Integer, HashSet<Integer>> successeurs = (HashMap<Integer, HashSet<Integer>>) L.get(3);
            myAgent.successeurs_recus.putAll(successeurs);
        }
    }

    public void crierPositions(){

        String position = this.myAgent.getCurrentPosition();
        LinkedList<Integer> stenches = myAgent.seekForStenchInt();
        ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
        msg.setConversationId("Wumpus");
        msg.setSender(this.myAgent.getAID());
        DFAgentDescription dfd = new DFAgentDescription();
        try {
            DFAgentDescription[] result = DFService.search(this.myAgent, dfd);
            for (int i=0;i<result.length;i++)
            {
                if(!result[i].getName().getLocalName().equals(this.myAgent.getLocalName())) {
                    msg.addReceiver(result[i].getName());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        LinkedList<Object> L = new LinkedList<>();
        L.add(position);
        L.add(stenches);
        if(this.myAgent.wumpusPresume != null && this.myAgent.wumpusPresume.get(myAgent.getLocalName())!=null)
            L.add(this.myAgent.wumpusPresume.get(this.myAgent.getLocalName()));
        else
            L.add(-1);
        this.myAgent.policyEvaluationExploration.updateSuccesseurs();
        L.add(this.myAgent.policyEvaluationExploration.successeurs);
        try {
            msg.setContentObject(L);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.myAgent.sendMessage(msg);
    }

    public static boolean containsStench(List<Couple<Observation, Integer>> L){
        for (Couple<Observation, Integer> c:L)
        {
            if(c.getLeft().getName().equalsIgnoreCase("stench"))
                return true;
        }
        return false;
    }

    public LinkedList<Integer> seekForStench()  {
        Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=myAgent.observe().iterator();
        LinkedList<Integer> L = new LinkedList<>();
        while(iter.hasNext())
        {
            Couple<String, List<Couple<Observation, Integer>>> observation_node = iter.next();
            //System.out.println("Noeud : " + observation_node.getLeft());
            //ystem.out.println("Liste d'observations : "+ observation_node.getRight()+ " : "+ containsStench(observation_node.getRight()));
            if(containsStench(observation_node.getRight()))
            {
                this.myAgent.Kb.addPerceptions(myAgent,observation_node.getLeft(),"Stench");
                this.myAgent.stepCountor = this.myAgent.stepCountor + 1;
                try {
                    this.myAgent.Kb.beliefs.write(new FileOutputStream("/home/mohamed/IdeaProjects/Dedale_Cognitif/src/main/java/Knowledge/output.rdf"),"TURTLE");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                L.add(Integer.valueOf(observation_node.getLeft()));
            }
        }
        return L;
    }





    @Override
    protected void onTick() {
        System.out.println("Je crie au wumpus");
        crierPositions();
        try {
           ecouterPositions();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
    }
}
