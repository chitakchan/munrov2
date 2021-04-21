/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.awt.geom.Rectangle2D;
import java.util.Properties;
import munrov2.Dem;
import static utility.UtilsD.convDegToENFactor;

/**
 *
 * @author Think
 * this class attempts to provide a vertice class for universal application
 * may try to use interface
 * VerticeEN
 */
public class MyVertice implements Vertice {
    /**
     * Vertice class 
     */
        double x, y, z;
        int r, c;
        double[] eNZ = new double[3];
        double[] eNZFalse = new double[3];
        double[] eNOrigin = new double[2];
        double[] eNFalseOrigin = new double[2];
        Properties prop;
        
        public MyVertice(int r, int c, double z, Rectangle2D rect2DBox, double xyIncStep){
            this.r = r;
            this.c = c;
            this.x = rect2DBox.getX()  + c*xyIncStep;
            this.y = rect2DBox.getY() - r*xyIncStep;
            
            this.z = z;
        }
        public MyVertice(int r, int c, double x, double y, double z){
            this.r = r;
            this.c = c;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public MyVertice(double x, double y, double z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
        // constructor for input as easting and northing with reference to the false origin
        public MyVertice(double e, double n, double z, double xTrueO, double yTrueO, double eFalseO, double nFalseO){
            double eTrue = e - eFalseO;
            double nTrue = n - nFalseO;
            int[] t = new int[2];
            t = utility.UtilsD.convDegToENFactor(yTrueO); // there is an issue with this yTrue may be too far away from y which is not known as yet.  to be dealt with later
            this.x = xTrueO + eTrue / t[0];
            this.y = yTrueO + nTrue / t[1];
            }
        
        // constructor for input as easting and northing with reference to the true origin 
        public MyVertice(double e, double n, double z, double xTrueO, double yTrueO){
            this(e, n, z, xTrueO, yTrueO, 0, 0);
            
            }    
        
    @Override
    public double[] getXYZ() {
        double[] xyz = {
            this.x, this.y, this.z
        };
        
        return xyz;
    }

    
    // calculate the easting and northing from lon and lat
    // without further parameters the false origin is the same location oas true origin
    // @Override
    public double[] getENZ(double xTrueO, double yTrueO, double eFalseO, double nFalseO) {
        
            int[] t = utility.UtilsD.convDegToENFactor(this.y);
            double eTrue = (this.x - xTrueO ) *  t[0] ;
            double e = eTrue - eFalseO ;
            
            double nTrue = (this.y - yTrueO ) *  t[1] ;
            double n = nTrue - nFalseO ;
        
         return new double[]{e, n, this.z};
    }    
    
        // calculate the easting and northing from lon and lat
    // without further parameters the false origin is the same location oas true origin
    @Override
    public double[] getENZ(double xTrueO, double yTrueO) {
        
         return getENZ(xTrueO, yTrueO, 0,0);
    }        
        
    public String toString(Properties boxProp){
            Rectangle2D rect2DBox = new Rectangle2D.Double(
            Double.parseDouble(boxProp.getProperty("ULXMAP")),
            Double.parseDouble(boxProp.getProperty("ULYMAP")),
            Double.parseDouble(boxProp.getProperty("WIDTH")),
            Double.parseDouble(boxProp.getProperty("HEIGHT"))
         );
                Dem dem = new Dem(rect2DBox, 
                boxProp.getProperty("Gtopo30.dem.dir"), 
                boxProp.getProperty("default.demFileNamePt1")); 
  
        double xTrueO = Double.parseDouble(boxProp.getProperty("XTRUEO","0.0"));
        double yTrueO = Double.parseDouble(boxProp.getProperty("YTRUEO","0.0"));
        // offset to falso origin:  eFalseOff, nFalse e' = e + eFalseOff; n' = n + nFalseOff
        double eFalseO = Double.parseDouble(boxProp.getProperty("EFALSEO","0.0"));
        double nFalseO = Double.parseDouble(boxProp.getProperty("NFALSEO","0.0"));
        double seaRepZ = Double.parseDouble(boxProp.getProperty("SEAREPZ","-0"));
        double zGWSeaLvl = Double.parseDouble(boxProp.getProperty("ZGWSEALVL","0.0"));
        double measUnit = Double.parseDouble(boxProp.getProperty("DISTSCALE","0.001"));
        double zExagg = Double.parseDouble(boxProp.getProperty("ZEXAGG","1.0"));
        double baseDepth = Double.parseDouble(boxProp.getProperty("BASEDEPTHPCT","0.05")) *
                dem.adjRect2DBox.getWidth()*convDegToENFactor(dem.adjRect2DBox.getY())[0];
        
        // double measUnit, double seaRepZ
        
            
            /*
        // sea was represented by usgs as -9999.  changed to a new value here.
        //   xyIncStep = 30.0/3600;  // need to use 30.0 otherwise the division would be treated as int and as result xyIncStep become zero.
        // ground distance for 30-arc seconds         
        // get the scale to translate deg to meter;   0.001 refers to km, 1 refers to m.
                
            */
            StringBuilder sb = new StringBuilder("");    
            double[] eNZ = getENZ(xTrueO, yTrueO, eFalseO, nFalseO);
           sb.append("v ").append(eNZ[0] * measUnit);
           sb.append(" ").append(eNZ[1] * measUnit);
           // Z of the geographical model should be pushed upwards by the baseDepth to 
           // avoid negative z,
           // because it is an offset and is dependent on width of the model
           // so baseDepth should be outside the calculation of z itself
           // sb.append(" ").append((((z == -9999) ? seaRepZ  : z * zExagg) + baseDepth)* measUnit  ).append("\n");
           // zGWSeaLvl is the sea level rise due to global warming.  land below this level 
           // would be submerged and represented as 80% of the seaDepth (should be in negative figure in the hdr file
           
           
           sb.append(" ").append((z * zExagg + baseDepth*0)* measUnit).append("\n");
           
           /*
           sb.append(" ").append((((z == -9999) ? 
                   seaRepZ  : 
                   ((z > zGWSeaLvl) ? z * zExagg : seaRepZ * 1.5)  // when z less than zGWSeaLvl it go deeper then sea level
                        ) 
                    + baseDepth)* measUnit)
                   .append("\n");
           
           */
           
           return sb.toString();
        }

    public String toStringForWFObjWriter(Properties boxProp){
        
        
        Rectangle2D rect2DBox = new Rectangle2D.Double(
            Double.parseDouble(boxProp.getProperty("ULXMAP")),
            Double.parseDouble(boxProp.getProperty("ULYMAP")),
            Double.parseDouble(boxProp.getProperty("WIDTH")),
            Double.parseDouble(boxProp.getProperty("HEIGHT"))
         );
                Dem dem = new Dem(rect2DBox, 
                boxProp.getProperty("Gtopo30.dem.dir"), 
                boxProp.getProperty("default.demFileNamePt1")); 
  
        double xTrueO = Double.parseDouble(boxProp.getProperty("XTRUEO","0.0"));
        double yTrueO = Double.parseDouble(boxProp.getProperty("YTRUEO","0.0"));
        // offset to falso origin:  eFalseOff, nFalse e' = e + eFalseOff; n' = n + nFalseOff
        double eFalseO = Double.parseDouble(boxProp.getProperty("EFALSEO","0.0"));
        double nFalseO = Double.parseDouble(boxProp.getProperty("NFALSEO","0.0"));
        double seaRepZ = Double.parseDouble(boxProp.getProperty("SEAREPZ","-0"));
        double zGWSeaLvl = Double.parseDouble(boxProp.getProperty("ZGWSEALVL","0.0"));
        double measUnit = Double.parseDouble(boxProp.getProperty("DISTSCALE","0.001"));
        double zExagg = Double.parseDouble(boxProp.getProperty("ZEXAGG","1.0"));
        double baseDepth = Double.parseDouble(boxProp.getProperty("BASEDEPTHPCT","0.05")) *
                dem.adjRect2DBox.getWidth()*convDegToENFactor(dem.adjRect2DBox.getY())[0];
        
        // double measUnit, double seaRepZ
        
            
            /*
        // sea was represented by usgs as -9999.  changed to a new value here.
        //   xyIncStep = 30.0/3600;  // need to use 30.0 otherwise the division would be treated as int and as result xyIncStep become zero.
        // ground distance for 30-arc seconds         
        // get the scale to translate deg to meter;   0.001 refers to km, 1 refers to m.
                
            */
            StringBuilder sb = new StringBuilder("");    
            double[] eNZ = getENZ(xTrueO, yTrueO, eFalseO, nFalseO);
           sb.append("v ").append(eNZ[0] * measUnit);
           sb.append(" ").append(eNZ[1] * measUnit);
           // Z of the geographical model should be pushed upwards by the baseDepth to 
           // avoid negative z,
           // because it is an offset and is dependent on width of the model
           // so baseDepth should be outside the calculation of z itself
           // sb.append(" ").append((((z == -9999) ? seaRepZ  : z * zExagg) + baseDepth)* measUnit  ).append("\n");
           // zGWSeaLvl is the sea level rise due to global warming.  land below this level 
           // would be submerged and represented as 80% of the seaDepth (should be in negative figure in the hdr file
           
           
           
           sb.append(" ").append((((z == -9999) ? 
                   seaRepZ  : 
                   ((z > zGWSeaLvl) ? z * zExagg : seaRepZ * 1.5)  // when z less than zGWSeaLvl it go deeper then sea level
                        ) 
                    + baseDepth)* measUnit)
                   .append("\n");
            
//            sb.append(" ").append((((z == -9999) ? seaRepZ : z ))).append("\n");
           return sb.toString();
        }

    
    @Override
    public double[] getRCZ() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    }

