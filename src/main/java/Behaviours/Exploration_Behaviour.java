package Behaviours;

import Agents.Simple_Cognitif_Agent;
import Knowledge.Knowledg_Base;
import Knowledge.MapRepresentation;
import Knowledge.PolicyEvaluationExploration;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.UnreadableException;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb.store.Hash;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

public class Exploration_Behaviour extends OneShotBehaviour {

    /**
     * Current knowledge of the agent regarding the environment
     */
    private Knowledg_Base myMap;
    public Simple_Cognitif_Agent myAgent;
    public boolean wumpusFound = false;
    public boolean someoneFoundTheWumpus = false;

    /**
     * Nodes known but not yet visited
     */
    private List<String> openNodes;
    /**
     * Visited nodes
     */
    private Set<String> closedNodes;
    private boolean finished;


    public Exploration_Behaviour(final AbstractDedaleAgent myagent, Knowledg_Base myMap) {
        super(myagent);
        myAgent = (Simple_Cognitif_Agent) myagent;
        this.myMap=myMap;
        this.openNodes=new ArrayList<String>();
        this.closedNodes=new HashSet<String>();
    }

    public String processPlans(){
        if(this.myAgent.policyEvaluationExploration == null)
        {
            this.myAgent.policyEvaluationExploration = new PolicyEvaluationExploration(this.myAgent);
        }
        updateNodeStates();
        String next_step = this.myAgent.policyEvaluationExploration.getNextStep();
        //System.out.println("Next move : "+next_step);
        //Scanner sc = new Scanner(System.in);
        //sc.nextLine();
        return next_step;
    }



    public void perceptionUpdates(String nextNode){
        this.myAgent.Kb.updateCount(this.myAgent,this.myAgent.stepCountor);
        this.myAgent.stepCountor = this.myAgent.stepCountor + 1;
        this.myAgent.Kb.assertKnowledg(myAgent);
        this.myAgent.Kb.updateAgentPosition(myAgent.getLocalName(),nextNode);
        myAgent.percept();
    }

    public void updateNodeStates() {
        //0) Retrieve the current position
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        if (myPosition != null) {
            //List of observable from the agent's current position
            List<Couple<String, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();//myPosition

            /**
             * Just added here to let you see what the agent is doing, otherwise he will be too quick
             */
            try {
                this.myAgent.doWait(200);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //1) remove the current node from openlist and add it to closedNodes.
            this.closedNodes.add(myPosition);
            this.openNodes.remove(myPosition);

            this.myMap.addNode(myPosition, MapRepresentation.MapAttribute.closed);

            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
            while (iter.hasNext()) {
                String nodeId = iter.next().getLeft();
                if (!this.closedNodes.contains(nodeId)) {
                    if (!this.openNodes.contains(nodeId)) {
                        this.openNodes.add(nodeId);
                        this.myMap.addNode(nodeId, MapRepresentation.MapAttribute.open);
                        this.myMap.addEdge(myPosition, nodeId);
                    } else {
                        //the node exist, but not necessarily the edge
                        this.myMap.addEdge(myPosition, nodeId);
                    }
                }
            }

        }
    }

    public static boolean containsStench(List<Couple<Observation, Integer>> L){
        for (Couple<Observation, Integer> c:L)
        {
            if(c.getLeft().getName().equalsIgnoreCase("stench"))
                return true;
        }
        return false;
    }

    @Override
    public void action() {
        myAgent.percept();
        if(this.myAgent.wumpusPresume == null)
            this.myAgent.wumpusPresume = new HashMap<>();
        this.myAgent.wumpusPresume.clear();
        //this.myAgent.positions_agents.clear();
        wumpusFound = false;
        someoneFoundTheWumpus = false;
        if(this.myMap==null)
        {
            this.myMap= new Knowledg_Base();
            this.myAgent.Kb = this.myMap;
            this.myMap.updateAgents(myAgent);
        }
        if(myAgent.received_map == null)
            myAgent.received_map = new HashMap<>();

        updateNodeStates();
        String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        perceptionUpdates(myPosition);

        myAgent.crierPositions();
        try {
            myAgent.ecouterPositions();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }

        //3) while openNodes is not empty, continues.
        if (this.openNodes.isEmpty()){
            //Explo finished
            finished=true;
            //TODO : Cas particulier a gérer
            System.out.println("Exploration successufully done, behaviour removed.");
        }else{
                String nextNode = processPlans();
            boolean success = false;
                try{
                     success = ((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
                     Scanner sc = new Scanner(System.in);
                    wumpusFound = myAgent.hasAccessibleStench();
                    try {
                        myAgent.ecouterPositions();
                        if(this.myAgent.wumpusFound()!=null)
                        {
                            System.out.println(this.myAgent.getLocalName() + ">" + "J'explore mais apparement quelqu'un d'autre a trouvé le Wumpus en : "+this.myAgent.wumpusPresume);
                            someoneFoundTheWumpus = true;
                            wumpusFound = false;
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                     //sc.nextLine();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                while (!success) {
                    wumpusFound = myAgent.hasAccessibleStench();
                    if(wumpusFound)
                        break;
                    try {
                        myAgent.ecouterPositions();
                        if(!this.myAgent.wumpusPresume.keySet().isEmpty())
                        {
                            System.out.println(this.myAgent.getLocalName() + ">" + "J'explore mais apparement quelqu'un d'autre a trouvé le Wumpus en : "+this.myAgent.wumpusPresume);
                            someoneFoundTheWumpus = true;
                            wumpusFound = false;
                            break;
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                    this.myAgent.policyEvaluationExploration.penality.clear();
                    this.myAgent.policyEvaluationExploration.addPenalised(nextNode);
                    nextNode = processPlans();
                    try {
                        success = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNode);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
        }

    }

    @Override
    public int onEnd() {
        System.out.println(myAgent.getLocalName()+"> Exploring returns "+(someoneFoundTheWumpus ? 20 : wumpusFound ? 10 : 0));
        System.out.println(myAgent.getLocalName()+"> Exploring returns is because : "+myAgent.wumpusPresume);
        return (someoneFoundTheWumpus ? 20 : wumpusFound ? 10 : 0);
        //if(wumpusFound)
        //    return 3;
        //if(someoneFoundTheWumpus)
        //    return 4;
        //return 0;
    }
}
