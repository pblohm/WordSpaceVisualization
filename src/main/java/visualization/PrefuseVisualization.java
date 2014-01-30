package visualization;

import graph.InteractionGraph;
import graph.Relation;
import graph.Term;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.html.HTMLEditorKit;
import pitt.search.semanticvectors.LSA;
import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.layout.graph.FruchtermanReingoldLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import semanticvectors.ClusterTree;
import semanticvectors.MyBuildIndex;
import semanticvectors.MyIndexFiles;
import semanticvectors.SemanticVectorsVisualization;
import util.IOHandler;

/**
 *
 * @author philipp
 */
public class PrefuseVisualization {
    
    JFrame mainframe;
    JPanel graph_panel;
    JTextField search_field;
    InteractionGraph mainGraph;
    ClusterTree topLevelClusters;
    ClusterTree currentClusters;
    Graph originalGraph;
    String nameString = "Name";
    String sizeString = "Size";
    String clusterString = "Clusterno";
    String termstring = "Term";
    String secondtermstring = "SecondTerm";
    String evidenceString = "Evidences";
    Term[] centers;
    boolean lowest_level;
    List<String> all_searchable_terms;
    private static final String COMMIT_ACTION = "commit";
    boolean offline = true;

    MyDisplay display;
    SemanticVectorsVisualization svv;
    
    public PrefuseVisualization(SemanticVectorsVisualization svv){
        this.svv = svv;
        all_searchable_terms = svv.getInteractionGraph().getAllNames();
    }

    public void visualizeClusters(ClusterTree clusters, Term[] centers) {

        this.centers = centers;
        topLevelClusters = clusters;
        currentClusters = topLevelClusters;
        mainGraph = clusters.getIGraph();
        lowest_level = amILowestLevel(clusters);

        Graph graph = buildGraph(clusters);

        originalGraph = graph;
        displayGraph(graph);

    }

    private Graph buildGraph(ClusterTree clusters) {
        if(clusters == null) {
            return null;
        }
        
        if(!amILowestLevel(clusters)){
        //initialize Prefuse graph
        Graph graph = new Graph();
        graph.addColumn(nameString, String.class);
        graph.addColumn(sizeString, Integer.class);
        graph.addColumn(clusterString, Integer.class);
        graph.addColumn(evidenceString, Integer.class);
        graph.addColumn(termstring, String.class);
        graph.addColumn(secondtermstring, Term.class);

        // create center nodes
        Node[] center_nodes = new Node[centers.length];
        for(int i=0; i<center_nodes.length; i++){
            center_nodes[i] = graph.addNode();
            center_nodes[i].set(nameString, centers[i].getName());
            center_nodes[i].set(sizeString, 20);
            center_nodes[i].set(clusterString, -1);
        }

        // create peripheral nodes and edges to them
        int totalsum = 0;
        List<Edge> myEdges = new LinkedList<Edge>();
        List<Integer> evs = new LinkedList<Integer>();
        for (Integer i : clusters.getClusters().keySet()) {
            Set<Term> cluster = clusters.getClusters().get(i);
            Term[] names = getMostImportantTerms(cluster, mainGraph);
            String topnames = "";
            for (Term t : names) {
                topnames += t.getName() + "\n";
            }
            Node n1 = graph.addNode();
            n1.set(nameString, topnames);
            n1.set(sizeString, cluster.size());
            n1.set(clusterString, i);

            for (int k = 0; k < center_nodes.length; k++) {
                
                int evidence = 0;
                String terms_string = "";
                for (Term t : cluster) {
                    terms_string += t.getName()+"\n";
                    if(mainGraph.getInEdges(t)!=null){
                        for(Relation rel : mainGraph.getInEdges(t)){
                            if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                                evidence += rel.getEvidences().size();
                            }
                        }
                    }
                    if(mainGraph.getOutEdges(t)!=null){
                        for(Relation rel : mainGraph.getOutEdges(t)){
                            if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                                evidence += rel.getEvidences().size();
                            }
                        }
                    }
                }
                if(evidence>0){
                    Edge e1 = graph.addEdge(n1, center_nodes[k]);
                        e1.set(secondtermstring, centers[k]); // added
                        e1.set(termstring, terms_string); // added
                    myEdges.add(e1);
                    evs.add(evidence);
                    totalsum += evidence;
                
                    e1.set(evidenceString, (int) (evidence / 15.0)); // oder hier Margin größer
                }
            }
        }
        // create edges between center nodes
        for(int i=0; i< centers.length; i++){
            for(int j=i; j<centers.length; j++){
                int connections = 0;
                Collection<Relation> itoj = mainGraph.findEdgeSet(centers[i], centers[j]);
                Collection<Relation> jtoi = mainGraph.findEdgeSet(centers[j], centers[i]);
                if(itoj!=null){
                    connections += itoj.size();
                }
                if(jtoi!=null){
                    connections += jtoi.size();
                }
                if(connections>0){
                    Edge e1 = graph.addEdge(center_nodes[i], center_nodes[j]);
                    myEdges.add(e1);
                    evs.add(connections);
                    totalsum += connections;
                }
            }
        }
        
        // scale edges
        for(int i=0; i<myEdges.size(); i++){
            myEdges.get(i).set(evidenceString, (int) (100*evs.get(i)/(double)totalsum));
        }
        
        
        return graph;
        } else {
            List<Term> myCluster = currentClusters.getTerms();

            // create Prefuse graph
            Graph graph = new Graph();
            graph.addColumn(nameString, String.class);
            graph.addColumn(sizeString, Integer.class);
            graph.addColumn(clusterString, Integer.class);
            graph.addColumn(termstring, Term.class);
            graph.addColumn(secondtermstring, Term.class);
            graph.addColumn(evidenceString, Integer.class);

            // create center nodes
            Node[] center_nodes = new Node[centers.length];
            for (int i = 0; i < center_nodes.length; i++) {
                center_nodes[i] = graph.addNode();
                center_nodes[i].set(nameString, centers[i].getName());
                center_nodes[i].set(sizeString, 20);
                center_nodes[i].set(clusterString, -1);
            }
        
            // create peripheral nodes and edges to them
            for (Term t : myCluster) {
                Node n1 = graph.addNode();
                n1.set(nameString, t.getName());
                n1.set(sizeString, 5);
                n1.set(clusterString, -2);
                n1.set(termstring, t);

                for (int k = 0; k < center_nodes.length; k++) {
                    int evidence = 0;
                    for(Relation rel : mainGraph.getInEdges(t)){
                        if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                            evidence += rel.getEvidences().size();
                        }
                    }
                    for(Relation rel : mainGraph.getOutEdges(t)){
                        if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                            evidence += rel.getEvidences().size();
                        }
                    }
                    if(evidence>0){
                        Edge e1 = graph.addEdge(n1, center_nodes[k]);
                        e1.set(termstring, t);
                        e1.set(secondtermstring, centers[k]);
                        e1.set(evidenceString, evidence);
                    }
                }
            }
            
            // create edges between center nodes
            for (int i = 0; i < centers.length; i++) {
                for (int j = i; j < centers.length; j++) {
                    int connections = 0;
                    Collection<Relation> itoj = mainGraph.findEdgeSet(centers[i], centers[j]);
                    Collection<Relation> jtoi = mainGraph.findEdgeSet(centers[j], centers[i]);
                    if (itoj != null) {
                        connections += itoj.size();
                    }
                    if (jtoi != null) {
                        connections += jtoi.size();
                    }
                    if (connections > 0) {
                        Edge e1 = graph.addEdge(center_nodes[i], center_nodes[j]);
                        e1.set(termstring, centers[i]);
                        e1.set(secondtermstring, centers[j]);
                        e1.set(evidenceString, connections);
                    }
                }
            }
            return graph;
        }
    }

    private Term[] getMostImportantTerms(Set<Term> terms, InteractionGraph igraph) {
        Term[] result = new Term[Math.min(3, terms.size())];
        int[] values = new int[result.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.MIN_VALUE;
        }
        for (Term term : terms) {
            int value = 0;
            if(centers.length>0){
                for(Term c : centers){
                    if(igraph.findEdgeSet(term, c)!=null){
                        for(Relation r : igraph.findEdgeSet(term, c)){
                            value += r.getEvidences().size();
                        }
                    }
                }
            } else{
                if(igraph.getIncidentEdges(term)!=null){
                    for(Relation r: igraph.getIncidentEdges(term)){
                        value += r.getEvidences().size();
                    }
                }
            }
            
            int index = values.length - 1;
            boolean on = value > values[index];
            while (on && index >= 0) {
                if (index < values.length - 1) {
                    values[index + 1] = values[index];
                    result[index + 1] = result[index];
                }
                values[index] = value;
                result[index] = term;

                on = index == 0 ? false : values[index] > values[--index];
            }
        }
        return result;
    }

    public void levelUp() {
        if (!lowest_level && currentClusters.getParent() != null) {
            currentClusters = currentClusters.getParent();
        }

        Graph graph = buildGraph(currentClusters);
        if(display!=null){
            display.cleanup();
            display.getVisualization().reset();
        }
        display = createDisplay(graph);
        lowest_level = amILowestLevel(currentClusters);
                
        graph_panel.removeAll();
        graph_panel.add(display);
        graph_panel.validate();
        mainframe.setVisible(true);
        System.gc();

    }
    
    public void newVisualization(ClusterTree newtree){

        all_searchable_terms = svv.getInteractionGraph().getAllNames();
        this.centers = new Term[0];
        topLevelClusters = newtree;
        currentClusters = topLevelClusters;
        mainGraph = newtree.getIGraph();
        lowest_level = amILowestLevel(currentClusters);

        Graph graph = buildGraph(newtree);

        originalGraph = graph;
        changeGraph(graph);
    }

    public void newSearch(String search){
        ClusterTree clusters = null;
        boolean overview = false;
        
        if(search.trim().length()==0){
            clusters = svv.getOveral();
            centers = new Term[0];
            overview = true;
        }else{
            // split by coma
            String[] words = search.split(",");
            // check for known terms
            List<Term> t = new ArrayList<Term>(words.length);
            for(String w: words){
                Term term = svv.getInteractionGraph().getTerm(w.trim().toLowerCase());
                if(term!=null){
                    t.add(term);
                }
            }
            Term[] terms = t.toArray(new Term[0]);
            centers = terms;
            // create new graph
            if(offline){
                clusters = svv.clusterFromOveral(t);
            } else{
                clusters = svv.clusterNodesSemantically(terms);            
            }
        }
                
        topLevelClusters = clusters;
        currentClusters = clusters;
        lowest_level = amILowestLevel(currentClusters);
        
        Graph g = buildGraph(clusters);
        originalGraph = g;
        mainGraph = clusters.getIGraph();
        // display
        changeGraph(g);
        if(!overview && centers.length == 0){
            JOptionPane.showMessageDialog(mainframe, "No terms found for '"+search+"'.");
        } else if(clusters.getTerms().isEmpty()){
            JOptionPane.showMessageDialog(mainframe, "No relations found for '"+search+"'.");
        }

    }
    
    private void changeGraph(Graph graph){
        if(display!=null){
            display.cleanup();
            display.getVisualization().reset();
        }
        display = createDisplay(graph);
        graph_panel.removeAll();
        graph_panel.add(display);
        graph_panel.validate();
        mainframe.setVisible(true);        
    }
    
    private boolean amILowestLevel(ClusterTree c){
        if(c.getSubClusters() != null && !c.getSubClusters().isEmpty()){
            return false;
        }
        for(Set<Term> st : c.getClusters().values()){
            if(st.size()>1){
                return false;
            }
        }
        return true;
    }
    
    public void changeDisplay(int cluster) {
        if (cluster < 0) {
            currentClusters = topLevelClusters;
            if(display!=null){
                display.cleanup();
                display.getVisualization().reset();
            }
            display = createDisplay(originalGraph);
            lowest_level = amILowestLevel(currentClusters);
        } else if (currentClusters.hasSubCluster(cluster) && !amILowestLevel(currentClusters.getSubCluster(cluster))){
            currentClusters = currentClusters.getSubCluster(cluster);
            Graph graph = buildGraph(currentClusters);
            if(display!=null){
                display.cleanup();
                display.getVisualization().reset();
            }
            display = createDisplay(graph);
            lowest_level = false;
        } else {
            Set<Term> myCluster = currentClusters.getClusters().get(cluster);

            // create Prefuse graph
            Graph graph = new Graph();
            graph.addColumn(nameString, String.class);
            graph.addColumn(sizeString, Integer.class);
            graph.addColumn(clusterString, Integer.class);
            graph.addColumn(termstring, Term.class);
            graph.addColumn(secondtermstring, Term.class);
            graph.addColumn(evidenceString, Integer.class);

            // create center nodes
            Node[] center_nodes = new Node[centers.length];
            for (int i = 0; i < center_nodes.length; i++) {
                center_nodes[i] = graph.addNode();
                center_nodes[i].set(nameString, centers[i].getName());
                center_nodes[i].set(sizeString, 20);
                center_nodes[i].set(clusterString, -1);
            }
        
            // create peripheral nodes and edges to them
            for (Term t : myCluster) {
                Node n1 = graph.addNode();
                n1.set(nameString, t.getName());
                n1.set(sizeString, 5);
                n1.set(clusterString, -2);
                n1.set(termstring, t);

                for (int k = 0; k < center_nodes.length; k++) {
                    int evidence = 0;
                    for(Relation rel : mainGraph.getInEdges(t)){
                        if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                            evidence += rel.getEvidences().size();
                        }
                    }
                    for(Relation rel : mainGraph.getOutEdges(t)){
                        if(rel.getAgent().equals(centers[k]) || rel.getTheme().equals(centers[k])){
                            evidence += rel.getEvidences().size();
                        }
                    }
                    if(evidence>0){
                        Edge e1 = graph.addEdge(n1, center_nodes[k]);
                        e1.set(termstring, t);
                        e1.set(secondtermstring, centers[k]);
                        e1.set(evidenceString, evidence);
                    }
                }
            }
            
            // create edges between center nodes
            for (int i = 0; i < centers.length; i++) {
                for (int j = i; j < centers.length; j++) {
                    int connections = 0;
                    Collection<Relation> itoj = mainGraph.findEdgeSet(centers[i], centers[j]);
                    Collection<Relation> jtoi = mainGraph.findEdgeSet(centers[j], centers[i]);
                    if (itoj != null) {
                        connections += itoj.size();
                    }
                    if (jtoi != null) {
                        connections += jtoi.size();
                    }
                    if (connections > 0) {
                        Edge e1 = graph.addEdge(center_nodes[i], center_nodes[j]);
                        e1.set(termstring, centers[i]);
                        e1.set(secondtermstring, centers[j]);
                        e1.set(evidenceString, connections);
                    }
                }
            }
            
            if(display!=null){ 
                display.cleanup();
                display.getVisualization().reset();
            }
            display = createDisplay(graph);
            lowest_level = true;
        }

        graph_panel.removeAll();
        graph_panel.add(display);
        graph_panel.validate();
        mainframe.setVisible(true);
        System.gc();
    }

    public void displayGraph(Graph graph) {
        if(display!=null){
            display.cleanup();
            display.getVisualization().reset();
        }
        display = createDisplay(graph);
        // create a new window to hold the visualization
        mainframe = new JFrame("Topic Graph");
        // ensure application exits when window is closed
        mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainframe.setSize(1500, 1000);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                if(e instanceof OutOfMemoryError){
                    JOptionPane.showMessageDialog(mainframe,"A memory error occurred. "
                            + "Try starting the program with more heap space "
                            + "(java -jar -Xmx2g WordSpaceVisualization.jar).","Error",JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(mainframe,"An unclassified error occurred.","Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel main_panel = new JPanel();
        main_panel.setLayout(new BorderLayout());
        
        // Corpus panel
        JPanel functions_panel = new JPanel();
        functions_panel.setLayout(new GridLayout(18,1));
        JLabel header1 = new JLabel("Word space model:");
        JPanel choose_method = new JPanel();
        choose_method.setLayout(new GridLayout(1,3));
        choose_method.add(new JLabel("Method:"));
        ButtonGroup mygroup = new ButtonGroup();
        final JRadioButton lsa = new JRadioButton("LSA");
        final JRadioButton ri = new JRadioButton("RI");
        ri.setSelected(true);
        mygroup.add(lsa);
        mygroup.add(ri);
        choose_method.add(lsa);
        choose_method.add(ri);
        
        JButton load_corpus = new JButton("Create WSM from corpus");
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout());
        panel3.add(new JLabel("Name:"), BorderLayout.WEST);
        final JTextField vectors_name = new JTextField("my_wsm");
        panel3.add(vectors_name, BorderLayout.CENTER);
        JButton load_vectors = new JButton("Load WSM");
        final JLabel vectors_label = new JLabel("<html><body>Loaded WSM:: <br>"+
                            "defaulttermvectors.bin"+"</body></html>");

        functions_panel.add(header1);
        functions_panel.add(panel3);
        functions_panel.add(choose_method);
        functions_panel.add(load_corpus);
        functions_panel.add(load_vectors);
        JPanel sep_panel2 = new JPanel();
        sep_panel2.setLayout(new BorderLayout());
        sep_panel2.add(vectors_label, BorderLayout.CENTER);
        sep_panel2.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
        functions_panel.add(sep_panel2);
        
        // Vectors panel
        JLabel header2 = new JLabel("Parameters:");
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(new JLabel("Amount clusters:"), BorderLayout.WEST);
        final JTextField param_tf1 = new JTextField("15");
        panel1.add(param_tf1, BorderLayout.CENTER);
        JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout());
        panel4.add(new JLabel("Entities per cluster:"), BorderLayout.WEST);
        final JTextField param_tf4 = new JTextField("15");
        panel4.add(param_tf4, BorderLayout.CENTER);
        JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout());
        panel5.add(new JLabel("Minimum evidences:"), BorderLayout.WEST);
        final JTextField param_tf5 = new JTextField("1");
        panel5.add(param_tf5, BorderLayout.CENTER);
        JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout());
        panel6.add(new JLabel("Type filter:"), BorderLayout.WEST);
        final JTextField type_filter_field = new JTextField("PHENOTYPE, GENE, FUNCTION");
        type_filter_field.setColumns(12);
        panel6.add(type_filter_field, BorderLayout.CENTER);
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        panel2.add(new JLabel("Name:"), BorderLayout.WEST);
        final JTextField clusters_name = new JTextField("my_semantic_visualization");
        panel2.add(clusters_name, BorderLayout.CENTER);
        JButton create_clusters_button = new JButton("Create visualization");
        JButton load_tm = new JButton("Load text mining results");

        functions_panel.add(new JLabel("Text mining results:"));
        functions_panel.add(load_tm);
        JPanel sep_panel = new JPanel();
        sep_panel.setLayout(new BorderLayout());
        final JLabel loaded_tm = new JLabel("<html><body>Loaded TM results: <br>"+
                            "defaultTMresults.txt"+"</body></html>");
        sep_panel.add(loaded_tm, BorderLayout.CENTER);
        sep_panel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
        functions_panel.add(sep_panel);
        functions_panel.add(header2);
        functions_panel.add(panel1);
        functions_panel.add(panel4);
        functions_panel.add(panel5);
        functions_panel.add(panel6);
        functions_panel.add(panel2);
        JPanel sep_panel3 = new JPanel();
        sep_panel3.setLayout(new BorderLayout());
        sep_panel3.add(create_clusters_button, BorderLayout.CENTER);
        sep_panel3.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
        functions_panel.add(sep_panel3);
        
        JButton load_vis = new JButton("Load visualization");
        functions_panel.add(load_vis);
        final JLabel loaded_vis_label = new JLabel("<html><body>Loaded visualization:<br>default.vis</body></html>");
        functions_panel.add(loaded_vis_label);    
        
        load_corpus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File("."));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                int returnVal = fc.showOpenDialog(mainframe);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    JFrame please_wait = new JFrame("New word space model is being created. Please wait.");
                    please_wait.setLocation(500,400);
                    please_wait.setSize(500, 0);
                    please_wait.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    please_wait.setVisible(true);
                    SemanticVectorsVisualization.TERM_VECTOR_FILE = "WSMs/"+vectors_name.getText();
                    if(lsa.isSelected()){
                        try {
                            MyIndexFiles.indexCorpus(file.getPath(), SemanticVectorsVisualization.TERM_VECTOR_FILE, true);
                            LSA.main(new String[]{"-minfrequency","-1","-maxnonalphabetchars","-1",SemanticVectorsVisualization.TERM_VECTOR_FILE});
                            vectors_label.setText("<html><body>Loaded WSM: <br>"+SemanticVectorsVisualization.TERM_VECTOR_FILE.substring(5)+"</body></html>");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(mainframe,"Error. Word space model could not be created","Error",JOptionPane.ERROR_MESSAGE);
                        }
                        
                    } else{
                        MyIndexFiles.indexCorpus(file.getPath(), SemanticVectorsVisualization.TERM_VECTOR_FILE, true);
                        MyBuildIndex.createVectors(new String[]{"-minfrequency","-1","-maxnonalphabetchars","-1",SemanticVectorsVisualization.TERM_VECTOR_FILE});
                        vectors_label.setText("<html><body>Loaded WSM: <br>"+SemanticVectorsVisualization.TERM_VECTOR_FILE.substring(5)+"</body></html>");
                    }
                    please_wait.dispose();
                }
            }
        });
        
        load_vectors.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File("./WSMs"));
                fc.setAcceptAllFileFilterUsed(false);
                fc.addChoosableFileFilter(new FileNameExtensionFilter(null,"bin"));
                int returnVal = fc.showOpenDialog(mainframe);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();

                        String s = file.getPath();
                        SemanticVectorsVisualization.TERM_VECTOR_FILE = s;
                        if (s.contains("WSMs/")) {
                            String t = s.substring(s.indexOf("WSMs/") + 5);
                            vectors_label.setText("<html><body>Loaded WSM: <br>"
                                    + t + "</body></html>");
                        } else {
                            vectors_label.setText("<html><body>Loaded WSM: <br>..."
                                    + s.substring(Math.max(0, s.length() - 25)) + "</body></html>");
                        }
                }

            }
        });

        load_tm.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File("./TMs"));
                fc.setAcceptAllFileFilterUsed(false);
                fc.addChoosableFileFilter(new FileNameExtensionFilter(null,"txt"));
                int returnVal = fc.showOpenDialog(mainframe);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    String s = file.getPath();
                    SemanticVectorsVisualization.TEXT_MINING_FILE = s;
                    if(s.contains("TMs/")){
                        String t = s.substring(s.indexOf("TMs/")+4);
                        loaded_tm.setText("<html><body>Loaded TM results: <br>"+
                            t+"</body></html>");
                    } else{
                        loaded_tm.setText("<html><body>Loaded TM results: <br>..."+
                            s.substring(Math.max(0, s.length()-25))+"</body></html>");
                    }
                }
                
            }
        });
        
        create_clusters_button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFrame please_wait = new JFrame("New visualization is being created. This might take several minutes. Please wait.");
                please_wait.setLocation(500, 400);
                please_wait.setSize(600, 0);
                please_wait.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                please_wait.setVisible(true);
                try{

                    int amount_clusters = Integer.parseInt(param_tf1.getText());
                    int entities_per_cluster = Integer.parseInt(param_tf4.getText());
                    int minimum_evidences = Integer.parseInt(param_tf5.getText());
                    String file_name = "Visualizations/"+clusters_name.getText()+".vis";
                    String tm_file = SemanticVectorsVisualization.TEXT_MINING_FILE;
                    String filter_text = type_filter_field.getText().trim();
                    Set<String> filter = null;
                    if(filter_text.length()>0){
                        String[] types = filter_text.split(",");
                        filter = new HashSet<String>();
                        for(String t: types){
                            filter.add(t.trim());
                        }
                    }
                    
                    InteractionGraph int_graph = InteractionGraph.getGraphFromNicerEncoding(IOHandler.parseFile(tm_file));
                    svv.setInteractionGraph(int_graph);
                    ClusterTree.NUM_CLUSTERS = amount_clusters;
                    ClusterTree.MIN_EVIDENCES = minimum_evidences;
                    ClusterTree.MAX_CLUSTER_SIZE = entities_per_cluster;
                    svv.setTypeFilter(filter);
                    ClusterTree tmp = svv.clusterNodesSemantically(new Term[0]);
                    String encoding = tmp.getEncoding();
 
                    IOHandler.writeToFile(file_name, new String[]{encoding});
                    svv.setOveral(tmp);
                    newVisualization(tmp);
                    
                    please_wait.dispose();
                } catch (NumberFormatException ex){
                    please_wait.dispose();
                    JOptionPane.showMessageDialog(mainframe,"Error. Parameters need to be integers.","Error",JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex){
                    please_wait.dispose();
                    JOptionPane.showMessageDialog(mainframe,"Error building visualizations."
                            + " Make sure the selected text mining and word space model files are "
                            + "in the correct formats.","Error",JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
        
        load_vis.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(new File("./Visualizations"));
                fc.setAcceptAllFileFilterUsed(false);
                fc.addChoosableFileFilter(new FileNameExtensionFilter(null,"vis"));
                int returnVal = fc.showOpenDialog(mainframe);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    JFrame please_wait = new JFrame("Loading visualization. Please wait.");
                    please_wait.setLocation(500, 400);
                    please_wait.setSize(600, 0);
                    please_wait.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    please_wait.setVisible(true);
                    try{
                        File file = fc.getSelectedFile();

                        String s = file.getPath();
                        Vector<String> vis_file = IOHandler.parseFile(s);
                        ClusterTree ov = ClusterTree.decode(vis_file.get(0));
                        svv.setInteractionGraph(ov.getIGraph());
                        svv.setTypeFilter(ov.getTypeFilter());
                        svv.setOveral(ov);
                        if(s.contains("Visualizations/")){
                            String t = s.substring(s.indexOf("Visualizations/")+15);
                            loaded_vis_label.setText("<html><body>Loaded visualization:<br>"+t+"</body></html>");
                        } else{
                            loaded_vis_label.setText("<html><body>Loaded visualization: <br>..."+
                                s.substring(Math.max(0, s.length()-25))+"</body></html>");
                        }
                        newVisualization(ov);
                        String tmf = SemanticVectorsVisualization.TEXT_MINING_FILE;
                        if(tmf.contains("TMs/")){
                            String t = tmf.substring(tmf.indexOf("TMs/")+4);
                            loaded_tm.setText("<html><body>Loaded TM results: <br>"+
                                t+"</body></html>");
                        } else{
                            loaded_tm.setText("<html><body>Loaded TM results: <br>..."+
                                tmf.substring(Math.max(0, tmf.length()-25))+"</body></html>");
                        }
                        please_wait.dispose();
                    } catch (Exception ex){
                        please_wait.dispose();
                        JOptionPane.showMessageDialog(mainframe,"Error loading visualizations.","Error",JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                }

                }

            }
        });
        
        // Usage panel
        JTabbedPane help_tabs = new JTabbedPane();
        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");
        jep.setText("<p><b>Search:</b></p>"
                + "<p>When loading a new visualization first<br>"
                + "the clustered terms occurring in the text<br>"
                + "mining results are shown. By entering a<br>"
                + "a search term at the top of the tool the<br>"
                + "semantic visualization centered around<br>"
                + "this term is shown.</p>"
                + "<p>Multiple terms can be entered into the<br>"
                + "search field by separating them with <br>"
                + "commas.</p>"
                + "<p>In order to go back to the original cluster,<br>"
                + "click the 'Visualize'-button without <br>"
                + "entering any search terms.</p>"
                + "<p><b>Core functionalities:</b></p>"
                + "<p><b>Left click cluster:</b></br> Explore cluster</p>"
                + "<p><b>Left click term:</b></br> Show evidences</p>"
                + "<p><b>Left click edge:</b></br> Show evidences</p>"
                + "<p><b>Right click:</b></br> Move up level</p>"
                + "<p><b>Drag background:</b></br> Move graph</p>"
                + "<p><b>Drag node:</b></br> Move node</p>"
                + "<p><b>Mouse wheel:</b></br> Zoom in/out</p>"
                );
        jep.setEditable(false);
        
        JEditorPane jep2 = new JEditorPane();
        jep2.setContentType("text/html");
        jep2.setSize(5, 5);
        jep2.setText(
                "<b>Create visualization:</b>"
                + "<p> In order to create a new visualization you <br>need a word space model and text mining <br>results."
                + "Both can be created using the <br>functionality on the left. </p>"
                + "<p><b>Load a word space model (WSM):</b></p>"
                + "<p>Either enter a name and method (latent<br>"
                + " semantic indexing, LSI, or random <br>"
                + " indexing, RI) and create a new one from<br> "
                + " a corpus <br>"
                + "('Create WSM from corpus'-button)</p>"
                + "<p>Or load an existing one <br>('Load WSM'-button)</p>"
                + "<p>Note: A default WSM is already loaded<br> on start-up."
                + "<p><b>Load text mining results:</b></p>"
                + "<p>Use 'Load text mining results'-button</p>"
                + "<p>Note: Default text mining results <br>containing relations from COPD and the<br> proteasome are already loaded on start-up.</p>"
                + "<p><b>Create a new visualization:</b></p>"
                + "<p>Make sure text mining results and a WSM<br> are loaded. Choose your parameters (type<br>"
                + " filters comma-separated) and use the<br> 'Create visualization'-button.</p>"
                + "<p>Note: Creating a new visualization usually<br>takes several minutes.</p>"
                + "<p><b>Load existing visualization:</b></p>"
                + "<p>Use 'Load visualization'-button</p>"
                + "<p>Note: A default visualization based on the <br>default WSM, TM and parameters is<br> already loaded at start-up.</p>"
                );
        jep2.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "<br/>\n");
        jep2.setEditable(false);
        help_tabs.add("Data input",jep2);
        help_tabs.add("Use",jep);
        JPanel help_panel = new JPanel();
        help_panel.setLayout(new BorderLayout());
        help_panel.add(new JLabel("How to:"), BorderLayout.NORTH);
        help_panel.add(help_tabs,BorderLayout.CENTER);
        
        graph_panel = new JPanel();
        graph_panel.add(display);
        JPanel search_panel = new JPanel();
        search_field = new JTextField("Proteasome");
        search_field.setColumns(20);
        search_field.setFocusTraversalKeysEnabled(false);
        AutoComplete autoComplete = new AutoComplete(search_field, all_searchable_terms);
        search_field.getDocument().addDocumentListener(autoComplete);
        search_field.getInputMap().put(KeyStroke.getKeyStroke("TAB"), COMMIT_ACTION);
        search_field.getActionMap().put(COMMIT_ACTION, new CommitAction(autoComplete));
        
        JButton search_button = new JButton("Visualize");
        search_button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String term = search_field.getText();
                newSearch(term);
            }
        });
        search_panel.add(search_field);
        search_panel.add(search_button);
        main_panel.add(search_panel, BorderLayout.NORTH);
        main_panel.add(functions_panel, BorderLayout.WEST);
        main_panel.add(graph_panel, BorderLayout.CENTER);
        main_panel.add(help_panel, BorderLayout.EAST);
        
        mainframe.getContentPane().add(main_panel);
        mainframe.setVisible(true); // show the window
    }

    public MyDisplay createDisplay(Graph graph) {
        final String labelField = nameString;
        final String fillField = sizeString;
        final String sizeField = sizeString;
        final String evField = evidenceString;

        // add the graph to the visualization as the data group "graph"
        // nodes and edges are accessible as "graph.nodes" and "graph.edges"
        final Visualization vis = new Visualization();
        vis.add("graph", graph==null?new Graph():graph);


        if(graph!=null){
        // draw the "name" label for NodeItems
        AbstractShapeRenderer r = new AbstractShapeRenderer() {

            protected Ellipse2D m_box = new Ellipse2D.Double();

            @Override
            protected Shape getRawShape(VisualItem item) {
                m_box.setFrame(item.getX(), item.getY(), Math.min(200, 20 + 2 * (Integer) item.get(sizeField)), Math.min(200, 20 + 2 * (Integer) item.get(sizeField)));
                return m_box;
            }
        };

        EdgeRenderer eRend = new EdgeRenderer(Constants.EDGE_TYPE_CURVE) {

            @Override
            protected BasicStroke getStroke(VisualItem item) {
                Edge e = (Edge) item;
                int evidences = (Integer) e.get(evidenceString);
                return new BasicStroke(Math.min(evidences / 2, 8));
            }
        };

        // create a new default renderer factory
        // return our name label renderer as the default for all non-EdgeItems
        // includes straight line edges for EdgeItems by default

        DefaultRendererFactory drf = new DefaultRendererFactory(r);
        drf.setDefaultEdgeRenderer(eRend);
        vis.setRendererFactory(drf);

        drf.add(new InGroupPredicate("nodedec"), new LabelRenderer(labelField));
        final Schema DECORATOR_SCHEMA = PrefuseLib.getVisualItemSchema();
        DECORATOR_SCHEMA.setDefault(VisualItem.INTERACTIVE, false);
        DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.rgb(100, 100, 100));
        DECORATOR_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma", 16));

        // Add the decorators to the visualization.  We tell it the label for the group, the items to label,
        // and a schema that describes the behavior of the decorators. 
        vis.addDecorators("nodedec", "graph.nodes", DECORATOR_SCHEMA);

        // create our nominal color palette
        // pink for females, baby blue for males
        int[] palette = new int[]{
            //            ColorLib.rgb(255, 180, 180), ColorLib.rgb(190, 190, 255)
            ColorLib.rgb(210, 210, 255), ColorLib.rgb(190, 240, 190)
        };
        int[] palette2 = new int[]{ColorLib.rgb(200, 220, 180), ColorLib.rgb(200, 180, 200), ColorLib.rgb(250, 210, 160), ColorLib.rgb(150, 200, 150)};
        // map nominal data values to colors using our provided palette
        DataColorAction fill = new DataColorAction("graph.nodes", fillField,
                Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
        // use black for node text
        ColorAction text = new ColorAction("graph.nodes",
                VisualItem.TEXTCOLOR, ColorLib.gray(0));

        Predicate p0 = ExpressionParser.predicate("ISEDGE() AND " + evField + " <= 2");
        Predicate p1 = ExpressionParser.predicate("ISEDGE() AND " + evField + " == 3");
        Predicate p2 = ExpressionParser.predicate("ISEDGE() AND " + evField + " == 4");
        Predicate p3 = ExpressionParser.predicate("ISEDGE() AND " + evField + " > 4");
        ColorAction edgeColor0 = new ColorAction("graph.edges", p0, VisualItem.STROKECOLOR, palette2[0]);
        ColorAction edgeColor1 = new ColorAction("graph.edges", p1, VisualItem.STROKECOLOR, palette2[1]);
        ColorAction edgeColor2 = new ColorAction("graph.edges", p2, VisualItem.STROKECOLOR, palette2[2]);
        ColorAction edgeColor3 = new ColorAction("graph.edges", p3, VisualItem.STROKECOLOR, palette2[3]);

        // create an action list containing all color assignments
        ActionList color = new ActionList();
        color.add(fill);
        color.add(text);

        color.add(edgeColor0);
        color.add(edgeColor1);
        color.add(edgeColor2);
        color.add(edgeColor3);

        // create an action list with an animated layout
        ActionList layout = new ActionList();
        layout.add(new FruchtermanReingoldLayout("graph", 5));
        ActionList layout2 = new ActionList(Activity.INFINITY);
        layout2.add(new MyLayout("nodedec"));

        // add the actions to the visualization
        vis.putAction("color", color);
        vis.putAction("layout", layout);
        vis.putAction("layout2", layout2);

        ActionList repaint = new ActionList();
        repaint.add(new RepaintAction());
        vis.putAction("repaint", repaint);


        // create a new Display that pull from our Visualization
        MyDisplay dis = new MyDisplay(vis);
        dis.setSize(1000, 1000); // set display size
        dis.addControlListener(new DragControl()); // drag items around
        dis.addControlListener(new PanControl());  // pan with background left-drag
        dis.addControlListener(new ZoomControl()); // zoom with vertical right-drag
        dis.addControlListener(new ControlAdapter() {

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    levelUp();
                }
            }

            @Override
            public void itemClicked(VisualItem item, MouseEvent event) {
                // NODE CLICKED:
                if (item instanceof Node) {
                    int clusterno = (Integer) item.get(clusterString);
                    if (clusterno != -2) {
                        // switch to different view
                        changeDisplay(clusterno);
                    } else {
                        // display evidences
                        Term t = (Term) item.get(termstring);
                        List<Relation> edges = new LinkedList<Relation>();
                        if(centers.length>0){
                            for(Term c: centers){
                                if(mainGraph.findEdgeSet(t,c)!= null){
                                    edges.addAll(mainGraph.findEdgeSet(t, c));
                                }
                                if(mainGraph.findEdgeSet(c, t)!= null){
                                    edges.addAll(mainGraph.findEdgeSet(c, t));
                                }                            
                            }
                        } else{
                            edges.addAll(mainGraph.getInEdges(t));
                            edges.addAll(mainGraph.getOutEdges(t));
                        }
                        JFrame evView = new JFrame("Evidences " + t);
                        evView.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        JEditorPane textArea = new JEditorPane();
                        textArea.setEditorKit(new HTMLEditorKit());
                        // add link handling to editor
                        textArea.addHyperlinkListener(new HyperlinkListener() {
                            public void hyperlinkUpdate(HyperlinkEvent e) {
                                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                    if (Desktop.isDesktopSupported()) {
                                        try {

                                            Desktop.getDesktop().browse(e.getURL().toURI());

                                        } catch (URISyntaxException ex) {
                                            Logger.getLogger(PrefuseVisualization.class.getName()).log(Level.SEVERE, null, ex);
                                        } catch (IOException ex) {
                                            Logger.getLogger(PrefuseVisualization.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }

                                }
                            }
                        });


                        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                        evView.setSize(new Dimension(800, 400));
                        evView.setLocation(500, 400);
                        textArea.setEditable(false);
                        String mytext = "";

                        for (Relation rel : edges) {
                            for (int i=0; i<rel.getEvidences().size(); i++) {
                                String ev = rel.getEvidences().get(i);
                                String agent = rel.getAgent().getName().toLowerCase();
                                String theme = rel.getTheme().getName().toLowerCase();
                                int indexOf = ev.toLowerCase().indexOf(agent);
                                String myString = indexOf >= 0 ? ev.substring(0, indexOf) + "<b>"
                                        + ev.substring(indexOf, indexOf + agent.length()) + "</b>" + ev.substring(indexOf + agent.length()) : ev;
                                indexOf = myString.toLowerCase().indexOf(theme);
                                myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                        + myString.substring(indexOf, indexOf + theme.length()) + "</b>" + myString.substring(indexOf + theme.length()) : myString;
                                List<String> aliAgent = rel.getAgent().getAliases();
                                if(aliAgent!=null){
                                    for(String a: aliAgent){
                                        indexOf = myString.toLowerCase().indexOf(a.toLowerCase());
                                        myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                            + myString.substring(indexOf, indexOf + a.length()) + "</b>" + myString.substring(indexOf + a.length()) : myString;
                                    }
                                }
                                List<String> aliTheme = rel.getTheme().getAliases();
                                if(aliTheme!=null){
                                    for(String a: aliTheme){
                                        indexOf = myString.toLowerCase().indexOf(a.toLowerCase());
                                        myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                            + myString.substring(indexOf, indexOf + a.length()) + "</b>" + myString.substring(indexOf + a.length()) : myString;
                                    }
                                }
                                mytext += "<p>" + myString +"<br><a href=" +rel.getPubmedids().get(i)+">"+rel.getPubmedids().get(i)+ "</a> </p>";
                            }
                        }
                        textArea.setText(mytext);

                        evView.getContentPane().add(scrollPane);
                        evView.setVisible(true);
                    }
                // EDGE CLICKED:
                } else if (item instanceof Edge) {
                    Edge e = (Edge) item;
                    
                    List<Relation> edges = new LinkedList<Relation>();
                    Term t2 = (Term) e.get(secondtermstring);
                    List<Term> p = new LinkedList<Term>();
                    
                    if (e.canGet(termstring, Term.class)) {
                        p.add( (Term) e.get(termstring) );
                    } else{
                        String ts = (String) e.get(termstring);
                        String[] allterms = ts.split("\\n");
                        for(String a: allterms){
                            p.add( mainGraph.getTerm(a) );
                        }
                    }
                        
                    for(Term t: p){
                            if(mainGraph.findEdgeSet(t,t2)!= null){
                                edges.addAll(mainGraph.findEdgeSet(t, t2));
                            }
                            if(mainGraph.findEdgeSet(t2, t)!= null){
                                edges.addAll(mainGraph.findEdgeSet(t2, t));
                            }
                    }
                        
                        JFrame evView = new JFrame("Evidences:");
                        evView.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        JEditorPane textArea = new JEditorPane();
                        textArea.setEditorKit(new HTMLEditorKit());
                        // add link handling to editor
                        textArea.addHyperlinkListener(new HyperlinkListener() {
                            public void hyperlinkUpdate(HyperlinkEvent e) {
                                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                    if (Desktop.isDesktopSupported()) {
                                        try {

                                            Desktop.getDesktop().browse(e.getURL().toURI());

                                        } catch (URISyntaxException ex) {
                                            Logger.getLogger(PrefuseVisualization.class.getName()).log(Level.SEVERE, null, ex);
                                        } catch (IOException ex) {
                                            Logger.getLogger(PrefuseVisualization.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }

                                }
                            }
                        });

                        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                        evView.setSize(new Dimension(800, 400));
                        evView.setLocation(500, 400);
                        textArea.setEditable(false);
                        String mytext = "";

                        for (Relation rel : edges) {
                            for (int i=0; i< rel.getEvidences().size(); i++) {
                                String ev = rel.getEvidences().get(i);
                                String agent = rel.getAgent().getName().toLowerCase();
                                String theme = rel.getTheme().getName().toLowerCase();
                                int indexOf = ev.toLowerCase().indexOf(agent);
                                String myString = indexOf >= 0 ? ev.substring(0, indexOf) + "<b>"
                                        + ev.substring(indexOf, indexOf + agent.length()) + "</b>" + ev.substring(indexOf + agent.length()) : ev;
                                indexOf = myString.toLowerCase().indexOf(theme);
                                myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                        + myString.substring(indexOf, indexOf + theme.length()) + "</b>" + myString.substring(indexOf + theme.length()) : myString;
                                List<String> aliAgent = rel.getAgent().getAliases();
                                if(aliAgent!=null){
                                    for(String a: aliAgent){
                                        indexOf = myString.toLowerCase().indexOf(a.toLowerCase());
                                        myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                            + myString.substring(indexOf, indexOf + a.length()) + "</b>" + myString.substring(indexOf + a.length()) : myString;
                                    }
                                }
                                List<String> aliTheme = rel.getTheme().getAliases();
                                if(aliTheme!=null){
                                    for(String a: aliTheme){
                                        indexOf = myString.toLowerCase().indexOf(a.toLowerCase());
                                        myString = indexOf >= 0 ? myString.substring(0, indexOf) + "<b>"
                                            + myString.substring(indexOf, indexOf + a.length()) + "</b>" + myString.substring(indexOf + a.length()) : myString;
                                    }
                                }
                                mytext += "<p>" + myString +"<br><a href=" +rel.getPubmedids().get(i)+">"+rel.getPubmedids().get(i)+ "</a> </p>";
                            }
                        }
                        textArea.setText(mytext);

                        evView.getContentPane().add(scrollPane);
                        evView.setVisible(true);
                    }
                }
            
        });

        vis.run("color");  // assign the colors
        vis.run("layout"); // start up the animated layout
        vis.run("layout2");
        vis.run("repaint");
        dis.setHighQuality(true);

        return dis;
    }else {
        MyDisplay dis = new MyDisplay(vis);
        dis.setSize(1000, 1000); // set display size
        return dis;
    }
    } 
}
