package Knowledge;

import Agents.Simple_Cognitif_Agent;
import jade.core.AID;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;

import java.util.*;

public class PolicyEvaluationExploration {

    public Knowledg_Base kb;
    public Simple_Cognitif_Agent myAgent;
    public HashMap<Integer,Double> values;
    public LinkedList<LinkedList<Integer>> plans = new LinkedList<>();
    public HashMap<Integer,HashSet<Integer>> successeurs = new HashMap<>();
    public double lambda = 0.9;
    public LinkedList<Integer> plan_courant = new LinkedList<>();
    public HashMap<Integer,Double> ponderations = null;
    public LinkedList<Integer> penality = new LinkedList<>();

    public PolicyEvaluationExploration(Simple_Cognitif_Agent myAgent){
        this.myAgent = myAgent;
        kb = myAgent.Kb;
    }

    public void initialiserValeur(){
        if(values == null)
            values = new HashMap<>();
        String query1 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?lab1 ?attr\n" +
                "WHERE    { "+
                " ?n1 rdf:type :Node . " +
                " ?n1 rdfs:label ?lab1 . " +
                "?n1 :mapAttribute  ?attr" +
                "}";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(kb.beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs = conn.query(query1).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    if(penality.contains(Integer.parseInt(qs.get("lab1").toString())))
                        values.put(Integer.parseInt(qs.get("lab1").toString()),-10.0);
                    else{
                        if(qs.get("attr").toString().equalsIgnoreCase("open"))
                            values.put(Integer.parseInt(qs.get("lab1").toString()),100.0);
                        else
                            values.put(Integer.parseInt(qs.get("lab1").toString()),0.0001);
                    }
                    //System.out.println("val = "+qs.get("val") + " a = "+qs.get("agent"));
                }
            });
        }
        //System.out.println("Valueres suite a l'initialisation : "+values);
        //System.out.println("Sachant que le vecteur de pénalité était a :"+this.penality);
//
    }

    public void updateSuccesseurs(){
        successeurs = new HashMap<>();

        String query1 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?n ?lab\n" +
                "WHERE    { ?n rdf:type :Node . ?n rdfs:label ?lab }";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(kb.beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs = conn.query(query1).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    successeurs.put(Integer.parseInt(qs.get("lab").toString()),new HashSet<>());
                    //System.out.println("val = "+qs.get("val") + " a = "+qs.get("agent"));
                }
            });
        }
        String query2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?lab1 ?lab2\n" +
                "WHERE    { "+
                " ?n1 :successor ?n2 . " +
                " ?n1 rdf:type :Node . " +
                " ?n2 rdf:type :Node . " +
                " ?n1 rdfs:label ?lab1 . " +
                "?n2 rdfs:label ?lab2" +
                "}";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(kb.beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs = conn.query(query2).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    successeurs.get(Integer.parseInt(qs.get("lab1").toString())).add(Integer.parseInt(qs.get("lab2").toString()));
                    successeurs.get(Integer.parseInt(qs.get("lab2").toString())).add(Integer.parseInt(qs.get("lab1").toString()));
                }
            });
        }

    }

    public void propagerValeurs(){
        for (Integer i:this.successeurs.keySet()){
            //System.out.println("===Update value of : "+i+ " wich have successeurs : "+this.successeurs.get(i) + " and value of : "+values.get(i) + "===");
            HashSet<Integer> S = this.successeurs.get(i);
            double m = 0;
            for (Integer n:S)
            {
                //System.out.println("The successor : "+n+" have value : "+values.get(n)*this.lambda);
                double v = values.get(n)*this.lambda;
                if(v>m)
                {
                    m = v;
                }
            }
            //System.out.println("Penality = "+penality);
            //System.out.println("Values : "+values);
            //System.out.println("Maximum found in the neighbours of "+i+" is : "+m);

            if((m >= values.get(i)) && !penality.contains(i))
            {
                values.put(i,m);
                //System.out.println("New value of "+i+"is "+values.get(i));
            }
            //System.out.println("=====End of the update with a value of "+values.get(i));
        }
        //System.out.println("Valeures suite a la propagation : "+values);
    }

    public void calculerPlans(){
        HashMap<Integer,Double> old_values;
        do {
            old_values= new HashMap<>();
            old_values.putAll(values);
            propagerValeurs();
            //System.out.println("Comparaison  : ");
            //for (Integer i:old_values.keySet()){
            //    System.out.println(i + " : h1-"+old_values.get(i)+" h2-"+values.get(i));
            //}
        }
        while(difference(values,old_values) > 0.000000001);
    }

    public void addPenalised(String node){
        penality.add(Integer.parseInt(node));
    }

    public String getNextStep(){


        while(this.plan_courant.isEmpty())
        {
            this.initialiserValeur();
            this.updateSuccesseurs();
            this.calculerPlans();
            this.genererPlans(Integer.parseInt(myAgent.getCurrentPosition()));
            this.plan_courant = this.plans.getFirst();
            this.penality.clear();
        }
        String next_step;
        //if(this.plan_courant.isEmpty())
        //{
        //     next_step = this.plan_courant.get(0).toString();
        //}
        //else
        next_step = this.plan_courant.get(0).toString();
        this.plan_courant.removeFirst();
        return next_step;
    }

    public LinkedList<LinkedList<Integer>> genererPlans(int source)
    {
        LinkedList<LinkedList<Integer>> plans = new LinkedList<>();
        boolean stop = false;
        LinkedList<Integer> tmp = new LinkedList<>();
        Stack<LinkedList<Integer>> pistes = new Stack<>();
        LinkedList<Integer> explored = new LinkedList<>();
        tmp.add(source);
        pistes.push(tmp);
        while(!pistes.empty()){
            //Dernier chemin empilé
            tmp = pistes.pop();
            //On récupére le dernier élément du chemin
            Integer last = tmp.getLast();
            //On l'ajoute aux explorés
            explored.add(last);
            //On énumère ces successeurs
            HashSet<Integer> possibilites = this.successeurs.get(last);
            if(possibilites == null)
            {
                //System.err.println("Pas de possibilités : ");
                return plans;
            }
            //On calcule la valeur maximale des successeurs qui ne sont pas explorés pour pas créer de cycles
            double max = -5;
            //Cette variable sert a ce que si le noeud n'a pas de successeurs non explorés ce qui signie que le chemin est fini on l'ajoute au plans
            boolean no_successors = true;

            for (Integer possibilite : possibilites) {
                if (!(explored.contains(possibilite))) {
                    no_successors = false;
                    if (values.get(possibilite) > max) {
                        max = values.get(possibilite);
                    }
                }
            }
            if(no_successors){
                //Chemin sans successeurs on l'ajoute aux plans
                //System.err.println("Pas de successeurs : "+tmp);
                plans.add(tmp);
            }
            else{
                final double m = max;

                if(m == 0.0)
                {
                    //TODO : Probléme d'initialisation qui de temps en temps apparait
                    System.err.println("Le max donne : "+m);
                    System.err.println("Position actuelle : "+myAgent.getCurrentPosition());
                    System.err.println("Sucesseurs de la postion actuelle : " + successeurs.get(Integer.parseInt(myAgent.getCurrentPosition())));
                    System.err.println("Sucesseurs : "+this.successeurs);
                    System.err.println("Valeures  : "+values);
                    Scanner sc = new Scanner(System.in);
                    //sc.nextLine();
                }
                //On efface les successeurs dont la valeur est inférieure au maximum
                possibilites.removeIf(integer -> values.get(integer)<m);
                //On génère des chemins en accumulant le possibilités intéressantes au chemin qu'on avait dépilé et on les empilent
                LinkedList<Integer> pos;
                for (Integer p:possibilites) {
                    //si le chemin intéressant en question est mieux que le chemin courant on l'ajoute et on empile
                    if(values.get(p) > values.get(last))
                    {
                        pos = new LinkedList<>(tmp);
                        pos.add(p);
                        //System.out.println("Empilement de : "+p);
                        pistes.push(pos);
                    }
                    //Sinon on considére que le chemin est terminé et par conséquent on l'enregistre
                    else{
                        //Chemin sans successeurs on l'ajoute aux plans
                        //System.out.println("Ajout du plan : "+tmp);
                        if(!(plans.contains(tmp)))
                            plans.add(tmp);
                    }
                }
            }

        }
        //Boucle d'affichage servant au test :
        //System.out.println("Liste des plans : ");
        //for (LinkedList plan:plans){
        //    System.out.println(plan);
        //}
        this.plans = new LinkedList<>();
        for (LinkedList<Integer> plan:plans)
        {
            plan.removeFirst();
            this.plans.add(plan);
        }
        return this.plans;
    }

    public static double difference(HashMap<Integer,Double> h1 , HashMap<Integer,Double> h2){
        double cpt = 0.0;
        for (Integer i : h1.keySet()){
            cpt = cpt + (Math.max(h1.get(i),h2.get(i)) - Math.min(h1.get(i),h2.get(i)));
        }
        return cpt;
    }



}
