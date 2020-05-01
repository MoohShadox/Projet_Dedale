package Behaviours;

import Agents.Simple_Cognitif_Agent;
import Knowledge.MapRepresentation;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;

import java.util.*;

public class followWumpus2Behaviour extends SimpleBehaviour {

    public Simple_Cognitif_Agent myAgent;
    public boolean pistes_perdus = false;


    public followWumpus2Behaviour(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }

    public boolean presumedWumpus(Integer pos){
        HashSet<Integer> positions_odorantes = new HashSet<>();
        HashSet<Integer> positions_adverses = new HashSet<>();
        for (String agent:myAgent.odeurs.keySet())
        {
            positions_odorantes.addAll(myAgent.odeurs.get(agent));
        }
        for(String agent:myAgent.positions_agents.keySet())
        {
            positions_adverses.add(myAgent.positions_agents.get(agent));
        }
        //System.out.println(this.myAgent.getLocalName() + ">" +"Positions adverse : "+positions_adverses);
        //System.out.println(this.myAgent.getLocalName() + ">" +"Positions odorantes : "+positions_odorantes);
        //System.out.println(this.myAgent.getLocalName() + ">" +"Position a tester : "+pos);
        return !positions_adverses.contains(pos) && positions_odorantes.contains(Integer.parseInt(myAgent.getCurrentPosition()));
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
        this.myAgent.percept();
        LinkedList<String> L = myAgent.seekForStench();
        if(!L.isEmpty())
        {
            String next_pos = L.getLast();
            //System.out.println(this.myAgent.getLocalName() + ">" + "Odeurs avant mouvement " + L);
            //System.out.println(this.myAgent.getLocalName() + ">" + "Positions avant mouvement " + this.myAgent.getCurrentPosition());
            //Scanner sc = new Scanner(System.in);
            //sc.nextLine();
            boolean success = myAgent.moveTo(next_pos);
            //System.out.println(this.myAgent.getLocalName() + ">" + "Test de présumage de wumpus en " + next_pos+ " sachant sucess = "+success);
            if(!success && presumedWumpus(Integer.valueOf(next_pos)))
            {
                System.out.println(this.myAgent.getLocalName() + ">" + "Wumpus présumé en : " + next_pos);
                this.myAgent.wumpusPresume.put(myAgent.getLocalName(),Integer.valueOf(next_pos));
            }
        }
        else
        {
            System.out.println("Pas de piste olfactive");
        }
        pistes_perdus = L.isEmpty();
    }

    @Override
    public boolean done() {
        return pistes_perdus;
    }

    @Override
    public int onEnd() {
        return 3;
    }
}
