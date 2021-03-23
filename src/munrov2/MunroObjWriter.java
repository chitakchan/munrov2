/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * to get coordinates for munros, create cylinders for each munros
 * merge the cyclinders and the object in WFObjWriter class
 * @author Think
 * 
 */
public class MunroObjWriter {

    private static final Logger LOG = Logger.getLogger(MunroObjWriter.class.getName());
    
    private final Properties prop;
    private ArrayList<Munro> munroList;
    
    public MunroObjWriter(Properties prop) {
        this.prop = prop;
        String fileDir = prop.getProperty("default.munrosDetailsFileDir");
        String fileName = fileDir + "\\" + prop.getProperty("default.munrosDetailsFileName");
        // this.munroList.addAll(CSVReader(fileName));
        this.munroList = CSVReader(fileName);
        // read x, y from csv file
        
        
        
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder("");
        this.munroList.forEach( e -> 
          sb.append(e.name).append(",")
            .append(e.ht).append(",")
            .append(e.lon).append(",")
            .append(e.lat).append("\n")        
                        );
        return sb.toString();
    }
    
    public String toStringObj(){
        StringBuilder sb = new StringBuilder("");
        sb.append("o ").append(prop.getProperty("default.munrosDetailsFileName")).append("\n");
        this.munroList.forEach( e -> 
          sb.append("v").append(" ")
            .append(e.lon).append(" ")
            .append(e.lat).append(" ")
             .append(e.ht).append("\n")
                        );
        return sb.toString();
    }
    
    
    
    public static void main(String[] args) {
        
        String boxName = "bottomMunrosV1";
        // get general property file under system home directory
        UserProperties prop = new UserProperties();
         
        Properties boxProp = new Properties();
        
        try {
            // get property specific to the munros
            boxProp.load(new FileInputStream(prop.getProperties("my.dear.home")+"\\"+boxName+".hdr"));
            
        } catch (IOException ex) {
            Logger.getLogger(WFObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        MunroObjWriter munro = new MunroObjWriter(boxProp);
        LOG.log(Level.INFO, "list of munros: \n {0}", munro.toString());
        LOG.log(Level.INFO, "list of munros in object format: \n {0}", munro.toStringObj());
        
    }

    /**
     * read the munro information from the file 
     * see https://www.tutorialspoint.com/how-to-read-the-data-from-a-csv-file-in-java
     * @param fileName
     * @return 
     */
    private ArrayList<Munro> CSVReader(String csvFile) {
        
        ArrayList<Munro> munroList = new ArrayList<>();
        FileReader fr = null;
        try {
            String delimiter = ",";
            File file = new File(csvFile);
            fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            String[] tempArr;
            if (prop.getProperty("HEADER") != "0") {
                br.readLine();
            }
            while ((line = br.readLine()) != null){
                tempArr = line.split(delimiter);
                /*
                for (String tempStr : tempArr) {
                    LOG.log(Level.INFO, "tempStr: {0}", tempStr);
                }
                */
                
                // create munro and add to the munroList
                Munro munro = new Munro(tempArr[2], Double.parseDouble(tempArr[3]), 
                        Double.parseDouble(tempArr[8]), Double.parseDouble(tempArr[7]));
                
                munroList.add(munro);
                // LOG.log(Level.INFO, "\n");
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MunroObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MunroObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(MunroObjWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return munroList;
    }

    class Munro {

        private final String name;
        private final double lon;
        private final double lat;
        private final double ht;
        
        public Munro(String name, double ht, double lon, double lat) {
            this.name = name;
            this.ht = ht;
            this.lon = lon;
            this.lat = lat;
        }
    }
    
    
    
            
    
}
