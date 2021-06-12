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
 
    public static void convert (String dir, String fileName, double xYShift){
        
        StringBuffer sb = new StringBuffer();       // construct a string buffer with no characters
        StringBuffer outSb = new StringBuffer();
        
        // calculate the angle from shift in X due to skewed Y rod
        // negative means shift to the left and positive to the right in a Y line of 123mm
        double tangent = - xYShift/123;
        double cos = 123.0/Math.sqrt(xYShift*xYShift + 123.0*123.0);
        
        try {
            File file = new File(dir + fileName);

            FileReader fr = new FileReader(file);       // read the file
            BufferedReader br = new BufferedReader(fr);  // create a buffereing character input stream
            double[] currentPoint = new double[2];
            String line;
            
            while ((line = br.readLine()) != null){
            // test regrex https://www.regexplanet.com/advanced/java/index.html
            // ([xX])([\d]+)[^\d]   
            // final Pattern PATTERNX = Pattern.compile("([xX])([0-9]+)\\s");
            final Pattern PATTERNX = Pattern.compile("([xX])([\\d.]+)[^\\d]");
               final Pattern PATTERNY = Pattern.compile("([xY])([\\d.]+)[^\\d]");
               Matcher mX = PATTERNX.matcher(line);
               Matcher mY = PATTERNY.matcher(line);
                   // System.out.println(line + ":");
               
               
               double[] newPoint = new double[2]; 
               double[] compPoint = null;
               
               if (mX.find()){
                newPoint[0] = Double.parseDouble(mX.group(2));
               } else {
                   newPoint[0]=currentPoint[0];
               }
               
               // only make skew amendments, if y is found
                if (mY.find()){
                    newPoint[1] = Double.parseDouble(mY.group(2));
                   compPoint = calNewPoint(currentPoint, newPoint, tangent, cos);
                   
               } else {
                    newPoint[1] = currentPoint[1];
                }
                
                outSb.append("X"+newPoint[0]).append(" ").append(" Y"+newPoint[1]);
                if (compPoint != null) {

                    String s = "";
                    s = mY.replaceFirst(" Y"+compPoint[1] +" ");
                    
                    Matcher mX1 = PATTERNX.matcher(s);
                    if (mX1.find()){
                        s = mX1.replaceFirst(" X"+compPoint[0] +" ");
                       
                    } else {
                        s += " X"+compPoint[0] + " ";
                    }
                    
                    sb.append(s);
                    
                    outSb.append(" >> X"+compPoint[0]).append(" ").append(" Y"+compPoint[1]);
                    
                } else {
                    sb.append(line);
                }
                outSb.append("\n");
                sb.append("\n");
                
                currentPoint = newPoint;
            }
            fr.close();
            System.out.println(outSb.toString());
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
        
    // write to file
        try {
            FileOutputStream fos = new FileOutputStream(dir + "Converted_"+fileName);
            byte[] bytesArray = sb.toString().getBytes();
            
            fos.write(bytesArray);
            fos.flush();
        
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GSkewXYCom.class.getName()).log(Level.SEVERE, null, ex);
        }
             
    }
    
    private static double[] calNewPoint(double[] currentPoint, double[] newPoint, double tangent, double cos) {
        double[] delta = new double[2];
        delta[0] = newPoint[1] * tangent;
        delta[1] = newPoint[1] *(1/cos - 1);
        double[] compPoint = new double[2];
        compPoint[0] = newPoint[0] + delta[0];
        compPoint[1] = newPoint[1] + delta[1];
        return compPoint;
    }

     public static void main(String[] args){
        // String dir ="C:\\Users\\Think\\OneDrive\\Documents\\3Dprint\\taiwan\\";
         // String fileName = "taiwan4thPrintBox2ndPrintSDTest.gco";
         String dir = "F:\\Users\\Think\\MunroProject\\taiwanMap\\";
         String fileName = "skewedCodeV2.gco";
         
         double shift = -2.0; // shift to the left in 123mm of upward Y
         convert(dir, fileName, shift);
     
    }
}    
