package Knowledge;


import org.apache.jena.tdb.store.Hash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class Markov_Model implements Serializable {
    public HashMap<Integer,Double> distribution = new HashMap<>();
    public transient HashMap<Integer, HashSet<Integer>> successeurs;
    public transient HashSet<Integer> noeuds = new HashSet<>();

    public Markov_Model(HashMap<Integer, HashSet<Integer>> successeurs){
        this.successeurs = successeurs;
        noeuds.addAll(successeurs.keySet());
        for (Integer i:successeurs.keySet()){
            noeuds.addAll(successeurs.get(i));
        }
    }

    public Double getTransition(Integer src,Integer dst){
        if(successeurs.get(src).contains(dst))
        {
            return (1.0/successeurs.get(src).size());
        }
        else
            return 0.0;
    }

    public void setDistribution(int position_encombree,int ma_position){
        if(noeuds.contains(position_encombree )){
            for (Integer i:noeuds)
            {
                if(i == position_encombree)
                {
                    distribution.put(i,1.0);
                }
                else
                {
                    distribution.put(i,0.0);
                }
            }
        }
        else{
            for (Integer i:noeuds)
            {
                distribution.put(i,1.0/(noeuds.size()-1));
            }
            distribution.put(ma_position,0.0);
        }
    }

    public void setDistributionWumpus(int position_odorante,int my_position){
        if(noeuds.contains(position_odorante )){
            //distribution.put(position_odorante,1.0/(successeurs.get(position_odorante).size()-1));
            for (Integer j:noeuds)
                if(successeurs.get(position_odorante).contains(j))
                    distribution.put(j,1.0/(successeurs.get(position_odorante).size()-1));
                else
                    distribution.put(j,0.0);
        }else
        {
            for (Integer i:noeuds)
            {
                distribution.put(i,0.0);
            }
        }
        distribution.put(my_position,0.0);
    }


    public boolean nodePresent(HashMap<Integer,HashSet<Integer>> successeurs){
        HashSet<Integer> nodes = new HashSet<>();
        nodes.addAll(successeurs.keySet());
        for (Integer i:successeurs.keySet()){
            nodes.addAll(successeurs.get(i));
        }
        return this.noeuds.containsAll(nodes);
    }

    public void avancerDansLeTemps(){
        HashMap<Integer,Double> n_distribution = new HashMap<>();
        for (Integer n:successeurs.keySet())
        {
            double d = 0;
            for (Integer i:successeurs.get(n))
            {
                d = d + distribution.get(i) * getTransition(i,n);
            }
            n_distribution.put(n,d);
        }
        distribution = n_distribution;
    }

    public void updateModele(HashMap<Integer,HashSet<Integer>> successeurs,int ma_position){
        if(!nodePresent(successeurs))
        {
            this.successeurs = successeurs;
            noeuds.addAll(successeurs.keySet());
            for (Integer i:successeurs.keySet()){
                noeuds.addAll(successeurs.get(i));
            }
            this.distribution = new HashMap<>();
            //Todo : Remember to let this permanent
            //setDistribution(-1,ma_position);
        }
    }

    public int tailleModele(){
        return distribution.keySet().size();
    }


    public static void main(String[] args){
        HashMap<Integer,HashSet<Integer>> succ = new HashMap<>();
        HashSet L1;
        L1 = new HashSet();
        L1.add(2);
        L1.add(3);
        succ.put(1,L1);
        L1 = new HashSet();
        L1.add(4);
        succ.put(3,L1);
        Markov_Model M = new Markov_Model(succ);
        M.setDistribution(10,2);
        System.out.println(M.distribution);
    }



}
