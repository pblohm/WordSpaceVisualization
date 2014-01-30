package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * Helper class for reading and writing files.
 */
public class IOHandler {

//    public static final String DATADIR = "/opt/excerbt/shallowsrl/";
    public static final String DATADIR = "";

    /**
     * Helper method to parse a file.
     * @param fileName file to read from
     * @return lines lines of the file
     */
    public static Vector<String> parseFile(String fileName) {
        Vector<String> sentences = new Vector<String>();

        try {
            FileReader fr = new FileReader(DATADIR + fileName);
            BufferedReader in = new BufferedReader(fr);
            String s = in.readLine();
            while (s != null) {
                sentences.add(s.trim());
                s = in.readLine();
            }
            in.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return sentences;
    }
    
    /**
     * Helper method to parse a file.
     * @param fileName file to read from
     * @return lines lines of the file
     */
    public static Collection<String> parseFile(String fileName, Collection c) {

        try {
            FileReader fr = new FileReader(DATADIR + fileName);
            BufferedReader in = new BufferedReader(fr);
            String s = in.readLine();
            while (s != null) {
                c.add(s.trim());
                s = in.readLine();
            }
            in.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return c;
    }

    /**
     * Helper method to write to a file.
     * @param fileName file to write to (relative to datadir)
     * @param lines lines to be written
     */
    public static void writeToFile(String fileName, Vector<String> lines) {
        File f = new File(DATADIR + fileName);
        try {
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);

            for (String s : lines) {
                bw.write(s + "\n");
            }
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            System.err.println("Error writing file.");
        }
    }

    /**
     * Helper method to write to a file.
     * @param fileName file to write to (relative to datadir)
     * @param lines lines to be written
     */
    public static void writeToFile(String fileName, String[] lines) {
        File f = new File(DATADIR + fileName);
        try {
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);

            for (String s : lines) {
                bw.write(s + "\n");
            }
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            System.err.println("Error writing file.");
        }
    }
}
