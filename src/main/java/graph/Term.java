package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author philipp
 */
public class Term implements Comparable<Term> {

    String name;
    Map<String,Integer> types;
    List<String> aliases;
    
    public Term(String s){
        s = s.replaceAll("<", "(");
        s = s.replaceAll(">", ")");
        this.name = s;
    }
    
    public Term(String s, List<String> a){
        this(s);
        aliases = a;
    }
    
    public Term(String s, String t){
        this(s);
        types = new HashMap<String, Integer>();
        types.put(t, 1);
    }
    
    public Term(String n, Map<String,Integer> t, List<String> a){
        this(n,a);
        types = t;
    } 
    
    @Override
    public String toString(){
        return name;
    }
    
    public static Term getTermFromSimpleEncoding(String enc){
        String[] parts = enc.split("§@§");
        if(parts.length!=3) return null;
        
        String name = parts[0];
        Map<String,Integer> map = getMapFromSimpleEncoding(parts[1]);
        List<String> list = getListFromSimpleEncoding(parts[2]);
        return new Term(name,map,list);
    }
    
    public static TermIDTuple getTermFomNicerEncoding(String enc){
        String[] parts = enc.split("\t");
        if(parts.length>3 || parts.length <2){
            System.out.println("Incorrect term format: "+enc);
            return null;
        }
        
        String[] names = parts[1].split("\\|");
        String[] types = parts.length==2?new String[]{"Default"}:parts[2].split("\\|");
        List<String> aliases = new ArrayList(Arrays.asList(names));
        aliases.remove(0);
        Map<String,Integer> map = new HashMap<String,Integer>();
        int counter = 1;
        for(String s: types){
            map.put(s,counter++);
        }
        
        return new TermIDTuple(parts[0], new Term(names[0],map,aliases));
    }
    
    private static List<String> getListFromSimpleEncoding(String enc){
        if(enc.equals("null")) return null;
        String[] parts = enc.split("!@!");
        return Arrays.asList(parts);
    }
    
    private static Map<String,Integer> getMapFromSimpleEncoding(String enc){
        if(enc.equals("null")) return null;
        String[] parts = enc.split("!@!");
        if(parts.length%2!=0) return null;
        
        Map<String,Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<parts.length;){
            map.put(parts[i++], Integer.parseInt(parts[i++]));
        }
        
        return map;
    }
    
    public String getNicerEncoding(int id){
        String separator = "\t";
        String typeEnc = (types == null) || (types.size()==1 && types.containsKey("Default"))?null:getNicerTypesEncoding();
        String encoding = "T"+id+separator+getNicerAliasesEncoding();
        if(typeEnc!=null) encoding += separator+typeEnc;
        return encoding;
    }
    
    public String getSimpleEncoding(){
        String separator = "§@§";
        String encoding = name+separator+getTypesEncoding()+separator+getAliasesEncoding();
        return encoding;
    }
    
    private String getNicerAliasesEncoding(){
        if(aliases == null) return name;
        if(aliases.isEmpty()) return name;
        
        String separator = "|";
        String result = InteractionGraph.esc(name);
        for(String s: aliases){
            result += separator;
            result += InteractionGraph.esc(s);
        }
        return result;                
    }
    
    private String getAliasesEncoding(){
        if(aliases == null) return "null";
        if(aliases.isEmpty()) return "null";
        
        String separator = "!@!";
        String result = "";
        int alength = aliases.size();
        int count = 0;
        for(String s: aliases){
            result += s;
            if(++count<alength) result+=separator;
        }
        return result;        
    }
    
    private String getTypesEncoding(){
        if(types == null) return "null";

        String separator = "!@!";
        String result = "";
        int typeslength = types.size();
        int count = 0;
        for(String s: types.keySet()){
            result+=s+separator+types.get(s);
            if(++count<typeslength) result +=separator;
        }
        return result;
    }
    
    private String getNicerTypesEncoding(){
        if(types == null) return "Default";
        
        String separator = "|";
        String result = "";
        int typeslength = types.size();
        int count = 0;
        for(String s: types.keySet()){
            result+=InteractionGraph.esc(s);
            if(++count<typeslength) result +=separator;
        }
        return result;        
    }

    public int compareTo(Term o) {
        if(o.aliases!=null && o.aliases.contains(name)) return 0;
        if(aliases!=null && aliases.contains(o.name)) return 0;
        return o.name.toLowerCase().compareTo(name.toLowerCase());
    }
    
    public boolean equals(Term o){
        return compareTo(o)==0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Term other = (Term) obj;
        return equals(other);
    }
    
    @Override
    public int hashCode(){
        return name.toLowerCase().hashCode();
    }
    
    public List<String> getAliases() {
        return aliases;
    }
    
    public void setAliases(List<String> aliases){
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public Map<String, Integer> getTypes() {
        return types;
    }
}
