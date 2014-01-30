package semanticvectors;

import java.io.IOException;
import java.util.Arrays;
import pitt.search.semanticvectors.DocVectors;
import pitt.search.semanticvectors.Flags;
import pitt.search.semanticvectors.IncrementalDocVectors;
import pitt.search.semanticvectors.IncrementalTermVectors;
import pitt.search.semanticvectors.TermVectorsFromLucene;
import pitt.search.semanticvectors.VectorStore;
import pitt.search.semanticvectors.VectorStoreWriter;
import pitt.search.semanticvectors.VerbatimLogger;
import pitt.search.semanticvectors.vectors.VectorType;

public class MyBuildIndex {
        
    
    public static void createVectors(String[] args){
        try {
      args = Flags.parseCommandLineFlags(args);
    } catch (IllegalArgumentException e) {
      throw e;
    }

    // Only one argument should remain, the path to the Lucene index.
    if (args.length != 1) {
      throw (new IllegalArgumentException("After parsing command line flags, there were " + args.length
                                          + " arguments, instead of the expected 1."));
    }

    String luceneIndex = args[0];
    VerbatimLogger.info("Seedlength: " + Flags.seedlength
        + ", Dimension: " + Flags.dimension
        + ", Vector type: " + Flags.vectortype
        + ", Minimum frequency: " + Flags.minfrequency
        + ", Maximum frequency: " + Flags.maxfrequency
        + ", Number non-alphabet characters: " + Flags.maxnonalphabetchars
        + ", Contents fields are: " + Arrays.toString(Flags.contentsfields) + "\n");

    String termFile = SemanticVectorsVisualization.TERM_VECTOR_FILE+".bin";//"termvectors.bin";
    String docFile = "docvectors.bin";

    try{
      TermVectorsFromLucene vecStore;
      if (Flags.initialtermvectors.length() > 0) {
        // If Flags.initialtermvectors="random" create elemental (random index)
        // term vectors. Recommended to iterate at least once (i.e. -trainingcycles = 2) to
        // obtain semantic term vectors.
        // Otherwise attempt to load pre-existing semantic term vectors.
        VerbatimLogger.info("Creating term vectors ... \n");
        vecStore = TermVectorsFromLucene.createTermBasedRRIVectors(
            luceneIndex, VectorType.valueOf(Flags.vectortype.toUpperCase()), Flags.dimension,
            Flags.seedlength, Flags.minfrequency, Flags.maxfrequency,
            Flags.maxnonalphabetchars, Flags.initialtermvectors, Flags.contentsfields);
      } else {
        VerbatimLogger.info("Creating elemental document vectors ... \n");
        vecStore = TermVectorsFromLucene.createTermVectorsFromLucene(
            luceneIndex, VectorType.valueOf(Flags.vectortype.toUpperCase()),
            Flags.dimension, Flags.seedlength, Flags.minfrequency, Flags.maxfrequency,
            Flags.maxnonalphabetchars, null, Flags.contentsfields);
      }

      // Create doc vectors and write vectors to disk.
      VectorStoreWriter vecWriter = new VectorStoreWriter();
      if (Flags.docindexing.equals("incremental")) {
        vecWriter.writeVectors(termFile, vecStore);
        IncrementalDocVectors.createIncrementalDocVectors(
            vecStore, luceneIndex, Flags.contentsfields, "incremental_"+docFile);
        IncrementalTermVectors itermVectors = null;

        for (int i = 1; i < Flags.trainingcycles; ++i) {
          itermVectors = new IncrementalTermVectors(
              luceneIndex, VectorType.valueOf(Flags.vectortype.toUpperCase()),
              Flags.dimension, Flags.contentsfields, "incremental_"+docFile);

          new VectorStoreWriter().writeVectors(
              "incremental_termvectors"+Flags.trainingcycles+".bin", itermVectors);

        // Write over previous cycle's docvectors until final
        // iteration, then rename according to number cycles
        if (i == Flags.trainingcycles-1) docFile = "docvectors"+Flags.trainingcycles+".bin";

        IncrementalDocVectors.createIncrementalDocVectors(
            itermVectors, luceneIndex, Flags.contentsfields,
            "incremental_"+docFile);
        }
      } else if (Flags.docindexing.equals("inmemory")) {
        DocVectors docVectors = new DocVectors(vecStore);
        for (int i = 1; i < Flags.trainingcycles; ++i) {
          VerbatimLogger.info("\nRetraining with learned document vectors ...");
          vecStore = TermVectorsFromLucene.createTermVectorsFromLucene(
              luceneIndex, VectorType.valueOf(Flags.vectortype.toUpperCase()),
              Flags.dimension, Flags.seedlength,
              Flags.minfrequency, Flags.maxfrequency, Flags.maxnonalphabetchars,
              docVectors, Flags.contentsfields);
          docVectors = new DocVectors(vecStore);
        }
        // At end of training, convert document vectors from ID keys to pathname keys.
        VectorStore writeableDocVectors = docVectors.makeWriteableVectorStore();

        if (Flags.trainingcycles > 1) {
          termFile = SemanticVectorsVisualization.TERM_VECTOR_FILE + Flags.trainingcycles + ".bin";
          docFile = "docvectors" + Flags.trainingcycles + ".bin";
        }
        VerbatimLogger.info("Writing term vectors to " + termFile + "\n");
        vecWriter.writeVectors(termFile, vecStore);
        VerbatimLogger.info("Writing doc vectors to " + docFile + "\n");
        //vecWriter.writeVectors(docFile, writeableDocVectors); // took out write doc vectors
      } else {
        // Write term vectors to disk even if there are no docvectors to output.
        VerbatimLogger.info("Writing term vectors to " + termFile + "\n");
        vecWriter.writeVectors(termFile, vecStore);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    }
    
     /**
   * Builds term vector and document vector stores from a Lucene index.
   * @param args [command line options to be parsed] then path to Lucene index
   */
//  public static void main (String[] args) throws IllegalArgumentException {
//      
//      String[] myargs = new String[5];
//      myargs[0] = "-minfrequency";
//      myargs[1] = "-1";
//      myargs[2] = "-maxnonalphabetchars";
//      myargs[3] = "-1";
//      myargs[4] = MyIndexFiles.INDEX;
//      createVectors(myargs);
//  }

    
}
