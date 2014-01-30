package graph;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 *
 * @author philipp
 */
public class InteractionGraph extends DirectedSparseGraph<Term, Relation> {
    Map<String,Term> nodemap;
    Map<Term,String> enc_map;
    
    public InteractionGraph(){
        super();
        nodemap = new HashMap<String, Term>();
    }
        
    public void addNode(Term t){
        if(!nodemap.containsKey(t.getName().toLowerCase())){
            addVertex(t);
            nodemap.put(t.getName().toLowerCase(), t);
            for(String a: t.getAliases()){
                nodemap.put(a.toLowerCase(), t);
            }        
        }
    }
    
    public List<String> getAllNames(){
        List<String> names = new LinkedList<String> (nodemap.keySet());
        Collections.sort(names);
        return names;
    }
  
    public void removeTerm(Term t) {
        List<Relation> bla = new ArrayList<Relation>(this.getInEdges(t));
        for (int i=0; i<bla.size(); i++) {
            this.removeEdge(bla.get(i));
        }
        bla.clear();
        bla.addAll(this.getOutEdges(t));
        for (int i=0; i<bla.size(); i++) {
            this.removeEdge(bla.get(i));
        }
        this.removeVertex(t);
        
        if(nodemap.containsKey(t.getName().toLowerCase())){
                nodemap.remove(t.getName().toLowerCase());
            for(String a: t.getAliases()){
                nodemap.remove(a.toLowerCase());
            }
        }
    }

    public void addRelation(Relation r){
        if(!nodemap.containsKey(r.getAgent().getName().toLowerCase())){
            addVertex(r.getAgent());
            nodemap.put(r.getAgent().getName().toLowerCase(), r.getAgent());
            for(String a: r.getAgent().getAliases()){
                nodemap.put(a.toLowerCase(), r.getAgent());
            }        }
        if(!nodemap.containsKey(r.getTheme().getName().toLowerCase())){
            addVertex(r.getTheme());
            nodemap.put(r.getTheme().getName().toLowerCase(), r.getTheme());
            for(String a: r.getTheme().getAliases()){
                nodemap.put(a.toLowerCase(), r.getTheme());
            }        }
        if(!containsEdge(r)){
            addEdge(r, r.getAgent(), r.getTheme());
        } 
    }
    
    public Term getTerm(String name){
        return nodemap.get(name.toLowerCase());
    }
    
    public void updateMap(){
        nodemap = new HashMap<String,Term>();
        for(Term t: getVertices()){
            nodemap.put(t.getName().toLowerCase(), t);
            for(String a: t.getAliases()){
                nodemap.put(a.toLowerCase(), t);
            }
        }
    }
    
    @Override
    public String toString(){
        String s = super.toString();
        return s;
    }
    
    private Vector<String> getNodeMapEncoding(){
        Vector<String> result = new Vector<String>();
        for(String s: nodemap.keySet()){
            result.add(nodemap.get(s).getSimpleEncoding());
        }
        return result;
    }
    
    private Vector<String> getNicerNodeMapEncoding(){
        Vector<String> result = new Vector<String>();
        int counter = 1;
        for(String s: nodemap.keySet()){
            enc_map.put(nodemap.get(s), "T"+counter);
            result.add(nodemap.get(s).getNicerEncoding(counter++));
        }
        return result;        
    }
    
    private Vector<String> getRelationsEncoding(){
        Vector<String> result = new Vector<String>();
        for(Relation r: getEdges()){
            result.add(r.getSimpleEncoding());
        }
        return result;
    }
    
    private Vector<String> getNicerRelationsEncoding(Map<Term,String> termmap){
        Vector<String> result = new Vector<String>();
        for(Relation r: getEdges()){
            result.add(r.getNicerEncoding(termmap));
        }
        return result;
    }
    
    public Vector<String> getSimpleEncoding(){
        Vector<String> result = getNodeMapEncoding();
        result.add("~@~");
        result.addAll(getRelationsEncoding());
        return result;
    }
    
    public Vector<String> getNicerEncoding(){
        enc_map = new HashMap<Term,String>();
        Vector<String> result = new Vector<String>();
        result.add("Terms:");
        result.addAll(getNicerNodeMapEncoding());
        result.add("Relations:");
        result.addAll(getNicerRelationsEncoding(enc_map));
        enc_map = null;
        return result;
        
    }
    
    public static InteractionGraph getGraphFromSimpleEncoding(Vector<String> enc){
        Vector<String> nodeMapEnc = new Vector<String>();
        Vector<String> edgeEnc = new Vector<String>();
        boolean second = false;
        for(String s: enc){
            if(!s.equals("~@~") && !second){
                nodeMapEnc.add(s);
            } else if(s.equals("~@~")){
                second = true;
            } else{
                edgeEnc.add(s);
            }
        }
        InteractionGraph g = new InteractionGraph();
        fillNodeMapFromEncoding(g, nodeMapEnc);
        addEdgesFromEncoding(g,edgeEnc);
        return g;
    }
    
    public static InteractionGraph getGraphFromNicerEncoding(Vector<String> enc){
        Vector<String> nodeMapEnc = new Vector<String>();
        Vector<String> edgeEnc = new Vector<String>();
        boolean second = false;
        for(String s: enc){
            if(s.trim().equals("Terms:")){
            } else if(s.trim().equals("Relations:")){
                second = true;
            } else if (s.trim().length()==0){
            } else if(second){
                edgeEnc.add(s);
            } else{
                nodeMapEnc.add(s);                
            }
        }
        InteractionGraph g = new InteractionGraph();
        Map<String,Term> termmap = fillNodeMapFromNicerEncoding(g, nodeMapEnc);
        addEdgesFromNicerEncoding(g,edgeEnc,termmap);
        return g;
    }
    
    private static void addEdgesFromEncoding(InteractionGraph g, Vector<String> enc){
        for(String s: enc){
            Relation r = Relation.getRelationFromSimpleEncoding(s);
            if(r!=null)g.addRelation(r);
        }
    }
    
    private static void addEdgesFromNicerEncoding(InteractionGraph g, Vector<String> enc, Map<String,Term> termmap){
        for(String s: enc){
            Relation r = Relation.getRelationFromNicerEncoding(s, termmap);
            if(r!=null)g.addRelation(r);
        }        
    }
    
    private static void fillNodeMapFromEncoding(InteractionGraph g, Vector<String> enc){
        for(String s: enc){
            Term t = Term.getTermFromSimpleEncoding(s);
            if(t!=null)g.addNode(t);
        }
    }
    
    private static Map<String,Term> fillNodeMapFromNicerEncoding(InteractionGraph g, Vector<String> enc){
        Map<String,Term> termmap = new HashMap<String,Term>();
        for(String s: enc){
            TermIDTuple t = Term.getTermFomNicerEncoding(s);
            if(t!=null) {
                g.addNode(t.term);
            }
            termmap.put(t.id, t.term);
        }
        return termmap;
    }
    
    public static String esc(String s){
        return s.replaceAll("\t", " ").replaceAll("\\|", "-");
    }
    
//    public static void main(String[] args){
//        InteractionGraph g = new InteractionGraph();
//        Term t1 = new Term("Node1", "Type1");
//        g.addNode(t1);
//        Term t2 = new Term("Node2","Type1");
//        g.addNode(t2);
//        Term t3 = new Term("Node3","Type2");
//        g.addNode(t3);
//        g.addRelation(new Relation("Relation1", t1, t2, "Ev1", Calendar.getInstance(), "id1"));
//        g.addRelation(new Relation("Relation2", t1, t3, "Ev2", Calendar.getInstance(), "id2"));
//        System.out.println(g);
//        
//        IOHandler.writeToFile("data/"+"testgraphfile.txt", g.getSimpleEncoding());
//        IOHandler.writeToFile("data/"+"nicetest.txt", g.getNicerEncoding());
//        System.out.println("++ Old encoding: ++");
//        for(String s: g.getSimpleEncoding())
//            System.out.println(s);
//        System.out.println("++ End old encoding ++");
//        System.out.println("++ Nice encoding: ++");
//        for(String s: g.getNicerEncoding())
//            System.out.println(s);
//        System.out.println("++ End nice encoding ++");
//        InteractionGraph g2 = InteractionGraph.getGraphFromSimpleEncoding(IOHandler.parseFile("data/"+"testgraphfile.txt"));
//        System.out.println(g2);
//        System.out.println(".................");
//        InteractionGraph g3 = InteractionGraph.getGraphFromNicerEncoding(IOHandler.parseFile("data/"+"nicetest.txt"));
//        System.out.println(g3);
//    }
}
