package Behaviours.Coalitionnal;

import Agents.Simple_Cognitif_Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.UnreadableException;

import javax.swing.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class followingStench  extends OneShotBehaviour {


    Simple_Cognitif_Agent myAgent;
    boolean lost = false;

    public followingStench(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }


    public void answeringProposals(){

    }


    @Override
    public void action() {
        System.out.println(myAgent.getLocalName() + " > Suivi d'une piste ");
        myAgent.percept();
        myAgent.updateNodeStates();
        myAgent.crierPositions();
        try {
            myAgent.ecouterPositions();
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        LinkedList<String> L = myAgent.seekForStench();
        //System.out.println(myAgent.getLocalName() + " > Following Stench : "+L);
        System.out.println("Accessible Stench : "+ myAgent.hasAccessibleStench());
        System.out.println("L = " + L);
        lost = !myAgent.hasAccessibleStench() || L.isEmpty();
        if(!lost)
        {
            String next_pos = L.getLast();
            Scanner sc = new Scanner(System.in);

            boolean success = myAgent.moveTo(next_pos);

            //System.out.println(this.myAgent.getLocalName() + ">" + "Test de présumage de wumpus en " + next_pos+ " sachant sucess = "+success);
            if(!success && myAgent.presumedWumpus(Integer.valueOf(next_pos)))
            {
                System.out.println(this.myAgent.getLocalName() + ">" + "Wumpus présumé en : " + next_pos);
                this.myAgent.wumpusPresume.put(myAgent.getLocalName(),Integer.valueOf(next_pos));
            }
            else{
                this.myAgent.wumpusPresume.remove(myAgent.getLocalName());
            }
        }

    }

    @Override
    public int onEnd() {
        return lost ? 1 : 0;
    }
}