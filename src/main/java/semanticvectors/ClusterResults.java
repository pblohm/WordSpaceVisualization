package semanticvectors;

/**
 *
 * @author philipp
 */

import graph.InteractionGraph;
import graph.Relation;
import graph.Term;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;
import pitt.search.semanticvectors.CompoundVectorBuilder;
import pitt.search.semanticvectors.Flags;
import pitt.search.semanticvectors.LuceneUtils;
import pitt.search.semanticvectors.ObjectVector;
import pitt.search.semanticvectors.VectorStoreReaderLucene;
import pitt.search.semanticvectors.vectors.Vector;
import pitt.search.semanticvectors.vectors.VectorFactory;
import pitt.search.semanticvectors.vectors.VectorUtils;

/**
 * Clusters search results using a simple k-means algorithm.
 * 
 */
public class ClusterResults {
  private static final Logger logger = Logger.getLogger(ClusterResults.class.getCanonicalName());
    
  /** 
   * Simple k-means clustering algorithm.
   * 
   * @param objectVectors Array of object vectors to be clustered.
   * @return Integer array parallel to objectVectors saying which
   * cluster each vector belongs to.
   */
  public static int[] kMeansCluster (ObjectVector[] objectVectors, int numClusters) {
    int[] clusterMappings = new int[objectVectors.length];
    Random rand = new Random();
    Vector[] centroids = new Vector[numClusters];

    logger.info("Initializing clusters ...");

    // Initialize cluster mappings randomly.
    for (int i = 0; i < objectVectors.length; ++i) {
      int randInt = rand.nextInt();
      while (randInt == Integer.MIN_VALUE) {
        //fix strange result where abs(MIN_VALUE) returns a negative number
        randInt = rand.nextInt();
      }
      clusterMappings[i] = Math.abs(randInt) % numClusters;
    }

    logger.info("Iterating k-means assignment ...");

    // Loop that computes centroids and reassigns members.
    while (true) {
      // Clear centroid register.
      for (int i = 0; i < centroids.length; ++i) {
        centroids[i] = VectorFactory.createZeroVector(Flags.vectortype, Flags.dimension); 
      }
      // Generate new cluster centroids.
      for (int i = 0; i < objectVectors.length; ++i) {
        centroids[clusterMappings[i]].superpose(objectVectors[i].getVector(), 1, null);
      }
      for (int i = 0; i < numClusters; ++i) {
        centroids[i].normalize();
      }

      boolean changeFlag = false;
      // Map items to clusters.
      for (int i = 0; i < objectVectors.length; i++) {
        int j = VectorUtils.getNearestVector(objectVectors[i].getVector(), centroids);
        if (j != clusterMappings[i]) {
          changeFlag = true;
          clusterMappings[i] = j;
        }
      }
      if (changeFlag == false) {
        break;
      }
    }

    logger.info("Got to stable clusters ...");
    return clusterMappings;
  }
  
    public static Tuple<int[], List<ObjectVector>> cluster(InteractionGraph g, List<Term> terms, int numclusters, int numEvidencesFilter, List<Term> centers) {
        String[] args = ("-numclusters " + (numclusters) + " -numsearchresults 0 -queryvectorfile "+ SemanticVectorsVisualization.TERM_VECTOR_FILE+".bin").split(" ");
        args = Flags.parseCommandLineFlags(args);

        VectorStoreReaderLucene vecReader = null;
        LuceneUtils luceneUtils = null;
        Pattern integerPattern = Pattern.compile("^\\d*$");

        try {
            vecReader = new VectorStoreReaderLucene(Flags.queryvectorfile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to open vector store from file: {0}", Flags.queryvectorfile);
            System.err.println("IOException initializing VectorStoreReaderLucene");
        }

        if (Flags.luceneindexpath != null) {
            try {
                luceneUtils = new LuceneUtils(Flags.luceneindexpath);
            } catch (IOException e) {
                logger.log(Level.INFO, "Couldn''t open Lucene index at {0}", Flags.luceneindexpath);
            }
        }
        
        Vector compare = CompoundVectorBuilder.getQueryVectorFromString(
                    vecReader, luceneUtils, "proteasome");
        

        List<ObjectVector> myvecs = new LinkedList<ObjectVector>();
        List<ObjectVector> notfounds = new LinkedList<ObjectVector>();
        Set<String> types = new HashSet<String>();
        for (int i = 0; i < terms.size(); i++) {
            boolean has_edge = false;
            Collection<Relation> outedges = g.getOutEdges(terms.get(i));
            Collection<Relation> inedges = g.getInEdges(terms.get(i));
            int numEv = 0;
            for(Relation r: outedges){
                numEv+= r.getEvidences().size();
                if(!has_edge && centers.contains(r.getTheme())){
                    has_edge = true;
                }
            }
            for(Relation r: inedges){
                numEv+= r.getEvidences().size();
                if(!has_edge && centers.contains(r.getAgent())){
                    has_edge = true;
                }
            }
            
            if ( (has_edge || centers.isEmpty()) && numEv >= numEvidencesFilter) {
                types.addAll(terms.get(i).getTypes().keySet());
                String term = terms.get(i).getName();
                Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);

                TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(term));
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
                List<String> words = new LinkedList<String>();
                try {
                    while (tokenStream.incrementToken()) {
                        int startOffset = offsetAttribute.startOffset();
                        int endOffset = offsetAttribute.endOffset();
                        words.add(charTermAttribute.toString());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ClusterResults.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (words.size() > 0) {
                    Vector vec1 = CompoundVectorBuilder.getQueryVectorFromString(
                            vecReader, luceneUtils, words.get(0));
                    for (int j = 1; j < words.size(); j++) {
                        Matcher matchesInteger = integerPattern.matcher(words.get(j));
                        if(!matchesInteger.matches() && !words.get(j).toLowerCase().equals("of")){
                            vec1.superpose(CompoundVectorBuilder.getQueryVectorFromString(
                                vecReader, luceneUtils, words.get(j)), 1, null);
                        }
                    }
                    vec1.normalize();
                    
                    if (!Double.isNaN(compare.measureOverlap(vec1))) {
                        myvecs.add(new ObjectVector(terms.get(i), vec1));
                    } else{
                        notfounds.add(new ObjectVector(terms.get(i), vec1));
                    }
                }
            }
        }

        int[] clusterMappings = kMeansCluster(myvecs.toArray(new ObjectVector[0]), Flags.numclusters);

        // add not found object vectors
        myvecs.addAll(notfounds);
        int[] combinedMappings = new int[clusterMappings.length+notfounds.size()];
        System.arraycopy(clusterMappings, 0, combinedMappings, 0, clusterMappings.length);
        for(int i=clusterMappings.length; i<combinedMappings.length; i++){
            combinedMappings[i] = numclusters;
        }
        
        return new Tuple<int[], List<ObjectVector>>(combinedMappings, myvecs);
    }
    
    public static class Tuple<X, Y> {

        public final X x;
        public final Y y;

        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

  /**
   * Main function gathers search results for a particular query,
   * performs clustering on the results, and prints out results.
   * @param args
   * @see ClusterResults#usage
   */
//  public static void main (String[] args) throws IllegalArgumentException {
//    args = "-numclusters 3 -numsearchresults 10".split(" ");
//    args = Flags.parseCommandLineFlags(args);
//
//    // Get search results, perform clustering, and print out results.           
//    ObjectVector[] resultsVectors = Search.getSearchResultVectors(new String[]{"proteasome"}, Flags.numsearchresults);
//    
//    int[] clusterMappings = kMeansCluster(resultsVectors, Flags.numclusters);
//    for (int i = 0; i < Flags.numclusters; ++i) {
//      System.out.println("Cluster " + i);
//      for (int j = 0; j < clusterMappings.length; ++j) {
//        if (clusterMappings[j] == i) {
//          System.out.print(resultsVectors[j].getObject() + " ");
//        }
//      }
//      System.out.println();
//    }
//  }
}

