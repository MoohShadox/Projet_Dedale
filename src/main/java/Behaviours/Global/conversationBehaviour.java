package Behaviours.Global;

import Agents.Simple_Cognitif_Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;

import java.util.LinkedList;

public class conversationBehaviour extends ParallelBehaviour {
    Simple_Cognitif_Agent myAgent;
    LinkedList<Behaviour> protocoles = new LinkedList<>();

    public conversationBehaviour(Simple_Cognitif_Agent myAgent, Behaviour ... other_behaviours){
        super(myAgent,WHEN_ANY);
        this.myAgent = myAgent;
        for (Behaviour b:other_behaviours)
        {
            addSubBehaviour(b);
            this.protocoles.add(b);
        }
    }

    @Override
    public int onEnd() {
        int cpt = 0;
        for (Behaviour o:protocoles)
        {
            cpt = cpt + o.onEnd();
        }
        return cpt;
    }

}
