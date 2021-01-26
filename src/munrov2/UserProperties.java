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
public final class UserProperties {
    
    // "C://Users//Think//Documents//NetBeansProjects//JobTreeAppStandAlone//"
    
     private static final Logger logger = Logger.getLogger(UserProperties.class.getName());
     Properties pro;
     String sDir, sTestDir;
     String sPropertyFileName;
     
    public UserProperties() {
        this("munroproject.properties");
    }       

    public UserProperties(String propertiesFileName) {
        
        pro = new Properties();
       
        this.sPropertyFileName = System.getProperty("user.home") + "\\"+ propertiesFileName;
        logger.log(Level.INFO, "attempting to getting property file from user.home.  {0}", sPropertyFileName);
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


/**
 * Given a title of the region it had the .hdr, say hongkong.hdr scotland.hdr
 * under the user home directory,  from the hdr file the parameters to run the WFObjWriter class
 * is retrieved and pass to the class constructor
 * 26th Jan 2021
 */
public Properties getPropFromUserHome(String titleRegion){
     pro = new Properties();
       
        
        logger.log(Level.INFO, "attempting to getting property file from user.home.  {0}", sPropertyFileName);
         try {
             // FileInputStream in = new FileInputStream(sPropertyFileName);
             pro.load(new FileInputStream(this.sPropertyFileName));
             
             return pro;
             
         } catch (IOException ex) {
            // Exceptions.printStackTrace(ex);
             logger.log(Level.SEVERE, "IOException {0}", this.sPropertyFileName );
         }
        
return null;    
}
    
}
