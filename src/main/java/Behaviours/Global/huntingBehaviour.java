package Behaviours.Global;

import Agents.Simple_Cognitif_Agent;
import Behaviours.followWumpusBehaviour;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;

import java.util.LinkedList;

public class huntingBehaviour extends ParallelBehaviour {
    Simple_Cognitif_Agent myAgent;
    LinkedList<Behaviour> other_behaviours = new LinkedList<>();

    public huntingBehaviour(Simple_Cognitif_Agent myAgent, Behaviour ... other_behaviours){
        super(myAgent,WHEN_ANY);
        this.myAgent = myAgent;

        for (Behaviour b:other_behaviours)
        {
            addSubBehaviour(b);
            System.out.println("Ajout au hunting behaivour de : " + b.getBehaviourName());
            this.other_behaviours.add(b);
        }
    }

    @Override
    public int onEnd() {
        System.out.println("On end du hunting = " + other_behaviours.get(0).onEnd());
        return other_behaviours.get(0).onEnd();
    }
}
