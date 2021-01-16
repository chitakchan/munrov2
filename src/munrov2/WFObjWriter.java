/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Think
 * attempt to write 3d model in object file to avoid using normals
 * update 30th Dec 2020; branch: creatingSurfaceAndWritingObjFile 
 * 1. start writing a 7 rows x 5 columns of triangular surfaces 
 * 2. structure:  a. userproperties class to get prop; 
 * 3. specification of obj file:
 *   
     * to create surfaces from the vertices x, y, z
     * method:  
     * format:
     * v x1 y1 z1
     * v x2 y2 z2
     * v x3 y3 z3
     * f 1 2 3
     * 
     * the sequence of f 1 2 3 is ordered to follow right hand rule, that is 
     * anticlockwise with the thumb pointing at the normal
     
 */
public class WFObjWriter {
    private static final Logger logger = Logger.
                getLogger(WFObjWriter.class.getName());

        double[][] x, y, z;
          /*
        int xyIncStep = 15;
        double xULCorner = 0;
        double yULCorner = 100;
        double xLRCorner = 51;
        double yLRCorner = 15;
        */
        double xyIncStep, xULCorner, yULCorner, xLRCorner, yLRCorner; // lat 57, lon -5 around fort william
        double xBoxWidth, yBoxWidth;
        
        int nofRows, nofCols;
        ArrayList<Vertice> vertices = new ArrayList<Vertice>();
        ArrayList<TriFace> triFaces = new ArrayList<TriFace>();

        
        public void AdjCorners(){
                
       logger.log(Level.INFO, "before adjustment: \nULCorner {0}, {1}; LRCorner {2}, {3}\n", new Object[]{
            xULCorner, yULCorner, xLRCorner, yLRCorner});
       
        // adjust the xULCorner and yULCorner to the point in line with the DEM 
        // floor is to find the double which is smaller then the argument and is equal to 
        // a mathematical integer.  ceil is conversely the similar but is bigger than the 
        // argument
        // convert the deg to sec then div by 30 arc sec, 
        xULCorner = Math.floor(xULCorner*120)/120;
        yULCorner = Math.ceil(yULCorner*120)/120;
        xLRCorner = Math.ceil(xLRCorner*120)/120;        
        yLRCorner = Math.floor(yLRCorner*120)/120;
        logger.log(Level.INFO, "After adjustment with Math.ceil and floor:\nULCorner {0}, {1}; LRCorner {2}, {3}\n", new Object[]{
            xULCorner, yULCorner, xLRCorner, yLRCorner}
        );
        
        }
        
        
public WFObjWriter (double[] ul, double[] lr) {
     //   xyIncStep = 30.0/3600;  // need to use 30.0 otherwise the division would be treated as int and as result xyIncStep become zero.
       xyIncStep = 0.00833333333333;
                   
        xULCorner = ul[0];
        yULCorner = ul[1]; // lat 59, lon -8 at sea
        xLRCorner = lr[0];
        yLRCorner = lr[1]; // lat 57, lon -5 around fort william
        
        AdjCorners();
        
        xBoxWidth = xLRCorner - xULCorner; // -1 -  (-8) = 7 
        yBoxWidth = yLRCorner - yULCorner; // 54 - 60 = -6
}        
 
        
    public int RC2Vno (int r, int c) {
        
        return nofCols * r + c;
    }
    
    public void CreateSurfaces () {
        
        // generate faces from the whole panel
        int r, c;
            for (int i = 0; i<vertices.size(); i++){
                r = vertices.get(i).r;
                c = vertices.get(i).c;
                
                    if (c < nofCols-1 & r < nofRows-1)  {
                        TriFace tri = new TriFace(vertices.get(i));
                        triFaces.add(tri);
                        
                        
                    }
            }
    }
    public class TriFace {
        int[] triVertices = new int[6];  
        public TriFace(Vertice v) {
            triVertices[0]=RC2Vno(v.r, v.c);
            triVertices[1]=RC2Vno(v.r+1, v.c);
            triVertices[2]=RC2Vno(v.r, v.c+1);
            triVertices[3]=RC2Vno(v.r+1, v.c+1);
            triVertices[4]=RC2Vno(v.r, v.c+1);
            triVertices[5]=RC2Vno(v.r+1, v.c);
            
        }
        public String toString(){
            StringBuilder sb = new StringBuilder("");
        
            sb.append("f ").append(triVertices[0]+1);
            sb.append(" ").append(triVertices[1]+1);
            sb.append(" ").append(triVertices[2]+1).append("\n");
            sb.append("f ").append(triVertices[3]+1);
            sb.append(" ").append(triVertices[4]+1);
            sb.append(" ").append(triVertices[5]+1).append("\n");
            return sb.toString();
        }
        
    }
 
    public class Vertice {
        double x, y, z;
        int r, c;
        
        public Vertice(int r, int c, double z){
            this.r = r;
            this.c = c;
            this.x = xULCorner + c*xyIncStep;
            this.y = yULCorner - r*xyIncStep;
            this.z = z;
        }
        public Vertice(int r, int c, double x, double y, double z){
            this.r = r;
            this.c = c;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public String toString(){
                 StringBuilder sb = new StringBuilder("");
        
                sb.append("v ").append(x);
                sb.append(" ").append(y);
                sb.append(" ").append(z).append("\n");
                return sb.toString();
        }
    }
    
    
    public double[][] GetZ (int noOfCols, int noOfRows ) {
        // use random number within range of say 0 to 50
        // see https://stackoverflow.com/questions/363681/how-do-i-generate-random-integers-within-a-specific-range-in-java
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        double[][] zt = new double[noOfRows][noOfCols];
        double max = 30.0/3600;  // 30 arc second represent about 1kms
        double min = 0.0;
        
        for (int i=0; i<zt.length; i++){
            for (int j=0; j<zt[i].length; j++){
                zt[i][j] = ThreadLocalRandom.current().nextDouble(min, max);
            }
        }
        return zt;
    }
    
    
    
    public double GetZ (double x, double y ) {
        // use random number within range of say 0 to 50
        // see https://stackoverflow.com/questions/363681/how-do-i-generate-random-integers-within-a-specific-range-in-java
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        
        double max = 30.0/3600;  // 30 arc second represent about 1kms
        double min = 0.0;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
    
    public String PrintVertices () {
        StringBuilder sbVertices = new StringBuilder("");
          // Create an iterator for the list 
        // using iterator() method 
        Iterator<Vertice> iter  = vertices.iterator(); 
  
        // Displaying the values after iterating 
        // through the list 

        while (iter.hasNext()) { 
            sbVertices.append(iter.next().toString());
        }
       
        logger.log(Level.INFO, "result of sbVertices: \n {0}", sbVertices.toString() );
        return sbVertices.toString();
    }
    
    public String PrintSurfaces() {
         StringBuilder sbTriFaces = new StringBuilder("");
                  // Create an iterator for the list 
        // using iterator() method 
        Iterator<TriFace> iter  = triFaces.iterator(); 
  
        // Displaying the values after iterating 
        // through the list +

        while (iter.hasNext()) { 
            sbTriFaces.append(iter.next().toString());
        }
        
        logger.log(Level.INFO, "result of sbFaces: \n {0}", sbTriFaces.toString() );
        return sbTriFaces.toString();
    }
    /*
    a method to create vertices with x,y according to the boundary of ULCorner 
    and LRCorner in separation of pre-defined increments
    the z coordinate comes from srmt, but at the moment just a dummy
 
    */
      public void CreateVertices () {

      
        // work out the noofCols and noofRows based on the boundaries and the 
        // increment step pre-defined
        double xInc = Math.signum(xBoxWidth)*xyIncStep;
        double yInc = Math.signum(yBoxWidth)*xyIncStep;
        nofCols = (int) Math.ceil((xBoxWidth) / xInc) +1; // 7 / (30/3600) + 1
        nofRows = (int) Math.ceil((yBoxWidth) / yInc) +1; // -6 / (-30/3000) + 1
                
        z = new double[nofRows][nofCols];
        // assume data from srmt
             
        // double xPt = xULCorner;
        // double yPt = yULCorner;
        double[] boundary = {xULCorner, yULCorner, xBoxWidth, yBoxWidth};
        // get z from reading dem file, but at the moment use GetZ which generate a 
        // random figure
     //   Dem dem = new Dem(boundary);  
       // z = dem.getZ();
       z = GetZ(nofCols, nofRows).clone(); 
       
        for (int r=0; r<nofRows; r++) {
            
            for (int c = 0; c<nofCols; c++){
           
                Vertice v = new Vertice(r, c, z[r][c]);
                vertices.add(v);
            }
       }
        StringBuilder sb = new StringBuilder("");
        
        for (int r=0; r<nofRows; r++) {
            
            for (int c = 0; c<nofCols; c++){
         
                sb.append("c: ").append(c).append(";");
                sb.append("r: ").append(r).append(";");
                sb.append("z: ").append(z[r][c]).append("\n");
            }   
        }
        
        logger.log(Level.INFO, "result: \n {0}", sb.toString() );
        
}
    public void CreateVerticesOld () {

      
        // work out the noofCols and noofRows based on the boundaries and the 
        // increment step pre-defined
        double xInc = Math.signum(xBoxWidth)*xyIncStep;
        double yInc = Math.signum(yBoxWidth)*xyIncStep;
        nofCols = (int) Math.ceil((xBoxWidth) / xInc) +1; // 7 / (30/3600) + 1
        nofRows = (int) Math.ceil((yBoxWidth) / yInc) +1; // -6 / (-30/3000) + 1
        
        x = new double[nofRows][nofCols];
        y = new double[nofRows][nofCols];
        z = new double[nofRows][nofCols];
        // assume data from srmt
             
        double xPt = xULCorner;
        double yPt = yULCorner;
        for (int r=0; r<nofRows; r++) {
            
            for (int c = 0; c<nofCols; c++){
                x [r][c] = xPt;
                y [r][c] = yPt;
                xPt += xInc;
                xPt = Math.min(xPt, xLRCorner);
                // z [r][c] = zData[r][c];
                z [r][c] = GetZ(xPt,yPt);
                Vertice v = new Vertice(r, c, x[r][c],y[r][c],z[r][c]);
                vertices.add(v);
            }
            yPt += yInc;
            yPt = Math.max(yPt, yLRCorner);
            xPt = xULCorner;
        }
        StringBuilder sb = new StringBuilder("");
        
        for (int r=0; r<nofRows; r++) {
            
            for (int c = 0; c<nofCols; c++){
         
                sb.append("x: ").append(x[r][c]).append(";");
                sb.append("y: ").append(y[r][c]).append(";");
                sb.append("z: ").append(z[r][c]).append("\n");
            }   
        }
        
        logger.log(Level.INFO, "result: \n {0}", sb.toString() );
        
}
 
    public void WriteObjFile() {
        String fileName = "munroproject.obj";
        String title = "#oneTriangle";
        
        StringBuilder sb = new StringBuilder("");
        // write comment
        sb.append(title+"\n");
        
        sb.append(PrintVertices());
        
        // write text for surfaces
        
        sb.append(PrintSurfaces());
        logger.log(Level.INFO, "result of .obj: \n {0}", sb.toString() );
        
        // write to file
        UserProperties pro = new UserProperties();
        String sDir = pro.getProperties("my.dear.home");//eg. username="zub"
        logger.log(Level.INFO, "sDir: {0}", sDir);
        
    
    try {
        
        logger.log(Level.INFO, "Writing sb: {0} \n to file: \n {1}", 
                         new Object[]{sb.toString(),sDir+"\\"+fileName} );
        
        FileOutputStream fos = new FileOutputStream(sDir+"\\"+fileName);
        byte[] bytesArray = sb.toString().getBytes();

	  fos.write(bytesArray);
	  fos.flush();
    
        }   catch (IOException ex) {
            Logger.getLogger(WFObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
             
public static void main(String args[]){
    
    double[] ul = new double[]{-8.010, 59.076};
    double xWidth = 0.8, yWidth = 0.5;
    double[] lr = new double[]{ul[0] + xWidth, ul[1] - yWidth};
    
    WFObjWriter obj = new WFObjWriter(ul, lr);
    
    obj.CreateVertices();
    obj.CreateSurfaces();
    obj.WriteObjFile();
    
    
    
}    
    
}
