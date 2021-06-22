/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import munrov2.WFObjWriter;

/**
 *
 * @author Think
 * to solve the problem in 3D printing
 * that the y axis is skewed either to a certain amount of left (+ve) or to the right (-ve) per 123mm of y
 * the program basically extract the g code file line by line
 * then get the x and y numerics,
 * some other codes like g28 will return to a specific position,
 * and then calculate the additional vector of the skewed system for compensation
 */
public class GSkewXYCom {
 
    public static void convert (String dir, String fileName, double xShift, double yLength){
        
        StringBuffer sb = new StringBuffer();       // construct a string buffer with no characters
        StringBuffer outSb = new StringBuffer();
        
        // calculate the angle from shift in X due to skewed Y rod
        // negative means shift to the left and positive to the right in a Y line of 123mm
        double tangent = - xShift/yLength;
        double cos = yLength/Math.sqrt(xShift*xShift + yLength*yLength);
        
        double minX = 999.9, minY = 999.9, maxX = -999.9, maxY= -999.9;
        
        // make sure it comes with the backlash
        if (dir.substring(dir.length()) != "\\"){
            dir += "\\";
        }
        
        try {
            File file = new File(dir + fileName);

            FileReader fr = new FileReader(file);       // read the file
            BufferedReader br = new BufferedReader(fr);  // create a buffereing character input stream
            double[] lastCoord = new double[2];
            String line;
            
            while ((line = br.readLine()) != null){

                // identify the skewed position as per the original gcode
               double[] newPoint = new double[2]; 
               
               newPoint = whereAmI(lastCoord, line);
               
               // calculate the coordinates which add compensation x and y delta 
               // to the newPoint in current line
               
               double[] compPoint = null;
               compPoint = calCompPoint(newPoint, tangent, cos);
               if (compPoint != null) {
                   
                   // home position is not compared
                   if ((compPoint[0]!=0.0) && (compPoint[1]!=0.0)) {
                        minX = Math.min(minX, compPoint[0]);
                        minY = Math.min(minY, compPoint[1]);
                   }
                    maxX = Math.max(maxX, compPoint[0]);
                    maxY = Math.max(maxY, compPoint[1]);
                   
               } 
               
                // replace the line with comPoint
                String newLine ="";
                newLine = replaceWithCompPoint(compPoint, line);
               
               sb.append(newLine).append("\n"); // add to the string buffer for writing later
               
                outSb.append(line).append(" >> ").append(newLine).append("\n");
                          
                lastCoord = newPoint;
            }
            fr.close();
            

                    System.out.println("complete conversion, writting to file...");
            
            
            // System.out.println(outSb.toString());
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
        
            String outFileName = dir + "Converted_"+fileName;
        
            StringBuilder sbRecord = new StringBuilder("; conversion output:\n");
            sbRecord.append("; input file: " + dir + fileName + "\n");
            sbRecord.append("; output file: " + outFileName + "\n");
            sbRecord.append("; xShift/xLength: " + xShift 
                    + "mm shift in x (-ve to left and +ve to right) per a traverse in y direction of " 
                    + yLength + "mm\n"); 
            sbRecord.append("; max x, y = " + maxX + ", " +  maxY +
                             "min x, y = " + minX + ", " +  minY + "\n");
            sbRecord.append("; if max x exceeding the maximum allowable X use G92 to offset by"
                    + "at least the amount of (maxX in code - printMaxX) or reduce the scale\n");
            
            
            // add to the gcode for records
            
            sb.append(sbRecord.toString());
            System.out.println(sbRecord.toString());
        
        
            
        
        
        
        
    // write to file
        try {
            
            FileOutputStream fos = new FileOutputStream(outFileName);
            byte[] bytesArray = sb.toString().getBytes();
            
            fos.write(bytesArray);
            fos.flush();
        
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        

            
    }
    
    /*
    return the point using the skewed orthogonal coordinate system
    */
    private static double[] calCompPoint(double[] newPoint, double tangent, double cos) {
        /*
        double[] delta = new double[2];
        delta[0] = newPoint[1] * tangent;
        delta[1] = newPoint[1] *(1/cos - 1);
        
        compPoint[0] = newPoint[0] + delta[0];
        compPoint[1] = newPoint[1] + delta[1];
        
        */
        double[] compPoint = new double[2];
        compPoint[0] = newPoint[0] + newPoint[1]*tangent;
        compPoint[1] = newPoint[1] / cos;
        
        return compPoint;
    }
    /*
     replace the line with compensated point,
     1) if the line consists both x and y, replace with the compensation coord
     2) if the line consists only with x and dont involve y, keep the line intact
     3) if the line consists only with y and dont involve x, in that case add the 
        compensated x to the line, together with replacing compensated y value in the line
     4) if the line dont involve either x or y, keep the line intact
    
    */
    private static String replaceWithCompPoint(double[] compPoint, String line){
        final Pattern PATTERNX = Pattern.compile("([xX])([\\d.]+)[^\\d]");
               final Pattern PATTERNY = Pattern.compile("([xY])([\\d.]+)[^\\d]");
                
               Matcher mX = PATTERNX.matcher(line);
               Matcher mY = PATTERNY.matcher(line);
        
        String s ="";
        if (compPoint != null && (mY.find() || mX.find() )) {
                    // replace Y with compensated value
                    s = mY.replaceFirst(" Y"+String.format("%.3f", compPoint[1]) +" ");
                    
                Matcher mX1 = PATTERNX.matcher(s);
                    if (mX1.find()){
                        s = mX1.replaceFirst(" X"+String.format("%.3f", compPoint[0]) +" ");
                       
                    } else {
                        
                        StringBuilder sTemp = new StringBuilder(s);
                        int halfway = (int) mY.start();
                        sTemp.insert(halfway, " X"+String.format("%.3f", compPoint[0]) + " ");
                        // mY1.start()
                        s = sTemp.toString();
                    }
                   
                    return s;
                    
                } else {
                    
                    return line;    // no X or Y in the line, just return intact
                }
        
    };
    private static double[] whereAmI(double[] lastCoord, String line) {
        
        // test regrex https://www.regexplanet.com/advanced/java/index.html
            // ([xX])([\d]+)[^\d]   
            // final Pattern PATTERNX = Pattern.compile("([xX])([0-9]+)\\s");
            final Pattern PATTERNX = Pattern.compile("([xX])([\\d.]+)[^\\d]");
               final Pattern PATTERNY = Pattern.compile("([xY])([\\d.]+)[^\\d]");
               Matcher mX = PATTERNX.matcher(line);
               Matcher mY = PATTERNY.matcher(line);
               
               double[] newPoint = new double[2];
               // consider three circumstances:
               // G1 X50 Y50
               // G1 X50
               // G1 Y50
               // G92 X40
               // G90
               //
               
               if (mX.find()){
                newPoint[0] = Double.parseDouble(mX.group(2));
               } else {
                   newPoint[0]=lastCoord[0];
               }
               
               // only make skew amendments, if y is found
                if (mY.find()){
                    newPoint[1] = Double.parseDouble(mY.group(2));
            //        compPoint = calNewPoint(lastCoord, newPoint, tangent, cos);
                   
               } else {
                    newPoint[1] = lastCoord[1];
                }
            
        return newPoint;
    }

     public static void main(String[] args){
        // String dir ="C:\\Users\\Think\\OneDrive\\Documents\\3Dprint\\taiwan\\";
         // String fileName = "taiwan4thPrintBox2ndPrintSDTest.gco";
        // String dir = "F:\\Users\\Think\\MunroProject\\taiwanMap\\";
        // String fileName = "skewedCodeV2.gco";
        // String dir ="C:\\Users\\Think\\OneDrive\\Documents\\3Dprint\\taiwan\\";
        // String fileName = "taiwanGMTED2010_150V1a-box_001_app_code.gcode";
        
        // String dir ="C:\\Users\\Think\\OneDrive\\Documents\\3Dprint\\taiwan\\";
         // String fileName = "taiwanGMTED2010_150V1a-taiwanGMTED150V1obj_001_app_code.gcode";
         // taiwan box 6th print
         String dir = "F:\\Users\\Think\\MunroProject\\taiwanMap\\";
         String fileName = "taiwanGMTED2010_150V2-box_001_app.gcode";
         // taiwan box 7th print with key hole
         fileName = "taiwanGMTED2010_150V2-box_001_app_001.gcode";
         // 8th print for box with key hole and brim
         fileName = "taiwanGMTED2010_150V2-box_001_app.gcode";
         // 9th print for box, 
         fileName = "taiwanGMTED2010_150V2-box_001_app_scale036.gcode";
         // 9th print for model
         fileName = "taiwanGMTED2010_150V2-taiwanGMTED150V1obj_001_app_code_scale036.gcode";
         // 9th print for model, greater scale to max the length to 200
         // fileName = "taiwanGMTED2010_150V2-taiwanGMTED150V1obj_001_app_code.gcode";
         // 1st print for iceland verticle, and with a matching box
         dir = "F:\\Users\\Think\\MunroProject\\iceLandMap\\";
         fileName ="icelandRectV2-iceLandV2Union_001.gcode";
         // 1st print for iceland vertical box
         fileName = "icelandRectV2-box_001.gcode";
         //10th print for taiwan, make further xShift (1.304mm to the left)
         fileName = "taiwanGMTED2010_150V2-taiwanGMTED150V1obj_001_app_code_scale036.gcode";
         dir = "F:\\Users\\Think\\MunroProject\\taiwanMap\\";
         // 10th print for taiwan box, make further xShift as above for the model
        // fileName = "taiwanGMTED2010_150V2-box_001_app_scale036.gcode";
        // standard box first print, based on golden ration 170 x 105 mm
        dir = "F:\\Users\\Think\\MunroProject";
        fileName = "standardBox-externalBlk_001.gcode";
        // 2nd print iceland with 170x105 dimension
        dir = "F:\\Users\\Think\\MunroProject\\iceLandMap\\";
        fileName = "icelandRectV3-iceLandV2Union_002.gcode";
        // 1st print for scotland rectangle with box design
        dir = "F:\\Users\\Think\\MunroProject\\scotlandMap\\";
        fileName = "scotlandRectV2-scotlandRectV1Union_001_app.gcode";
        // 2nd print for rectangle box with keyhole on longedge
        dir = "F:\\Users\\Think\\MunroProject\\standardBox170x105";
        fileName = "standardBoxV2-box_001_app_keyholeOnLongEdge.gcode";
        // himalaya model
        dir = "F:\\Users\\Think\\MunroProject\\indiaMap\\RectV1";
       fileName = "himalayaPlusTibetRectV1-indianX70Y38W15H3V3_obj_002_app.gcode";
       //unst rect model p1
        dir = "F:\\Users\\Think\\MunroProject\\unstMap\\";
       fileName = "unstRect170x105V1-unstV3ZEexagg08_obj_002_app.gcode";
       
         
         // double shift = -3.5-1.394; // shift to the left in 123mm of upward Y
         double shift = -3.5+1.394; // shift to the left in 123mm of upward Y

         double yLength = 123.0;
         convert(dir, fileName, shift, yLength);
     
    }
}    
