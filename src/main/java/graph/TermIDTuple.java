package graph;

/**
 *
 * @author Philipp Blohm
 */
public class TermIDTuple {
    Term term;
    String id;
    
    public TermIDTuple(String id, Term term){
        this.term = term;
        this.id = id;
    }
    
    public Term getTerm(){
        return term;
    }
    
}
