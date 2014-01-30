package semanticvectors;

import graph.InteractionGraph;
import graph.Relation;
import graph.Term;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import util.IOHandler;
import visualization.PrefuseVisualization;

/**
 *
 * @author philipp
 */
public class SemanticVectorsVisualization {

    public final static String STOPWORD_LIST = "stopwords.txt";
    public static String TERM_VECTOR_FILE = "WSMs/defaulttermvectors.bin";
    public static String TEXT_MINING_FILE = "TMs/defaultTMresults.txt";
    InteractionGraph g;
    Set<String> type_filter;
    ClusterTree overalClusters;

    public ClusterTree clusterFromOveral(List<Term> center) {

        // find out relevant terms
        Collection<Relation> relations = g.getEdges();
        Set<Term> relevant_terms = new HashSet<Term>();
        for (Relation r : relations) {
            if (center.contains(r.getAgent()) && !center.contains(r.getTheme())) {
                relevant_terms.add(r.getTheme());
            } else if (center.contains(r.getTheme()) && !center.contains(r.getAgent())) {
                relevant_terms.add(r.getAgent());
            }
        }

        Map<ClusterTree, ClusterTree> equivalents = new HashMap<ClusterTree, ClusterTree>();
        List<ClusterTree> stillToCheck = new LinkedList<ClusterTree>();

        // apply for upper level
        List<Term> oTerms = new LinkedList<Term>();
        for (Term t : overalClusters.terms) {
            if (relevant_terms.contains(t)) {
                oTerms.add(t);
            }
        }

        ClusterTree oTree = new ClusterTree(overalClusters.getIGraph(), oTerms, overalClusters.typeFilter, center);
        equivalents.put(overalClusters, oTree);
        Map<Integer, Set<Term>> oClusters = new HashMap<Integer, Set<Term>>();
        Map<Integer, ClusterTree> oSubs = new HashMap<Integer, ClusterTree>();
        int count = 0;
        if (oTerms.size() <= ClusterTree.MAX_CLUSTER_SIZE) {
            for (int i = 0; i < oTerms.size(); i++) {
                Set<Term> tmp = new HashSet<Term>();
                tmp.add(oTerms.get(i));
                oClusters.put(i, tmp);
            }
            oTree.clusters = oClusters;
            oTree.subclusters = oSubs;
        } else {
            for (Integer i : overalClusters.clusters.keySet()) {
                ClusterTree old = (overalClusters.subclusters != null && overalClusters.subclusters.containsKey(i))
                        ? overalClusters.subclusters.get(i) : null;
                Set<Term> old_terms = overalClusters.clusters.get(i);
                List<Term> new_terms = new LinkedList<Term>();
                for (Term t : old_terms) {
                    if (relevant_terms.contains(t)) {
                        new_terms.add(t);
                    }
                }
                if (!new_terms.isEmpty() && old != null) {
                    ClusterTree new_tree = new ClusterTree(overalClusters.graph, new_terms, overalClusters.typeFilter, center);
                    equivalents.put(old, new_tree);
                    stillToCheck.add(old);
                    oClusters.put(count, new HashSet<Term>(new_terms));
                    oSubs.put(count, new_tree);
                    new_tree.parent = oTree;
                    count++;
                } else if (!new_terms.isEmpty()) {
                    oClusters.put(count, new HashSet<Term>(new_terms));
                    count++;
                }
            }
            oTree.clusters = oClusters;
            oTree.subclusters = oSubs;
        }

        // check sub-levels
        while (!stillToCheck.isEmpty()) {
            ClusterTree current = stillToCheck.remove(0);
            ClusterTree sTree = equivalents.get(current);

            Map<Integer, Set<Term>> sClusters = new HashMap<Integer, Set<Term>>();
            Map<Integer, ClusterTree> sSubs = new HashMap<Integer, ClusterTree>();
            count = 0;
            if (sTree.terms.size() <= ClusterTree.MAX_CLUSTER_SIZE) {
                for (int i = 0; i < sTree.terms.size(); i++) {
                    Set<Term> tmp = new HashSet<Term>();
                    tmp.add(sTree.terms.get(i));
                    sClusters.put(i, tmp);
                }
                sTree.clusters = sClusters;
                sTree.subclusters = sSubs;
            } else {
                for (Integer i : current.clusters.keySet()) {
                    ClusterTree old = current.subclusters.containsKey(i) ? current.subclusters.get(i) : null;
                    Set<Term> old_terms = current.clusters.get(i);
                    List<Term> new_terms = new LinkedList<Term>();
                    for (Term t : old_terms) {
                        if (relevant_terms.contains(t)) {
                            new_terms.add(t);
                        }
                    }
                    if (!new_terms.isEmpty() && old != null) {
                        ClusterTree new_tree = new ClusterTree(overalClusters.graph, new_terms, overalClusters.typeFilter, center);
                        equivalents.put(old, new_tree);
                        stillToCheck.add(old);
                        sClusters.put(count, new HashSet<Term>(new_terms));
                        sSubs.put(count, new_tree);
                        new_tree.parent = sTree;
                        count++;
                    } else if (!new_terms.isEmpty()) { //if(!new_terms.isEmpty() && old == null){
                        sClusters.put(count, new HashSet<Term>(new_terms));
                        count++;
                    }
                }
                sTree.clusters = sClusters;
                sTree.subclusters = sSubs;
            }
            // merge pass-through nodes
            if (sTree.parent != null && sTree.parent.parent != null
                    && sTree.parent.clusters.size() == 1 && sTree.parent.subclusters.size() == 1) {
                // get subcluster number
                int subcluster_number = -1;
                for (int key : sTree.parent.parent.subclusters.keySet()) {
                    if (sTree.parent.parent.subclusters.get(key).equals(sTree.parent)) {
                        subcluster_number = key;
                        break;
                    }
                }
                sTree.parent.parent.subclusters.put(subcluster_number, sTree);
                sTree.parent = sTree.parent.parent;
            }

        }

        // merge with parent where possible
        stillToCheck = new LinkedList<ClusterTree>();
        stillToCheck.add(oTree);
        while (!stillToCheck.isEmpty()) {
            ClusterTree current = stillToCheck.remove(0);
            if (current.clusters.size() < ClusterTree.NUM_CLUSTERS && current.terms.size() > ClusterTree.MAX_CLUSTER_SIZE) {
                int n_terms = -1;
                int best_fit = -1;
                for (int i : current.clusters.keySet()) { // determine best cluster to move up
                    if (current.clusters.get(i).size() > n_terms
                            && (current.clusters.get(i).size() + current.clusters.size() - 1 <= ClusterTree.NUM_CLUSTERS
                            || (current.hasSubCluster(i) && current.clusters.size() + current.subclusters.get(i).clusters.size() - 1 <= ClusterTree.NUM_CLUSTERS))) {
                        n_terms = current.clusters.get(i).size();
                        best_fit = i;
                    }
                }
                if (best_fit != -1) {
                    if (current.hasSubCluster(best_fit)) {
                        int append_num = current.clusters.size();
                        current.clusters.remove(best_fit);
                        ClusterTree removed = current.subclusters.remove(best_fit);
                        int counting = -1;
                        for (int i : removed.clusters.keySet()) {
                            if (counting == -1) {
                                current.clusters.put(best_fit, removed.clusters.get(i));
                                if (removed.hasSubCluster(i)) {
                                    current.subclusters.put(best_fit, removed.subclusters.get(i));
                                }
                            } else {
                                current.clusters.put(append_num + counting, removed.clusters.get(i));
                                if (removed.hasSubCluster(i)) {
                                    current.subclusters.put(append_num + counting, removed.subclusters.get(i));
                                }
                            }
                            counting++;
                        }
                    } else {
                        int append_num = current.clusters.size();
                        Set<Term> removed = current.clusters.remove(best_fit);
                        int counting = -1;
                        for (Term t : removed) {
                            if (counting == -1) {
                                Set<Term> single_set = new HashSet<Term>();
                                single_set.add(t);
                                current.clusters.put(best_fit, single_set);
                            } else {
                                Set<Term> single_set = new HashSet<Term>();
                                single_set.add(t);
                                current.clusters.put(append_num + counting, single_set);
                            }
                            counting++;
                        }
                    }
                }
            }
            stillToCheck.addAll(current.subclusters.values());
        }

        return oTree;
    }

    public void setOveral(ClusterTree overal) {
        this.overalClusters = overal;
    }

    public ClusterTree getOveral() {
        return overalClusters;
    }

    public ClusterTree clusterNodesSemantically(Term[] center) {
        List<Term> terms = new ArrayList<Term>(g.getVertices());
        // remove term in question
        for (Term t : center) {
            terms.remove(t);
            if (t.getAliases() != null) {
                for (String s : t.getAliases()) {
                    terms.remove(new Term(s));
                }
            }
        }

        ClusterTree clusters = new ClusterTree(g, terms, type_filter, Arrays.asList(center)); // ClusterResults.Type.ENVIRFACT
        boolean created = clusters.create();

        if (created) {

            return clusters;
        }
        return null;
    }

    public static Set<String> getTypeFilter(String file) {
        if (file == null) {
            return null;
        }
        Set<String> result = new HashSet<String>();
        Vector<String> lines = IOHandler.parseFile(file);
        for (String l : lines) {
            if (l.trim().length() > 0) {
                result.add(l.trim());
            }
        }
        return result;
    }

    public void setTypeFilter(Set<String> type_filter) {
        this.type_filter = type_filter;
    }

    public InteractionGraph getInteractionGraph() {
        return g;
    }

    public void setInteractionGraph(InteractionGraph g) {
        this.g = g;
    }

    public static void main(String[] args) {
        // avoid all the sys.err output from the semantic vectors package
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        SemanticVectorsVisualization svv = new SemanticVectorsVisualization();

        Set<String> my_filter = new HashSet<String>();
        my_filter.add("PHENOTYPE, GENE, FUNCTION");
        svv.setTypeFilter(my_filter);
        ClusterTree tmp = ClusterTree.decode(IOHandler.parseFile("Visualizations/default.vis").get(0));
        svv.setInteractionGraph(tmp.getIGraph());
        svv.setOveral(tmp);

        PrefuseVisualization vis = new PrefuseVisualization(svv);
        vis.visualizeClusters(tmp, new Term[0]);
    }
}
