package Behaviours;

import Agents.Simple_Cognitif_Agent;
import Knowledge.MapRepresentation;
import Knowledge.PolicyEvaluationHunting;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.SimpleBehaviour;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class followWumpusBehaviour extends SimpleBehaviour {

    public Simple_Cognitif_Agent myAgent;
    private boolean finished = false;


    public followWumpusBehaviour(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
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
            this.myAgent.Kb.addNode(myPosition, MapRepresentation.MapAttribute.closed);
            //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
            Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
            while (iter.hasNext()) {
                String nodeId = iter.next().getLeft();
                this.myAgent.Kb.addNode(nodeId, MapRepresentation.MapAttribute.open);
                this.myAgent.Kb.addEdge(myPosition, nodeId);
                //the node exist, but not necessarily the edge
                this.myAgent.Kb.addEdge(myPosition, nodeId);
                }
            }

    }





    @Override
    public void action() {
        updateNodeStates();
        System.out.println(getAgent().getLocalName() +">" + "Following behaviour..");
        LinkedList<Integer> L = myAgent.seekForStenchInt();
        this.myAgent.Kb.addStenche(this.myAgent.getLocalName(),L);
        this.myAgent.Kb.assertKnowledg(this.myAgent);
        this.myAgent.Kb.updateAgentPosition(this.myAgent.getLocalName(),this.myAgent.getCurrentPosition());
        myAgent.percept();
        if(this.myAgent.policyEvaluationHunting == null)
            this.myAgent.policyEvaluationHunting = new PolicyEvaluationHunting(this.myAgent);

        //Ajout des perceptions
        if(myAgent.canAccessAStench())
        {
            this.myAgent.policyEvaluationHunting.initialiserValeur();
            this.myAgent.policyEvaluationHunting.getNextStep();
            for (LinkedList<Integer> plan:this.myAgent.policyEvaluationHunting.plans)
            {
                if(!plan.isEmpty())
                {
                    String nextStep = plan.getFirst()+"";
                    try
                    {
                        boolean sucess = this.myAgent.moveTo(nextStep);
                    }catch (RuntimeException e){
                        e.printStackTrace();
                    }

                }
            }
        }
        else
        {
            finished = !myAgent.canAccessAStench();
        }
    }

    @Override
    public boolean done() {
        if(finished)
            System.out.println(getAgent().getLocalName() +">" + "Finished = "+finished);
        return finished;
    }

    @Override
    public int onEnd() {
        int code = 3;
        System.out.println(getAgent().getLocalName() +">" + "OnEnd = "+code);
        return code;
    }
}
