package Agents;

import Behaviours.Exploration_Behaviour;
import Behaviours.Global.General_Behaviour;
import Behaviours.Knowledg_Sharing;
import Knowledge.Knowledg_Base;
import Knowledge.MapRepresentation;
import Knowledge.PolicyEvaluationExploration;
import Knowledge.PolicyEvaluationHunting;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb.store.Hash;

import java.io.IOException;
import java.util.*;

public class Simple_Cognitif_Agent extends AbstractDedaleAgent {

    public Knowledg_Base Kb;
    public long stepCountor = 0;
    public HashMap<String, String> conversation_id = new HashMap<>();
    public HashMap<String, HashSet<Integer>> odeurs = new HashMap<>();
    public HashMap<String, Integer> positions_agents = new HashMap<>();
    public HashMap<String,Integer> wumpusPresume = null;
    public HashMap<String,Integer> received_map = null;

    public HashMap<Integer, HashSet<Integer>> successeurs_recus = new HashMap<>();

    public String agentToFollow = null;
    public String objectif = null;

    public PolicyEvaluationHunting policyEvaluationHunting = null;
    public PolicyEvaluationExploration policyEvaluationExploration = null;


    public boolean hasAccessibleStench(){
        HashSet<Integer> cases_odorantes = new HashSet<>();
        for(String s:this.odeurs.keySet())
            cases_odorantes.addAll(this.odeurs.get(s));

        for (String agent:this.positions_agents.keySet()){
            if(!agent.equalsIgnoreCase(this.getLocalName()))
                cases_odorantes.remove(this.positions_agents.get(agent));
        }

        return !cases_odorantes.isEmpty();
    }

    public static boolean containsStench(List<Couple<Observation, Integer>> L){
        for (Couple<Observation, Integer> c:L)
        {
            if(c.getLeft().getName().equalsIgnoreCase("stench"))
                return true;
        }
        return false;
    }

    public LinkedList<String> seekForStench(){
        Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=this.observe().iterator();
        LinkedList<String> L = new LinkedList<>();
        while(iter.hasNext())
        {
            Couple<String, List<Couple<Observation, Integer>>> observation_node = iter.next();
            //System.out.println("Noeud : " + observation_node.getLeft());
            //System.out.println("Liste d'observations : "+ observation_node.getRight()+ " : "+ containsStench(observation_node.getRight()));
            if(containsStench(observation_node.getRight()))
            {
                //this.Kb.addPerceptions(this,observation_node.getLeft(),"Stench");
                L.add(observation_node.getLeft());
            }
        }
        return L;
    }

    public LinkedList<Integer> seekForStenchInt(){
        Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=this.observe().iterator();
        LinkedList<Integer> L = new LinkedList<>();
        while(iter.hasNext())
        {
            Couple<String, List<Couple<Observation, Integer>>> observation_node = iter.next();
            //System.out.println("Noeud : " + observation_node.getLeft());
            //System.out.println("Liste d'observations : "+ observation_node.getRight()+ " : "+ containsStench(observation_node.getRight()));
            if(containsStench(observation_node.getRight()))
            {
                //this.Kb.addPerceptions(this,observation_node.getLeft(),"Stench");
                L.add(Integer.valueOf(observation_node.getLeft()));
            }
        }
        return L;
    }

    public boolean seekForWumpus(){
        if(!seekForStench().isEmpty())
            return true;
        Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=this.observe().iterator();
        LinkedList<String> L = new LinkedList<>();
        String query2  = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?lab1 \n" +
                "WHERE    { "+
                " ?n1 rdf:type :Node . " +
                " ?a1 :indicateStenche ?n1 . " +
                " ?a1 rdf:type :Agent ." +
                " ?n1 rdfs:label ?lab1 . " +
                "}";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(this.Kb.beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs = conn.query(query2).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    //System.out.println("Récupérer : "+qs.get("lab1"));
                    L.add(qs.get("lab1").toString());
                }
            });
        }
        return !L.isEmpty();
    }


    public boolean presumedWumpus(Integer pos){
        HashSet<Integer> positions_odorantes = new HashSet<>();
        HashSet<Integer> positions_adverses = new HashSet<>();
        for (String agent:odeurs.keySet())
        {
            positions_odorantes.addAll(odeurs.get(agent));
        }
        for(String agent:positions_agents.keySet())
        {
            positions_adverses.add(positions_agents.get(agent));
        }
        //System.out.println(this.myAgent.getLocalName() + ">" +"Positions adverse : "+positions_adverses);
        //System.out.println(this.myAgent.getLocalName() + ">" +"Positions odorantes : "+positions_odorantes);
        //System.out.println(this.myAgent.getLocalName() + ">" +"Position a tester : "+pos);
        return !positions_adverses.contains(pos) && positions_odorantes.contains(Integer.parseInt(getCurrentPosition()));
    }

    public String getPresumedWumpus(){
        LinkedList<String> S = new LinkedList(wumpusPresume.values());
        if(!S.isEmpty())
            return S.get(0);
        else return null;
    }


    public void percept(){
        if(this.Kb == null)
            this.Kb = new Knowledg_Base();
        LinkedList<Integer> L = seekForStenchInt();
        odeurs.put(getLocalName(),new HashSet<>(L));
        positions_agents.put(getLocalName(), Integer.valueOf(getCurrentPosition()));
    }


    public String wumpusFound(){
        if(wumpusPresume == null)
            wumpusPresume = new HashMap<>();
        for (String agent:wumpusPresume.keySet())
            if(wumpusPresume.get(agent) != -1)
                return agent;
            return null;
    }

    public void updateNodeStates() {
        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this).getCurrentPosition();

        if (myPosition != null) {
            //List of observable from the agent's current position
            List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this).observe();//myPosition
            /**
             * Just added here to let you see what the agent is doing, otherwise he will be too quick
             */
            try {
                this.doWait(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.Kb.addNode(myPosition, MapRepresentation.MapAttribute.closed);
            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
            while (iter.hasNext()) {
                String nodeId = iter.next().getLeft();
                this.Kb.addNode(nodeId, MapRepresentation.MapAttribute.open);
                this.Kb.addEdge(myPosition, nodeId);
                //the node exist, but not necessarily the edge
                this.Kb.addEdge(myPosition, nodeId);
            }
        }

    }



    public void ecouterPositions() throws UnreadableException {
        MessageTemplate tmp = MessageTemplate.and(MessageTemplate.MatchConversationId("Wumpus"),MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE));
        ACLMessage msg = this.receive(tmp);
        if(msg != null)
        {
            LinkedList<Object> L = (LinkedList<Object>) msg.getContentObject();
            String s = (String) L.get(0);
            LinkedList<Integer> stenches = (LinkedList<Integer>) L.get(1);
            String agent = msg.getSender().getLocalName();
            this.odeurs.put(agent,new HashSet<Integer>(stenches));
            this.positions_agents.put(agent, Integer.valueOf(s));
            //System.out.println(getLocalName() + " > Liste reçu : "+L);
            if(((Integer) L.get(2) )!= -1)
            {
                if(this.wumpusPresume == null)
                    this.wumpusPresume = new HashMap<>();
                this.wumpusPresume.put(msg.getSender().getLocalName(),(Integer) L.get(2));
                System.out.println("J'entend que le wumpus aurait bougé : "+this.wumpusPresume);
            }
            HashMap<Integer, HashSet<Integer>> successeurs = (HashMap<Integer, HashSet<Integer>>) L.get(3);
            successeurs_recus.putAll(successeurs);
        }
    }

    public void crierPositions(){
        String position = this.getCurrentPosition();
        LinkedList<Integer> stenches = seekForStenchInt();
        ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
        msg.setConversationId("Wumpus");
        msg.setSender(this.getAID());
        DFAgentDescription dfd = new DFAgentDescription();
        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            for (int i=0;i<result.length;i++)
            {
                if(!result[i].getName().getLocalName().equals(this.getLocalName())) {
                    msg.addReceiver(result[i].getName());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        LinkedList<Object> L = new LinkedList<>();
        L.add(position);
        L.add(stenches);
        if(this.wumpusPresume != null && this.wumpusPresume.get(getLocalName())!=null)
            L.add(this.wumpusPresume.get(getLocalName()));
        else
            L.add(-1);
        if(policyEvaluationExploration == null)
            policyEvaluationExploration = new PolicyEvaluationExploration(this);
        policyEvaluationExploration.updateSuccesseurs();
        L.add(policyEvaluationExploration.successeurs);
        try {
            msg.setContentObject(L);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.sendMessage(msg);
    }


    public boolean canAccessAStench(){
        if(policyEvaluationHunting == null)
            policyEvaluationHunting = new PolicyEvaluationHunting(this);
        System.out.println(getLocalName() +">" + "Reponse du hasAccessibleStench : "+ hasAccessibleStench());
        if(hasAccessibleStench()){
            policyEvaluationHunting.initialiserValeur();
            policyEvaluationHunting.updateSuccesseurs();
            policyEvaluationHunting.calculerPlans();
            policyEvaluationHunting.genererPlans(Integer.parseInt(getCurrentPosition()));
            boolean suitable_plan = false;
            System.out.println("Calcul des plans suitable parmi : " + policyEvaluationHunting.plans);
            for (LinkedList<Integer> plan:policyEvaluationHunting.plans)
            {
                if(plan.size() > 0 && !plan.contains(Integer.parseInt(getCurrentPosition())))
                {
                    suitable_plan = true;
                    break;
                }
            }
            System.out.println("Donc réponse de  : " + suitable_plan);
            return suitable_plan;
        }
        else
            return false;


    }

    protected void setup(){
        super.setup();
        List<Behaviour> lb=new ArrayList<Behaviour>();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName( getAID() );

        try {
            DFService.register( this, dfd );
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        /************************************************
         *
         * ADD the Default.behaviours of the Dummy Moving Agent
         *
         ************************************************/
        //lb.add(new Exploration_Behaviour(this,this.Kb));
        //lb.add(new Knowledg_Sharing(this,500));
        lb.add(new General_Behaviour(this));
        /***
         * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
         */
        addBehaviour(new startMyBehaviours(this,lb));
        System.out.println("the  agent "+this.getLocalName()+ " is started");

    }


}
