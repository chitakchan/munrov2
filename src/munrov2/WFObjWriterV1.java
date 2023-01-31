/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

/**
 *
 * @author Think
 */
public class WFObjWriterV1 {
    
    public static void WFObjWriter(String boxName, int numRuns){
        
        for (int i=0; i < numRuns; i++){
            new WFObjWriter(boxName, i);
        }
        
    }
    public static void WFObjWriter(String boxName){
        
        WFObjWriter(boxName, 1);
        
    }
     public static void main(String args[]){
        // for scotland X, Y, WIDTH, HEIGHT
         // Rectangle2D rect2DBox = new Rectangle2D.Double(-7.1, 58.8, 5.5, 4.5);
         // define the new origin to offset array.  From this lat and long 
         // all EN from vertice.toString() will explorate outwards.
         // For UK 
         
         // read parameters from local file
         // all local parameters are saved in the directory as defined in 
         // the UserProperties
         
         // printed and published in etsy for scotlandV1
         // String boxName = "scotlandV1";  
         
        // printed and published in etsy for OrkneyShetLand
        //   String boxName = "scotlandOrkneyShetlandV1";

          // String boxName = "iceLandV1Left";  
           // String boxName = "iceLandV1Right";  
         //  String boxName = "jeju";  
         // String boxName = "indiaV1";  // 4TH PRINT UNDERWAY
           // String boxName = "iceLandV2Left";  
          // String boxName = "iceLandV2Right";  
         // String boxName = "unst";  
         
           // String boxName = "benNevisV1";  // 1st print on 30th may 2021
        // WFObjWriter obj = new WFObjWriter(boxProp);
        
        // String boxName = "unstGMTED075";  
        // String boxName = "taiwanGMTED075";  
        // String boxName = "taiwanGMTED075Left";  
     //   String boxName = "taiwanGMTED150";  
       String boxName = "taiwanGMTED150Left";  
        
       // scotland 1st print for rectangular design
       boxName = "scotlandV2";  
       // to solve the heap memory issue due to large map area and hence array size
       boxName = "indiaV1Test";  
       // ben nevis using GMTED2010
       boxName = "benNevisGMTED2010V1";
       
       // hong kong Rect in colloboration with king
       boxName = "hongkongRect";
       
       // ukraine map
           boxName = "ukraine";

        // WFObjWriter obj = new WFObjWriter(boxName, 2);
       
        // WFObjWriterV1.WFObjWriter(boxName, 3);
        WFObjWriterV1.WFObjWriter(boxName);
        
    }   
    
    
    
}
