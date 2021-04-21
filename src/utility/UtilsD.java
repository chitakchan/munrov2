/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Think
 */

/**
 * 
 * @author Think
 * this utility class allow conversion from long, lat (x,y) to easting and northing (EN)
 * given an origin and a table
 */
public final class UtilsD {
    private static final Logger logger = Logger.getLogger(UtilsD.class.getName());
    
    public static double[] convDegToEN(double x, double y, double xTrueO, double yTrueO){
        double[] eN = new double[2];
           double x2E = convDegToENFactor(y)[0];
           double y2N = convDegToENFactor(y)[1];
           eN[0] = (x - xTrueO) * x2E;
           eN[1] = (y - yTrueO) * y2N;
        
        return eN;
    }
            
/**
     * lookup table for xScale and yScale
         note that ground distance is in unit for each 30-arc second
         or 30/3600 degress
           * Latitude Ground distance (meters)
        (degrees) E/W N/S
        --------- ------------------------
           Equator 928 921
                10 914 922
                20 872 923
                30 804 924
                40 712 925
                50 598 927
                60 465 929
                70 318 930
                73 272 930
                78 193 930
                82 130 931

    for how to traversing arrays to find the value within the range 
    see https://books.trinket.io/thinkjava2/chapter7.html
     * @param lat
     * @return would be the ground distance in meter per arc degree
     * int[0] for lon and int[1] for lat
     */
        
  public static int[] convDegToENFactor(double lat){
  
    // this table is the ground distance in meter per 30-arc seconds
    //           * Latitude Ground distance (meters)
    //        (degrees) E/W N/S
    //    --------- ------------------------
      int[][] lookUpTble = new int[][]{
              {0, 928, 921},
              {10, 914, 922},
              {20, 872, 923},
              {30, 804, 924},
              {40, 712, 925},
              {50, 598, 927},
              {60, 465, 929},
              {70, 318, 930},
              {73, 272, 930},
              {78, 193, 930},
              {82, 130, 931}
        };
      // apply -ve latitude value for southern hemisphere assuming having same profile as 
      // north
      // int intLat = (int) Math.abs(lat);  
      double intLat = Math.abs(lat);  
      for (int i=0; i< lookUpTble.length; i++){
     
          int thisLat = lookUpTble[i][0];
          if (thisLat >= intLat) {
          int prevLat =lookUpTble[i-1][0];
              float prorata = (float) (thisLat - intLat)/(thisLat - prevLat);
              int[] arrayEN = new int[] {
                  Math.round((lookUpTble[i][1] * (1- prorata) + lookUpTble[i-1][1] * prorata)*120),
                  Math.round((lookUpTble[i][2] * (1- prorata) + lookUpTble[i-1][2] * prorata)*120)
              };
              logger.log(Level.FINER, "arrayEN for lat {0} is [{1}, {2}]", new Object[]{lat, arrayEN[0], arrayEN[1]});
            return arrayEN;
       
              }
          }
      
          return null;
      }
      
    
    
}
