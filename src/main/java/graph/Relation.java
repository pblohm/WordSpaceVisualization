package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author philipp
 */
public class Relation implements Comparable<Relation> {

    String name;
    Term agent;
    Term theme;
    
    List<String> evidences;
    List<Calendar> dates;
    List<String> pubmedids;
    
    public Relation(String n){
        name = n;
    }
    
    public Relation(String n, Term a, Term t, String ev, Calendar d, String id){
        name = n;
        agent = a;
        theme = t;
        evidences = new LinkedList<String>();
        dates = new LinkedList<Calendar>();
        pubmedids = new LinkedList<String>();
        evidences.add(ev);
        dates.add(d);
        pubmedids.add(id);
    }
    
    public Relation(String n, Term a, Term t, List<String> ev, List<Calendar> c, List<String> ids){
        name = n;
        agent = a;
        theme = t;
        evidences = ev;
        dates = c;
        pubmedids = ids;
    }
    
    @Override
    public String toString(){
        return name+"("+agent+","+theme+")";
    }
    
    public void mergeRelations(Relation r){
        for(int i=0; i<r.evidences.size(); i++){
            if(!evidences.contains(r.evidences.get(i))){
                evidences.add(r.evidences.get(i));
                dates.add(r.dates.get(i));
                pubmedids.add(r.pubmedids.get(i));
            }
        }
    }
    
    public static Relation getRelationFromNicerEncoding(String enc, Map<String,Term> termmap){
        String[] parts = enc.split("\t");
        if(parts.length>5 || parts.length<3){
            System.out.println("Incorrect relation format: "+enc);
            return null;
        }
        String name = parts[0];
        Term agent = termmap.get(parts[1]);
        Term theme = termmap.get(parts[2]);
        String evline = null;
        String idline = null;
        if(parts.length == 5){
            evline = parts[3];
            idline = parts[4];
        } else if(parts.length == 4){
            if(parts[3].toLowerCase().startsWith("pubmed:") || parts[3].toLowerCase().startsWith("pmc:") ){
                idline = parts[3];
            } else{
                evline = parts[3];
            }
        }
        List<String> ev = evline == null? null:Relation.getEvFromNiceEncoding(parts[3]);
        List<String> ids = idline == null? null:Relation.getIDsFromNiceEncoding(parts[4]);
        
        return new Relation(name,agent,theme,ev,null,ids);        
    }
    
    private static List<String> getEvFromNiceEncoding(String enc){
        String[] parts = enc.split("\"\\|\"");
        List<String> res = new ArrayList<String>(parts.length);
        for(int i=0; i<parts.length; i++){
            String r = parts[i];
            if(i==0) r = r.substring(1);
            if(i==parts.length-1) r= r.substring(0,r.length()-1);
            res.add(r);
        }
        return res;
    }
    
    private static List<String> getIDsFromNiceEncoding(String enc){
        String[] parts = enc.split("\\|");
        return new ArrayList<String>(Arrays.asList(parts));

    }
    
    public static Relation getRelationFromSimpleEncoding(String enc){
        String[] parts = enc.split("@;@");
        String name = parts[0];
        if(parts.length<6) System.out.println(enc);
        Term agent = Term.getTermFromSimpleEncoding(parts[1]);
        Term theme = Term.getTermFromSimpleEncoding(parts[2]);
        List<String> ev = Relation.getStringListFromSimpleEncoding(parts[3]);
        List<Calendar> c = Relation.getCalendarListFromSimpleEncoding(parts[4]);
        List<String> ids = Relation.getStringListFromSimpleEncoding(parts[5]);
        
        return new Relation(name,agent,theme,ev,c,ids);
    }
    
    private static List<String> getStringListFromSimpleEncoding(String enc){
        if(enc.equals("null")) return null;
        String[] parts = enc.split("!@!");
        return new ArrayList<String>(Arrays.asList(parts));
    }
    
    private static List<Calendar> getCalendarListFromSimpleEncoding(String enc){
        if(enc.equals("null")) return null;
        String[] parts = enc.split("!@!");
        List<Calendar> result = new ArrayList<Calendar>(parts.length);
        for(int i=0; i<parts.length; i++){
            String[] units = parts[i].split("/");
            if(units.length!=3){
                result.add(null);
            }
            else{
                Calendar myCal = Calendar.getInstance();
                myCal.set( Integer.parseInt(units[2]), Integer.parseInt(units[1]), Integer.parseInt(units[0]) );
                result.add(myCal);
            }
        }
        return result;
    }
    
    public String getSimpleEncoding(){
        String separator = "@;@";
        String encoding = name+separator+agent.getSimpleEncoding()+separator
                +theme.getSimpleEncoding()+separator+getStringListEncoding(evidences)
                +separator+getCalendarListEncoding(dates)+separator+getStringListEncoding(pubmedids);
        return encoding;
    }
    
    public String getNicerEncoding(Map<Term,String> termmap){
        String sep = "\t";
        String encoding = InteractionGraph.esc(name)+sep+InteractionGraph.esc(termmap.get(agent))+sep+InteractionGraph.esc(termmap.get(theme))+sep
                +getNiceEvidencesEncoding()+sep+getNicePubmedEncoding();
        return encoding;
    }
    
    private String getStringListEncoding(List<String> mylist){
        if(mylist == null) return "null";
        
        String separator = "!@!";
        String result = "";
        int listlength = mylist.size();
        int count = 0;
        for(String s: mylist){
            result += s;
            if(++count<listlength) result+=separator;
        }
        return result;        
    }
    
    private String getNicePubmedEncoding(){
        if(pubmedids == null || pubmedids.isEmpty()) return "";
        
        String sep = "|";
        String result = "";
        int listlength = pubmedids.size();
        int count = 0;
        for(String s: pubmedids){
            result += InteractionGraph.esc(s);
            if(++count<listlength) result+=sep;
        }
        return result;        
        
    }
    
    private String getNiceEvidencesEncoding(){
        if(evidences == null || evidences.isEmpty()) return "No evidence available";
        
        String sep = "|";
        String result = "";
        int listlength = evidences.size();
        int count = 0;
        for(String s: evidences){
            result += "\""+InteractionGraph.esc(s)+"\"";
            if(++count<listlength) result+=sep;
        }
        return result;        
        
    }
    
    private String getCalendarListEncoding(List<Calendar> mylist){
        if(mylist == null) return "null";
        
        String separator = "!@!";
        String result = "";
        int listlength = mylist.size();
        int count = 0;
        for(Calendar s: mylist){
            if(s==null) result+="null";
            else result += s.get(Calendar.DAY_OF_MONTH)+"/"+s.get(Calendar.MONTH)+"/"+s.get(Calendar.YEAR);
            if(++count<listlength) result+=separator;
        }
        return result;        
    }

    public Term getAgent() {
        return agent;
    }

    public List<Calendar> getDates() {
        return dates;
    }

    public List<String> getEvidences() {
        return evidences;
    }

    public String getName() {
        return name;
    }

    public List<String> getPubmedids() {
        return pubmedids;
    }

    public Term getTheme() {
        return theme;
    }

    public boolean equals(Relation o){
        return compareTo(o)==0;
    }
    
    @Override 
    public int hashCode() {
        return 100*agent.hashCode()+10*theme.hashCode()+name.hashCode();
  }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Relation other = (Relation) obj;
        return equals(other);
    }
    
    public int compareTo(Relation o) {
        if(name.equals(o.name) && agent.equals(o.agent) && theme.equals(o.theme)){
            return 0;
        } else if(!name.equals(o.name)){
            return name.compareTo(o.name);
        } else if(!agent.equals(o.agent)){
            return agent.compareTo(o.agent);
        } else{
            return theme.compareTo(o.theme);
        }
    }
    
}
