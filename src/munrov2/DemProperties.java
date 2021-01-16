/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author Think
 * to create a class of user properties to store user data for easy retrieval
 * the class would get FileInputStream from a .properties file under the user home directory 
 * the user home directory is retrieved by System.getProperty("user.home") 
 */
public final class DemProperties {
    
    // "C://Users//Think//Documents//NetBeansProjects//JobTreeAppStandAlone//"
    
     private static final Logger logger = Logger.getLogger(DemProperties.class.getName());
     Properties pro;
     String sTestDir;
     String sDir;
     String sPropertyFileName;
     
     public DemProperties(){
         
         this("gt30w020n90.hdr");
         
     }
    public DemProperties(String headerFileName) {
        sPropertyFileName = headerFileName;
        // locate the full path of the header file
        UserProperties prop = new UserProperties();
        sDir = prop.getProperties("Gtopo30.dem.dir");
        sPropertyFileName = sDir + "\\"+ sPropertyFileName;
        
        // load the header file as properties file
        pro = new Properties();
        
        logger.log(Level.INFO, "attempting to getting dem hear file from header file.  {0}", 
                sPropertyFileName);
         try {
             // FileInputStream in = new FileInputStream(sPropertyFileName);
             pro.load(new FileInputStream(sPropertyFileName));
         } catch (IOException ex) {
            // Exceptions.printStackTrace(ex);
             logger.log(Level.SEVERE, "IOException {0}", sPropertyFileName );
         }
   }       

public String getProperties(String fieldName) {
    return this.pro.getProperty(fieldName);
}
    
}
