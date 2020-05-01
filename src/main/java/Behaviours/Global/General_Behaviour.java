package Behaviours.Global;

import Agents.Simple_Cognitif_Agent;
import Behaviours.Coalitionnal.followingAgent;
import Behaviours.Coalitionnal.followingStench;
import Behaviours.Communication.*;
import Behaviours.Exploration_Behaviour;
import Behaviours.followWumpus2Behaviour;
import Behaviours.followWumpusBehaviour;
import jade.core.behaviours.FSMBehaviour;

public class General_Behaviour extends FSMBehaviour {


    private final followingStench followStenchTrace;
    private final huntingBehaviour rushingBehaviour;
    private final Behaviours.Coalitionnal.followingAgent followingAgent;
    private Simple_Cognitif_Agent myAgent;
    public movingBehaviour movingBehaviour;
    public Exploration_Behaviour exploration_behaviour;
    public callingBehaviour callingBehaviour;
    //public Conversation conversationBehaivour;
    public conversationBehaviour conversationBehaviour;
    public mapExchanging mapExchanging;
    public conversationMode conversationMode;

    public perceptionExchanging perceptionExchanging;
    public followWumpusBehaviour followWumpusBehaviour;
    public crierAuWumpus crierAuWumpus;
    public huntingBehaviour huntingBehaviour;
    private  followWumpus2Behaviour followWumpus2Behaviour2;

    public General_Behaviour(Simple_Cognitif_Agent myAgent){
        super(myAgent);
        this.myAgent =  myAgent;
        //Création des behaviours
        exploration_behaviour = new Exploration_Behaviour(myAgent,myAgent.Kb);
        callingBehaviour = new callingBehaviour(myAgent);

        crierAuWumpus = new crierAuWumpus(myAgent);
        conversationMode = new conversationMode(this.myAgent);
        perceptionExchanging = new perceptionExchanging(this.myAgent);

        mapExchanging = new mapExchanging(this.myAgent);


        followWumpusBehaviour = new followWumpusBehaviour(this.myAgent);
        followWumpus2Behaviour2 = new followWumpus2Behaviour(this.myAgent);


        followStenchTrace = new followingStench(this.myAgent);
        followingAgent = new followingAgent(this.myAgent);

        conversationBehaviour = new conversationBehaviour(myAgent,mapExchanging,conversationMode,perceptionExchanging);
        movingBehaviour = new movingBehaviour(myAgent,exploration_behaviour,callingBehaviour,crierAuWumpus);




        huntingBehaviour = new huntingBehaviour(this.myAgent,followStenchTrace,crierAuWumpus);
        rushingBehaviour = new huntingBehaviour(this.myAgent,followingAgent,crierAuWumpus);

        //Variables de réinitialisation
        String [] moving = {"MOVING"} ;
        String [] conversation = {"CONVERSATION"} ;
        String [] hunting = {"HUNTING"} ;
        String [] rushing = {"RUSHING"} ;

        //Création des états
        registerFirstState(movingBehaviour,"MOVING");
        registerState(conversationBehaviour,"CONVERSATION");
        registerState(huntingBehaviour,"HUNTING");
        registerState(rushingBehaviour,"RUSHING");

        registerTransition("MOVING","MOVING",0,moving);
        registerTransition("MOVING","CONVERSATION",1,conversation);


        registerTransition("MOVING","HUNTING",11,hunting);
        registerTransition("MOVING","HUNTING",10,hunting);

        registerTransition("MOVING","HUNTING",4,hunting);

        registerTransition("MOVING","RUSHING",20,rushing);
        registerTransition("MOVING","RUSHING",21,rushing);

        registerDefaultTransition("RUSHING","MOVING",moving);


        registerTransition("CONVERSATION","MOVING",1,moving);
        registerDefaultTransition("CONVERSATION","MOVING",moving);

        registerDefaultTransition("HUNTING","MOVING",moving);
        registerTransition("HUNTING","HUNTING",0,hunting);





    }


}
