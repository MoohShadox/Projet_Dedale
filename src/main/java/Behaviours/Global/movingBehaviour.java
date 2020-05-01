package Behaviours.Global;

import Agents.Simple_Cognitif_Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;

import java.util.LinkedList;

public class movingBehaviour extends ParallelBehaviour {
    Simple_Cognitif_Agent myAgent;
    LinkedList<Behaviour> other_behaviours = new LinkedList<>();

    public movingBehaviour(Simple_Cognitif_Agent myAgent,Behaviour ... other_behaviours){
        super(myAgent,WHEN_ANY);
        this.myAgent = myAgent;
        for (Behaviour b:other_behaviours)
        {
            addSubBehaviour(b);
            this.other_behaviours.add(b);
        }
    }

    @Override
    public int onEnd() {
        int cpt = 0;
        for (Behaviour o:other_behaviours)
        {
            cpt = cpt + o.onEnd();
        }
        return cpt;
    }
}
