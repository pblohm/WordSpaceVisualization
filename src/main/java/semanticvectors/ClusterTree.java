package semanticvectors;

import graph.InteractionGraph;
import graph.Term;
import util.IOHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pitt.search.semanticvectors.ObjectVector;

/**
 *
 * @author philipp
 */
public class ClusterTree {
    
    public static int MAX_CLUSTER_SIZE = 15;
    public static int MIN_EVIDENCES = 1;
    public static int NUM_CLUSTERS = 15;
    
    InteractionGraph graph;
    List<Term> terms;
    int num_clusters;
    Set<String> typeFilter;
    int num_evidences;
    List<Term> centers;
    
    Map<Integer, Set<Term>> clusters;
    Map<Integer, ClusterTree> subclusters;
    ClusterTree parent;
    
    public String myoffset = "";

    public ClusterTree(InteractionGraph g, List<Term> terms, Set<String> typeFilter, List<Term> centers){
        graph = g;
        this.terms = new LinkedList<Term>();
        // apply term filter
        if(typeFilter == null || typeFilter.isEmpty()){
            this.terms = terms;
        } else{
            for(Term t: terms){
                boolean passed_filter = false;
                for(String type: typeFilter){
                    if(t.getTypes().keySet().contains(type)){
                        passed_filter = true;
                        break;
                    }
                }
                if(passed_filter){
                    this.terms.add(t);
                }
            }
        }
        num_clusters = NUM_CLUSTERS;
        this.typeFilter = typeFilter;
        num_evidences = MIN_EVIDENCES;
        this.centers = centers;
    }
    
    public boolean create(){
        if (terms.size() > MAX_CLUSTER_SIZE) {
            subclusters = new HashMap<Integer,ClusterTree>();
            
            boolean propercluster = false;
            int[] clusterMappings = null;
            List<ObjectVector> filtered = null;
            
            int tries = 0;
            while(!propercluster){
                // cluster terms
                ClusterResults.Tuple<int[], List<ObjectVector>> clusterRes = ClusterResults.cluster(graph, terms, num_clusters, num_evidences, centers);

                clusterMappings = clusterRes.x;
                filtered = clusterRes.y;
                if(clusterMappings.length==0){
                    return false;
                }
                int first = clusterMappings[0];
                for(int i = 1; i<clusterMappings.length; i++){
                    if(clusterMappings[i]!=first){
                        propercluster = true;
                        break;
                    }
                }
                if(tries++ > 10){
                    int mycount = 0;
                    for(int i=0; i<clusterMappings.length; i++){
                        clusterMappings[i] = mycount % num_clusters;
                        mycount++;
                    }
                    propercluster = true;
                }
            }
            
            

            // distribute to clusters
            clusters = new HashMap<Integer, Set<Term>>();
            for (int i = 0; i < clusterMappings.length; i++) {
                if (clusters.containsKey(clusterMappings[i])) {
                    clusters.get(clusterMappings[i]).add((Term) filtered.get(i).getObject());
                } else {
                    Set<Term> myset = new HashSet<Term>();
                    myset.add((Term) filtered.get(i).getObject());
                    clusters.put(clusterMappings[i], myset);
                }
            }

            // create subclusters where necessary
            for (Integer i : clusters.keySet()) {
                if (clusters.get(i).size() > MAX_CLUSTER_SIZE ) { //&& i != num_clusters
                    ClusterTree subcluster = new ClusterTree(graph, new ArrayList<Term>(clusters.get(i)), typeFilter, centers);
                    subcluster.setParent(this);
                    subcluster.myoffset= "__"+myoffset;
                    subcluster.create();
                    subclusters.put(i, subcluster);
                }
            }
        } else{
            clusters = new HashMap<Integer, Set<Term>>();
            for(int i=0; i<terms.size(); i++){
                Set<Term> s = new HashSet<Term>();
                s.add(terms.get(i));
                clusters.put(i, s);
            }
        }
        return true;
    }
    
    public void setParent(ClusterTree parent){
        this.parent = parent;
    }
    
    public ClusterTree getParent(){
        return parent;
    }
    
    public Map<Integer, Set<Term>> getClusters(){
        return clusters;
    }
    
    public List<Term> getTerms(){
        return terms;
    }
    
    public void setClusters(Map<Integer, Set<Term>> clusters){
        this.clusters = clusters;
    }
    
    public boolean hasSubCluster(int i){
        if(subclusters == null) return false;
        return subclusters.containsKey(i);
    }
    
    public Set<String> getTypeFilter(){
        return typeFilter;
    }
    
    public ClusterTree getSubCluster(int i){
        return subclusters.get(i);
    }
    
    public Map<Integer, ClusterTree> getSubClusters(){
        return subclusters;
    }
    
    public void setSubClusters(Map<Integer, ClusterTree> subclusters){
        this.subclusters = subclusters;
    }
    
    
    public InteractionGraph getIGraph(){
        return graph;
    }
    
    public void printInfo2(String offset){
        System.out.println(offset+terms);
        for(int i: clusters.keySet()){
            if(!subclusters.containsKey(i)){
                System.out.println(offset+" ."+clusters.get(i));
            }
        }
        for(ClusterTree ct : subclusters.values()){
            ct.printInfo2(offset+"  ");
        }
    }
    
    public void printInfo(){
        System.out.println("..............");
        System.out.println("INFO:");
        System.out.println("Terms:");
        for(Term t: terms){
            System.out.print(t.getName()+",");
        }
        System.out.println();
        System.out.println("Amount terms: "+terms.size());
        System.out.println("Centers:");
        for(Term t: centers){
            System.out.print(t.getName()+",");
        }
        System.out.println();
        System.out.println("Amount centers: "+centers.size());
        System.out.println("iGraph: "+graph);
        int cluster_count = 0;
        int level_count = 0;
        List<ClusterTree> nextLevel = new LinkedList<ClusterTree>();
        nextLevel.add(this);
        System.out.println("Highest level clusters: "+clusters.size());
        System.out.println("Subclusters: "+subclusters.size());
        while(!nextLevel.isEmpty()){
            level_count++;
            List<ClusterTree> tmp = new LinkedList<ClusterTree>();
            for(ClusterTree c : nextLevel){
                if(c.subclusters!=null){
                    tmp.addAll(c.subclusters.values());
                    cluster_count += c.clusters.size();
                }
            }
            nextLevel = tmp;
        }
        System.out.println("Clusters: "+cluster_count);
        System.out.println("Levels: "+level_count);
        
        System.out.println("..............");
    }
        
    
    private static String separator = "<-ct->";
    private static String list_sep = "<-ct-list->";
    private static String list_sep2 = "<-ct-list2->";
    
    private String getCoreEncoding(){
        String enc = "";
        // encode terms
        if(terms == null){
            enc += "null"+separator;
        } else{
            String term_string = "";
            for(Term t: terms){
                term_string += t.getSimpleEncoding()+list_sep;
            }
            if(term_string.length()>0){
                enc += term_string.substring(0, term_string.length()-list_sep.length())+separator;
            } else{
                enc += term_string + separator;
            }
        }
        // encode clusters
        if(clusters == null){
            enc += "null"+separator;
        } else{
            String cl_string = "";
            for(Integer i : clusters.keySet()){
                Set<Term> value = clusters.get(i);
                 cl_string += i;
                for(Term t: value){
                    cl_string += list_sep2+t.getSimpleEncoding();
                }
                cl_string += list_sep;
            }
            if(cl_string.length()>0){
                enc += cl_string.substring(0,cl_string.length()-list_sep.length()) + separator;
            }else{
                enc += cl_string + separator;
            }
        }
        return enc;
    }
    
    public String getEncoding(){
        String enc = "";
        // encode parameters
        enc += num_clusters+","+num_evidences+","+MAX_CLUSTER_SIZE+separator;
        // encode type filter
        if(typeFilter == null){
            enc += "null"+separator;
        } else{
            String tf_string = "";
            for(String s: typeFilter){
                tf_string += s+list_sep;
            }
            if(tf_string.length()>0){
                enc += tf_string.substring(0, tf_string.length()-list_sep.length())+separator;
            } else{
                enc += tf_string + separator;
            }
        }
        // encode centers
        if(centers == null){
            enc += "null"+separator;
        } else{
            String term_string = "";
            for(Term t: centers){
                term_string += t.getSimpleEncoding()+list_sep;
            }
            if(term_string.length()>0){
                enc += term_string.substring(0, term_string.length()-list_sep.length())+separator;
            } else{
                enc += term_string + separator;
            }
        }
        // encode interaction graph
//        Vector<String> igraph_enc = graph.getSimpleEncoding();
//        if(igraph_enc == null){
//            enc += "null"+separator;
//        } else {
//            for(String s : igraph_enc){
//                enc += s+list_sep;
//            }
//            enc = enc.substring(0,enc.length()-list_sep.length())+separator;
//        }
        enc += SemanticVectorsVisualization.TEXT_MINING_FILE+separator;
        // create IDs
        List<ClusterTree> stillToDo = new LinkedList<ClusterTree>();
        Map<ClusterTree, Integer> idMap = new HashMap<ClusterTree, Integer>();
        int id = 0;
        idMap.put(this, id++);
        if(this.subclusters!=null){
            stillToDo.addAll(this.subclusters.values());
        }
        while(!stillToDo.isEmpty()){
            ClusterTree tmp = stillToDo.remove(0);
            idMap.put(tmp, id++);
            if(tmp.subclusters!=null){
                stillToDo.addAll(tmp.subclusters.values());
            }
        }
        
        // encode all trees
        List<ClusterTree> stillToDo2 = new LinkedList<ClusterTree>();
        stillToDo2.add(this);
        while(!stillToDo2.isEmpty()){
            ClusterTree tmp2 = stillToDo2.remove(0);
            
            // encode ID
            enc += idMap.get(tmp2)+separator;
            // encode terms & clusters
            enc += tmp2.getCoreEncoding();
            
            // encode subclusters
            if(tmp2.subclusters == null){
                enc += "null"+separator;
            } else{
                String sub_string = "";
                for(Integer i: tmp2.subclusters.keySet()){
                    sub_string += i+","+idMap.get(tmp2.subclusters.get(i))+list_sep;
                }
                if(sub_string.length()>0){
                    enc += sub_string.substring(0, sub_string.length()-list_sep.length())+separator;
                } else{
                    enc += sub_string + separator;
                }
                stillToDo2.addAll(tmp2.subclusters.values());
            }
            
            // encode parent
            enc += idMap.get(tmp2.getParent())+separator;
        }

        enc = enc.substring(0,enc.length()-separator.length());

        return enc;
    }
    
    private String escapeSeparator(String text, String separator){
        if(text.contains(separator)){
            return text.replaceAll(separator, "");
        } else{
            return text;
        }
    }
    
    public static ClusterTree decode(String encoding){
        
        String[] parts = encoding.split(separator);
        
        // decode parameters
        String[] parameter_parts = parts[0].split(",");
        NUM_CLUSTERS = Integer.parseInt(parameter_parts[0]);
        MIN_EVIDENCES = Integer.parseInt(parameter_parts[1]);
        MAX_CLUSTER_SIZE = Integer.parseInt(parameter_parts[2]);
        
        // decode type-filter
        Set<String> tFilter = null;
        if(!parts[1].equals("null")){
            tFilter = new HashSet<String>();
            String[] filter_parts = parts[1].split(list_sep);
            tFilter.addAll(Arrays.asList(filter_parts));
        }
        
        // decode centers
        List<Term> centerTerms = null;
        if(!parts[2].equals("null")){
            centerTerms = new LinkedList<Term>();
            String[] center_parts = parts[2].split(list_sep);
            for(String s: center_parts){
                Term tmpterm = Term.getTermFromSimpleEncoding(s);
                if(tmpterm!=null){
                    centerTerms.add(tmpterm);
                }
            }
        }
        
        // decode interaction graph
//        InteractionGraph iGraph = null;
//        if(!parts[3].equals("null")){
//            String[] graph_parts = parts[3].split(list_sep);
//            Vector<String> lines = new Vector<String>(Arrays.asList(graph_parts));
//            iGraph = InteractionGraph.getGraphFromSimpleEncoding(lines);
//        }
        InteractionGraph iGraph = InteractionGraph.getGraphFromNicerEncoding(
                IOHandler.parseFile(parts[3]));
        SemanticVectorsVisualization.TEXT_MINING_FILE = parts[3];
        
        // decode all trees
        Map<Integer, ClusterTree> idMap = new HashMap<Integer, ClusterTree>();
        Map<ClusterTree, Map<Integer,Integer>> childMap = new HashMap<ClusterTree,Map<Integer,Integer>>();
        Map<ClusterTree, Integer> parentMap = new HashMap<ClusterTree, Integer>();
        
        for(int i=4; i<parts.length; i+=5){ // start at 4, if iGraph is in
            // decode id
            int id = Integer.parseInt(parts[i]);
            
            // decode terms
            List<Term> termlist = null;
            if(!parts[i+1].equals("null")){
                termlist = new LinkedList<Term>();
                String[] term_parts = parts[i+1].split(list_sep);
                for(String s: term_parts){
                    termlist.add(Term.getTermFromSimpleEncoding(s));
                }
            }
            
            // decode cluster
            Map<Integer, Set<Term>> clustermap = null;
            if(!parts[i+2].equals("null")){
                clustermap = new HashMap<Integer, Set<Term>>();
                String[] cluster_parts = parts[i+2].split(list_sep);
                for(String s: cluster_parts){
                    String[] entry_parts = s.split(list_sep2);
                    int num = Integer.parseInt(entry_parts[0]);
                    Set<Term> entry_terms = new HashSet<Term>();
                    for(int j=1; j<entry_parts.length; j++){
                        entry_terms.add(Term.getTermFromSimpleEncoding(entry_parts[j]));
                    }
                    clustermap.put(num, entry_terms);
                }
            }
            
            // build ClusterTree
            ClusterTree c = new ClusterTree(iGraph,termlist,tFilter,centerTerms);
            c.setClusters(clustermap);
            idMap.put(id, c);
            
            // decode subcluster
            Map<Integer, Integer> children = null;
            if(!parts[i+3].equals("null")){
                children = new HashMap<Integer,Integer>();
                String[] sub_parts = parts[i+3].split(list_sep);
                for(String s: sub_parts){
                    if(s.trim().length()>0){
                        String[] kv = s.split(",");
                        children.put(Integer.parseInt(kv[0]), Integer.parseInt(kv[1]));
                    }
                }
                childMap.put(c,children);
            }
            
            // decode parent
            if(!parts[i+4].equals("null")){
                int parent = Integer.parseInt(parts[i+4]);
                parentMap.put(c, parent);
            }
        }
        
        // resolve IDs
        for(ClusterTree t: childMap.keySet()){
            Map<Integer, ClusterTree> mysubs = new HashMap<Integer, ClusterTree>();
            Map<Integer, Integer> idsubs = childMap.get(t);
            for(Integer i: idsubs.keySet()){
                mysubs.put(i, idMap.get(idsubs.get(i)));
            }
            t.setSubClusters(mysubs);
        }
        for(ClusterTree t: parentMap.keySet()){
            t.setParent(idMap.get(parentMap.get(t)));
        }
        
        return idMap.get(0);
    }
    
}
