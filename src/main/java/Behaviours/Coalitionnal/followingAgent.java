package Behaviours.Coalitionnal;

import Agents.Simple_Cognitif_Agent;
import Knowledge.Planificateur_Simple;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.UnreadableException;
import java.util.LinkedList;
import java.util.Scanner;

public class followingAgent extends SimpleBehaviour {

    Simple_Cognitif_Agent myAgent;
    Planificateur_Simple planificateur_simple;
    boolean cantFollow = false;
    Integer objectif = null;
    LinkedList<Integer> contrainte  = new LinkedList<>();
    LinkedList<Integer> plan_calcule = new LinkedList<>();

    public followingAgent(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }


    @Override
    public void action() {
        System.out.println(myAgent.getLocalName() + " > Trying tu rush the Wumpus en : " + myAgent.wumpusPresume.get(myAgent.wumpusFound()));
        myAgent.crierPositions();
        try {
            myAgent.ecouterPositions();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        if(planificateur_simple == null)
            planificateur_simple = new Planificateur_Simple(myAgent);

        if(this.objectif !=null && this.objectif.toString().equalsIgnoreCase(myAgent.getCurrentPosition()))
        {
            this.objectif = null;
            this.contrainte = new LinkedList<>();
        }
        planificateur_simple.successeurs.putAll(this.myAgent.successeurs_recus);
        Integer p;
        p= myAgent.wumpusPresume.get(myAgent.wumpusFound());
        //p = new LinkedList<>(myAgent.policyEvaluationHunting.successeurs.get(p)).get(0);
        planificateur_simple.getNextStep(p,myAgent.positions_agents.get(myAgent.wumpusFound()));
        System.out.println(myAgent.getLocalName() + " > "+ "Postions des agents  : "+myAgent.positions_agents);
        System.out.println(myAgent.getLocalName() + " > "+ "Plan calculé pour aller vers l'agent vers l'agent : "+planificateur_simple.plans);
        LinkedList<Integer> L = planificateur_simple.plans.getFirst();
        plan_calcule = L;
        if(!L.isEmpty()) {
            String step = L.getFirst() + "";
            myAgent.moveTo(step);
            Scanner sc = new Scanner(System.in);
            System.out.println(myAgent.getLocalName() + " > " + "Next step vers le wumpus : " + step);
        }
        else{
            planificateur_simple.getNextStep(p);
            LinkedList<Integer> L2 = planificateur_simple.plans.getFirst();
            plan_calcule = L2;
            if(!L2.isEmpty()) {
                String step = L2.getFirst() + "";
                myAgent.moveTo(step);
                Scanner sc = new Scanner(System.in);
                System.out.println(myAgent.getLocalName() + " > " + "Finalement je rush l'agent Next step vers l'agent : " + step);
            }
            else
                cantFollow = true;
        }
        this.myAgent.wumpusPresume.clear();
    }

    @Override
    public boolean done() {
        return cantFollow;
    }

    @Override
    public int onEnd() {
        return 1;
    }
}
