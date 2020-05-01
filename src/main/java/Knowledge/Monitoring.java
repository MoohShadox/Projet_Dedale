package Knowledge;

import Agents.Simple_Cognitif_Agent;
import jade.core.Agent;

import java.util.HashMap;
import java.util.LinkedList;

public class Monitoring {
    public Simple_Cognitif_Agent myAgent;
    HashMap<String, Markov_Model> monitors_agents = new HashMap<>();
    HashMap<String, Integer> positions = new HashMap<>();

    public Monitoring(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
    }


}
