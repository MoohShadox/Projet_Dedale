package Knowledge;

import Agents.Simple_Cognitif_Agent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import net.sourceforge.plantuml.project.Ressource;
import org.apache.jena.base.Sys;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import javax.xml.crypto.Data;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Knowledg_Base extends MapRepresentation {

    public static String successor_ns = "http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#successor";
    public static String positionated_ns = "http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#isPositionnated";
    public static String visited_ns = "http://www.co-ode.org/ontologies/ont.owl#visited";
    public static String node_ns = "http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#Node";
    public static String agent_ns = "http://www.semanticweb.org/mohamed/ontologies/2020/2/DedaleOnt#Agent";
    public static String owl_ns = "http://www.co-ode.org/ontologies/ont.owl#";
    public static String rdf_ns = "http://datas/";

    public Model knowledges = null;
    public Model beliefs = null;


    public Knowledg_Base(){
        super();
        knowledges = ModelFactory.createDefaultModel().read("src/main/java/Knowledge/dedaleOnt.owl");
        beliefs = ModelFactory.createDefaultModel();
    }

    public Knowledg_Base(int i){
        knowledges = ModelFactory.createDefaultModel().read("src/main/java/Knowledge/dedaleOnt.owl");
        beliefs = ModelFactory.createDefaultModel();
    }

    public void addAgent(String id){
        Resource agent_type = beliefs.createResource(owl_ns+"Agent");
        Resource agent = beliefs.createResource(rdf_ns+"Agent"+id);
        agent.addProperty(RDFS.label, beliefs.createLiteral(id));
        agent.addProperty(RDF.type,agent_type);
    }


    /**
     * Add or replace a node and its attribute
     * @param id Id of the node
     * @param mapAttribute associated state of the node
     */
    public void addNode(String id,MapAttribute mapAttribute){
        super.addNode(id,mapAttribute);
        // create the resource
        Resource r = beliefs.createResource(rdf_ns+"Node"+id);
        Resource node = beliefs.createResource(owl_ns+"Node");
        Resource openNode = beliefs.createResource(owl_ns+"openNode");
        // add the property
        r.addProperty(RDF.type, node);
        r.addProperty(RDFS.label, beliefs.createLiteral(id));
        r.addProperty(beliefs.createProperty(owl_ns,"mapAttribute"),mapAttribute.toString());
        //Delete the previous states
        enleverDoublonOpenClose();

    }

    public void enleverDoublonOpenClose(){
        final String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "DELETE\n" +
                " { ?node :mapAttribute \"open\" }"+
                "WHERE\n" +
                " { ?node :mapAttribute \"closed\" ;\n" +
                "         :mapAttribute \"open\" .\n" +
                " } ";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                conn.update(request);
            });
        }
    }


    public void updateCount(Simple_Cognitif_Agent agent, long counter){
        String request;
        if(counter == 0){
            //System.out.println("Insertion dans "+agent.getLocalName()+" du compteur "+counter);
            request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                    "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                    "INSERT { ?a :currentCount \"%val\"^^<http://www.w3.org/2001/XMLSchema#long> }\n".replace("%val",""+counter) +
                    "WHERE\n" +
                    "  { ?a rdf:type :Agent . ?a rdfs:label '%a'  \n".replace("%a",agent.getLocalName()) +
                    "}";
        }
        else{
             request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                    "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                    "DELETE { ?a :currentCount ?val }\n" +
                    "INSERT { ?a :currentCount \"%val\"^^<http://www.w3.org/2001/XMLSchema#long> }\n".replace("%val",""+counter) +
                    "WHERE\n" +
                    "  {  ?a :currentCount ?val . ?a rdf:type :Agent . ?a rdfs:label '%a'  \n".replace("%a",agent.getLocalName()) +
                    "}";
        }

        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                conn.update(request);
            });
        }
    }

    public void updateNodeStatus(String node,int last_visited,Simple_Cognitif_Agent agent){

        String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "INSERT { ?n :lastVisitedBy %a .".replace("%a",agent.getLocalName()) +
                "?n :lastVisitedOn " + "\"%val\"^^<http://www.w3.org/2001/XMLSchema#long> .".replace("%val",""+last_visited)+
                "}\n"+
                "WHERE\n" +
                "  { ?a rdfs:label '%a' . \n".replace("%a",agent.getLocalName()) +
                "  ?n rdfs:label '%n'" +
                "}";


        String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "DELETE { ?a :currentCount \"%val\"^^<http://www.w3.org/2001/XMLSchema#long> }\n".replace("%val",""+last_visited) +
                "WHERE\n" +
                "  { ?a rdfs:label '%a' . \n".replace("%a",agent.getLocalName()) +
                "  ?n rdfs:label '%n'" +
                "}";



        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(agent.Kb.beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                //conn.update(request);
                conn.update(request2);
            });
        }
    }


    public void  updateAgentPosition(String a,String node){
        Property p = beliefs.createProperty("http://www.co-ode.org/ontologies/ont.owl#","isInNode");
        Resource r = beliefs.createResource(rdf_ns+"Node"+node);
        Resource agent = beliefs.createResource(rdf_ns+"Agent"+a);
        beliefs.removeAll(agent,p,null);
        beliefs.add(agent,p,r);
    }

    public void loadFromKnowledge(){
        final String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT DISTINCT ?node ?attr ?lab \n" +
                " { ?node rdf:type :Node . \n" +
                " ?node :mapAttribute ?attr . \n" +
                " ?node rdfs:label ?lab . \n" +
                " }";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
               ResultSet rs =  conn.query(request).execSelect();
               while(rs.hasNext()){
                   QuerySolution qs = rs.next();
                   //System.out.println("node = " + qs.get("lab").toString() +" attr = " + qs.get("attr").toString() );
                   super.addNode(qs.get("lab").toString(),MapAttribute.valueOf(qs.get("attr").toString()));
               }
            });
        }
        final String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT DISTINCT ?n1 ?n2 ?lab1 ?lab2\n" +
                " { ?n1 rdf:type :Node . \n" +
                " ?n2 rdf:type :Node . " +
                " ?n1 :successor ?n2 ." +
                " ?n1 rdfs:label ?lab1 ." +
                " ?n2 rdfs:label ?lab2 " +
                " }";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs =  conn.query(request2).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    //System.out.println("n1 = " + qs.get("lab1").toString() +" n2 = " + qs.get("lab2").toString() );
                    super.addEdge(qs.get("lab1").toString(),qs.get("lab2").toString());
                }
            });
        }
        //super.openGui();
    }


    /**
     * Add the edge if not already existing.
     * @param idNode1 one side of the edge
     * @param idNode2 the other side of the edge
     */
    public void addEdge(String idNode1,String idNode2){
        try {
            if(idNode1.equalsIgnoreCase(idNode2))
                return;
            super.nbEdges++;
            super.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
            Resource noeud1 = beliefs.createResource(rdf_ns+"Node"+idNode1);
            Resource noeud2 = beliefs.createResource(rdf_ns+"Node"+idNode2);
            Property successeur = beliefs.createProperty(owl_ns,"successor");
            noeud1.addProperty(successeur,noeud2);
            noeud2.addProperty(successeur,noeud1);
        }catch (EdgeRejectedException e){
            //Do not add an already existing one
            this.nbEdges--;
        }
        try {
            beliefs.write(new FileOutputStream("/home/mohamed/Bureau/Projets/Dedale_Project/src/main/java/Knowledge/output.rdf"),"TURTLE");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void assertKnowledg(Agent agent){
        Property p = beliefs.createProperty("http://www.co-ode.org/ontologies/ont.owl#","know");
        beliefs.removeAll(null,p,null);
        String request1 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "DELETE { ?n :knownBy ?a }\n" +
                "WHERE\n" +
                "  {  ?a rdf:type :Agent . ?a rdfs:label '%a' . ?n rdf:type :Node  \n".replace("%a",agent.getLocalName()) +
                "}";

        String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "INSERT { ?n :knownBy ?a }\n" +
                "WHERE\n" +
                "  { ?a rdf:type :Agent . ?a rdfs:label '%a' . ?n rdf:type :Node  \n".replace("%a",agent.getLocalName()) +
                "}";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                conn.update(request1);
                conn.update(request2);
            });
        }

    }

    //try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
    //    Txn.executeWrite(conn, () -> {
    //        //conn.update(request);
    //        ResultSet rs = conn.query(request).execSelect();
    //        while(rs.hasNext()){
    //            QuerySolution qs = rs.next();
    //            Iterator it = qs.varNames();
    //            while(it.hasNext())
    //            {
    //                String var = it.next().toString();
    //                System.out.println("Var : "+var+ " val : " + qs.get(var));
    //            }
    //            //System.out.println("val = "+qs.get("val") + " a = "+qs.get("agent"));
    //        }
    //    });
    //}

    public LinkedList<AID> queryAgents(){
        LinkedList<AID> L = new LinkedList<>();
        String query = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?n ?lab\n" +
                "WHERE    { ?n rdf:type :Node . ?a rdfs:label ?lab }";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                ResultSet rs = conn.query(query).execSelect();
                while(rs.hasNext()){
                    QuerySolution qs = rs.next();
                    L.add(new AID(qs.get("lab").asLiteral().getString(),AID.ISLOCALNAME));
                    //System.out.println("val = "+qs.get("val") + " a = "+qs.get("agent"));
                }
            });
        }

        return L;
    }

    public void removeDuplicatedCountor(){
        //Model rs = beliefs;
        //Resource R = rs.createResource("http://www.co-ode.org/ontologies/ont.owl#Agent");
        //Property p = rs.createProperty("http://www.co-ode.org/ontologies/ont.owl#","currentCount");
        //ResIterator Lstm =  rs.listResourcesWithProperty(p);
        //while(Lstm.hasNext()){
        //    Resource agent = Lstm.nextResource();
        //    System.out.println("Ressource : "+agent);
//
        //    Selector sel = new SimpleSelector(agent,p,null,null);
//
        //    StmtIterator it = rs.listStatements(sel);
//
        //    long val = 0;
        //    Statement s_val = null;
//
        //    while(it.hasNext()){
        //        Statement s = it.next();
        //        System.out.println("Statement : "+s);
        //        if(s.getPredicate().equals(p))
        //        {
        //            long v = s.getLong();
        //            if(v > val){
        //                val = v;
        //                s_val = s;
        //            }
//
        //        }
        //    }
        //    rs.removeAll(agent,p,null);
        //    rs.add(s_val);
        //}
    }

    public void updateAgents(Agent myAgent){
        Resource agent = beliefs.createResource(owl_ns+"Agent");
        Selector s = new SimpleSelector(null,RDF.type,agent);
        LinkedList<AID> L = queryAgents();
        DFAgentDescription dfd = new DFAgentDescription();
        try {
            DFAgentDescription[] result = DFService.search(myAgent, dfd);
            for (int i=0;i<result.length;i++)
            {
                if(!L.contains(result[i].getName()))
                {
                    beliefs.add(beliefs.createResource(rdf_ns+"Agent"+result[i].getName().getLocalName()),RDF.type,agent);
                    beliefs.add(beliefs.createResource(rdf_ns+"Agent"+result[i].getName().getLocalName()),RDFS.label,result[i].getName().getLocalName());
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }

    public void knowledgOtherAgents(LinkedList<HashMap<String, String>> Results){
        for( HashMap<String,String> results : Results){
            Resource agents = beliefs.createResource(results.get("agent"));
            Property p = beliefs.createProperty(results.get("p"));
            RDFNode o;
            if(p == RDF.type)
            {
                o = beliefs.createResource(results.get("o"));
            }
            else
            {
                if(p.equals(RDFS.label))
                {
                    //System.out.println("for p = "+p+" on ajoute un literal ");
                    o = beliefs.createLiteral(results.get("o"));
                }
                else{
                    if(p == beliefs.createProperty("http://www.co-ode.org/ontologies/ont.owl#currentCount"))
                    {
                        String number = results.get("o").split("\\^\\^")[0];
                        o = beliefs.createTypedLiteral(Long.parseLong(number));
                    }
                    else{
                        o = beliefs.createResource(results.get("o"));
                    }
                }
            }
            //"http://www.co-ode.org/ontologies/ont.owl#currentCount"
            beliefs.removeAll(agents,p,null);
            beliefs.add(agents,p,o);
        }
    }

    public void addPerceptions(Simple_Cognitif_Agent agent,String node,String perception){
        Resource p = beliefs.createResource(rdf_ns+"P"+node);
        //beliefs.removeAll(p,null,null);
        //beliefs.removeAll(null,null,p);
        String request1 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "DELETE {  %p :perceptedBy ?a . %p :perceptedIn ?n . %p :perceptedOn \"%cpt\"^^<http://www.w3.org/2001/XMLSchema#long>  . %p :perceptionContent '%ctnt'  }\n"
                        .replace("%p","<"+rdf_ns+"P"+node+">")
                        .replace("%cpt",agent.stepCountor+"").replace("%ctnt",perception) +
                "WHERE\n" +
                ("  { ?a rdf:type :Agent . ?a rdfs:label '%a' . ?n rdf:type :Node . \n".replace("%a",agent.getLocalName())  +
                        " ?n rdfs:label '%n' .".replace("%n",node) +
                        "")+
                "}";

        String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "INSERT {  %p :perceptedBy ?a . %p :perceptedIn ?n . %p :perceptedOn \"%cpt\"^^<http://www.w3.org/2001/XMLSchema#long>  . %p :perceptionContent '%ctnt'  }\n"
                        .replace("%p","<"+rdf_ns+"P"+node+">")
                        .replace("%cpt",agent.stepCountor+"").replace("%ctnt",perception) +
                "WHERE\n" +
                ("  { ?a rdf:type :Agent . ?a rdfs:label '%a' . ?n rdf:type :Node . \n".replace("%a",agent.getLocalName())  +
                        " ?n rdfs:label '%n' .".replace("%n",node) +
                        "")+
                "}";
        try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
            Txn.executeWrite(conn, () -> {
                conn.update(request1);
                conn.update(request2);
            });
        }
    }



    public void addStenche(String agentName,LinkedList<Integer> stenches){
        Property p = beliefs.createProperty(owl_ns+"indicateStenche");

        Resource agent = beliefs.createResource(rdf_ns +"Agent"+agentName);
        beliefs.removeAll(agent,p,null);
        for (Integer stench:stenches){
            Resource node = beliefs.createResource(rdf_ns + "Node"+stench);
            if(stench != null)
            {
                //System.out.println("Ajout des stenche : agent = "+agentName+" node = "+stench);
                beliefs.add(agent,p,node);
            }
        }
    }






    public static void main(String[] args) throws FileNotFoundException {

        long counter = 0;
        Model beliefs = ModelFactory.createDefaultModel();
        beliefs.read(new FileInputStream("/home/mohamed/IdeaProjects/Dedale_Cognitif/src/main/java/Knowledge/exemple.rdf"),null,"TURTLE");



        //String node = "19";
        //String perception = "Stench";
        //long stepCountor = 41;
        //String agentName = "Explo1";
        //String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
        //        "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
        //        "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        //        "INSERT {  %p :perceptedBy ?a . %p :perceptedIn ?n . %p :perceptedOn \"%cpt\"^^<http://www.w3.org/2001/XMLSchema#long>  . %p :perceptionContent '%ctnt'  }\n"
        //                .replace("%p","<"+rdf_ns+"P"+node+">")
        //                .replace("%cpt",stepCountor+"").replace("%ctnt",perception) +
        //        "WHERE\n" +
        //        ("  { ?a rdf:type :Agent . ?a rdfs:label '%a' . ?n rdf:type :Node . \n".replace("%a",agentName)  +
        //                " ?n rdfs:label '%n' .".replace("%n",node) +
        //                "")+
        //        "}";
        //System.out.println("Requete qu'on execute : ");
        //System.out.println(request2);
        //try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
        //    Txn.executeWrite(conn, () -> {
        //        //conn.update(request1);
        //        conn.update(request2);
        //    });
        //}
        //beliefs.write(System.out,"TURTLE");

        String agent = "Explo2";
        String request =  "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
                "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "SELECT ?lab1 \n" +
                "WHERE    { "+
                " ?n1 rdf:type :Node . " +
                " ?a1 :indicateStenche ?n1 . " +
                " ?a1 rdf:type :Agent ." +
                " ?n1 rdfs:label ?lab1 . " +
                " ?a1 rdfs:label '%a'. ".replace("%a",agent) +
                "}";


        try (RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs))) {
            Txn.executeWrite(conn, () -> {
                System.out.println("Exeuction de la requete : ");
                System.out.println(request);
                System.out.println("Résultats : ====");
                ResultSet rs = conn.query(request).execSelect();
                //rs.write(System.out,"TURTLE");
                LinkedList<HashMap<String, String>> lstm = new LinkedList<>();
                while(rs.hasNext())
                {
                    QuerySolution qs = rs.next();
                    Iterator<String> it = qs.varNames();
                    HashMap<String, String> H = new HashMap();
                    while (it.hasNext())
                    {
                        String var = it.next();
                        H.put(var,qs.get(var).toString());
                    }
                    lstm.add(H);
                }
                for (HashMap<String, String> rez:lstm)
                    System.out.println(rez);
            });
        }
//
//
//
//
        //String request2 = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
        //        "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        //        "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
        //        "DESCRIBE ?n ?agent\n" +
        //        "WHERE    { ?n rdf:type :Node . ?agent rdf:type :Agent . ?agent rdfs:label ?lab . " +
        //        "OPTIONAL { ?a :know ?n . " +
        //        "?a rdf:type :Agent . " +
        //        "?a rdfs:label '%a' . ".replace("%a","Explo1") +
        //        "}\n" +
        //        "FILTER ( !bound(?a) ||  regex(?lab, \"^%a\") )".replace("%a","Explo1") +
        //        "}";
//
//
        ////String request = "PREFIX :  <http://www.co-ode.org/ontologies/ont.owl#>\n" +
        ////        "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
        ////        "PREFIX rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n" +
        ////        "DESCRIBE ?n ?agent\n" +
        ////        "WHERE    { ?n rdf:type :Node . ?agent rdf:type :Agent . ?agent rdfs:label ?lab . " +
        ////        "}";
//
        //try ( RDFConnection conn = RDFConnectionFactory.connect(DatasetFactory.create(beliefs)) ) {
        //    Txn.executeWrite(conn, () -> {
        //        //conn.update(request);
        //        Model rs = conn.queryDescribe(request2);
        //        //rs.write(System.out,"TURTLE");
        //    });
        //}
        ////beliefs.write(System.out,"TURTLE");
    }




}
