package semanticvectors;

import ch.akuhn.edu.mit.tedlab.*;
import java.io.File;
import java.util.logging.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexOutput;
import pitt.search.semanticvectors.Flags;
import pitt.search.semanticvectors.vectors.RealVector;
import pitt.search.semanticvectors.vectors.Vector;
import pitt.search.semanticvectors.vectors.VectorFactory;

public class MyLSA {

  static boolean le = false;
  static String[] theTerms;

  /* Converts a dense matrix to a sparse one (without affecting the dense one) */
  static SMat smatFromIndex(String fileName) throws Exception {
    SMat S;

    //initiate IndexReader and LuceneUtils
    File file = new File(fileName);
    IndexReader indexReader = IndexReader.open(FSDirectory.open(file));
    MyLuceneUtils.compressIndex(fileName);
    MyLuceneUtils lUtils = new MyLuceneUtils(fileName);

    //calculate norm of each doc vector so as to normalize these before SVD
    int[][] index;
    String[] desiredFields = Flags.contentsfields;

    TermEnum terms = indexReader.terms();
    int tc = 0;
    while(terms.next()){
      if (lUtils.termFilter(terms.term(), desiredFields,
          Flags.minfrequency, Flags.maxfrequency, Flags.maxnonalphabetchars))
        tc++;
    }

    theTerms = new String[tc];
    index = new int[tc][];

    terms = indexReader.terms();
    tc = 0;
    int nonzerovals = 0;

    while(terms.next()){
      org.apache.lucene.index.Term term = terms.term();
      if (lUtils.termFilter(term, desiredFields,
          Flags.minfrequency, Flags.maxfrequency, Flags.maxnonalphabetchars)) {
        theTerms[tc] = term.text();

        // Create matrix of nonzero indices.
        TermDocs td = indexReader.termDocs(term);
        int count =0;
        while (td.next()) {
          count++;
          nonzerovals++;
        }
        index[tc] = new int[count];

        // Fill in matrix of nonzero indices.
        td = indexReader.termDocs(term);
        count = 0;
        while (td.next()) {
          index[tc][count++] = td.doc();
        }
        tc++;   // Next term.
      }
    }

    // Initialize "SVDLIBJ" sparse data structure.
    S = new SMat(indexReader.numDocs(),tc, nonzerovals);

    // Populate "SVDLIBJ" sparse data structure.
    terms = indexReader.terms();
    tc = 0;
    int nn= 0;

    while (terms.next()) {
      org.apache.lucene.index.Term term = terms.term();
      if (lUtils.termFilter(term, desiredFields,
                            Flags.minfrequency, Flags.maxfrequency,
                            Flags.maxnonalphabetchars)) {
        TermDocs td = indexReader.termDocs(term);
        S.pointr[tc] = nn;  // Index of first non-zero entry (document) of each column (term).

        while (td.next()) {
          /** public int[] pointr; For each col (plus 1), index of
            *  first non-zero entry.  we'll represent the matrix as a
            *  document x term matrix such that terms are columns
            *  (otherwise it would be difficult to extract this
            *  information from the lucene index)
            */

          float value = td.freq();

          // Use log-entropy weighting if directed.
          if (le) {
            float entropy = lUtils.getEntropy(term);
            float log1plus = (float) Math.log10(1+value);
            value = entropy*log1plus;
          }

          S.rowind[nn] = td.doc();  // set row index to document number
          S.value[nn] = value;  // set value to frequency (with/without weighting)
          nn++;
        }
        tc++;
      }
    }
    S.pointr[S.cols] = S.vals;

    return S;
  }

  public static void createVectors(String[] args) throws Exception {
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

    if (Flags.termweight.equals("logentropy")) le = true;
    else le = false;

    SMat A = smatFromIndex(args[0]);
    Svdlib svd = new Svdlib();

    SVDRec svdR = svd.svdLAS2A(A, Flags.dimension);
    DMat vT = svdR.Vt;
    DMat uT = svdR.Ut;

    // Open file and write headers.
    String termFile = SemanticVectorsVisualization.TERM_VECTOR_FILE+".bin"; //"svd_termvectors.bin";
    FSDirectory fsDirectory = FSDirectory.open(new File("."));
    IndexOutput outputStream = fsDirectory.createOutput(termFile);
    float[] tmpVector = new float[Flags.dimension];

    // Write header giving number of dimension for all vectors.
    outputStream.writeString("-dimension " + Flags.dimension + " -vectortype real");

    int cnt;
    // Write out term vectors
    for (cnt = 0; cnt < vT.cols; cnt++) {
      outputStream.writeString(theTerms[cnt]);

      Vector termVector = VectorFactory.createZeroVector(Flags.vectortype, Flags.dimension);

      float[] tmp = new float[Flags.dimension];
      for (int i = 0; i < Flags.dimension; i++)
        tmp[i] = (float) vT.value[i][cnt];
      termVector = new RealVector(tmp);
      termVector.normalize();

      termVector.writeToLuceneStream(outputStream);
    }

    outputStream.flush();
    outputStream.close();

    // Write document vectors.
    // Open file and write headers.
//    String docFile = "svd_docvectors.bin";
//    outputStream = fsDirectory.createOutput(docFile);
//    tmpVector = new float[Flags.dimension];
//
//    // Write header giving number of dimension for all vectors.
//    outputStream.writeString("-dimension");
//    outputStream.writeInt(Flags.dimension);
//
//    // initilize IndexReader and LuceneUtils
//    File file = new File(args[0]);
//    IndexReader indexReader = IndexReader.open(FSDirectory.open(file));
//
//    // Write out document vectors
//    for (cnt = 0; cnt < uT.cols; cnt++) {
//      String thePath = indexReader.document(cnt).get("path");
//      outputStream.writeString(thePath);
//      float[] tmp = new float[Flags.dimension];
//
//      for (int i = 0; i < Flags.dimension; i++)
//        tmp[i] = (float) uT.value[i][cnt];
//      RealVector docVector = new RealVector(tmp);
//
//      docVector.writeToLuceneStream(outputStream);
//    }
//
//    outputStream.flush();
//    outputStream.close();
  }
}