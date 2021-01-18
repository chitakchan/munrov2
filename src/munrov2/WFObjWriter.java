/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
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

 */
public class WFObjWriter {
    private static final Logger logger = Logger.
                getLogger(WFObjWriter.class.getName());
    

        double[][] x, y;
        
        short [][] z;
          
        double xyIncStep, xULCorner, yULCorner, xLRCorner, yLRCorner; // lat 57, lon -5 around fort william
        double xBoxWidth, yBoxWidth;
        
        int nofRows, nofCols;
        ArrayList<Vertice> vertices = new ArrayList<Vertice>();
        ArrayList<TriFace> triFaces = new ArrayList<TriFace>();
        
        Dem gt30w020n90Dem;
        Rectangle rectBoxIdx = null;
        Rectangle2D adjRect2DBox;
        public final int seaRepZ;
    private final double xScale;
    private final double yScale;

        public WFObjWriter (Rectangle2D rect2DBox) {
     
        // create a dem object and retrieve the relevant idx of the box from its calculation
        
        this.gt30w020n90Dem = new Dem(rect2DBox); 
        this.rectBoxIdx = gt30w020n90Dem.rectBoxIdx;
        this.xyIncStep = gt30w020n90Dem.xDim;
        this.adjRect2DBox = gt30w020n90Dem.adjRect2DBox;
        logger.setLevel(Level.INFO);
        this.seaRepZ = -2;   // sea was represented by usgs as -9999.  changed to a new value here.
        //   xyIncStep = 30.0/3600;  // need to use 30.0 otherwise the division would be treated as int and as result xyIncStep become zero.
                //    e/w  n/s
                // 50 598 927
                // 60 465 929
                // get the scale to translate deg to meter
                this.xScale = (598+465)/2;
                this.yScale = (927+929)/2;
        
        
        }        
 
        /*
        Not used, replaced by calling Dem class and adjust the rectangle
        */
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
        
        
    public int RC2Vno (int r, int c) {
        
        return rectBoxIdx.width * r + c;
    }
    
    public void CreateSurfaces () {
        
        // generate faces from the whole panel
        int r, c;
            for (int i = 0; i<vertices.size(); i++){
                r = vertices.get(i).r;
                c = vertices.get(i).c;
               
                if (c < rectBoxIdx.width-1 & r < rectBoxIdx.height-1)  {
                        TriFace tri = new TriFace(vertices.get(i));
                        triFaces.add(tri);
                }
            }
    }
    
    /**
     * 
    create triangle polygon surface,
    
    */
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
    /**
     * Vertice class 
     */
    public class Vertice {
        double x, y, z;
        int r, c;
        
        public Vertice(int r, int c, double z){
            this.r = r;
            this.c = c;
            this.x = adjRect2DBox.getX()  + c*xyIncStep;
            this.y = adjRect2DBox.getY() - r*xyIncStep;
            
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
        
                sb.append("v ").append(x * xScale);
                sb.append(" ").append(y * yScale);
                sb.append(" ").append(((z == -9999) ? seaRepZ : z) ).append("\n");
                return sb.toString();
        }
    }
    
    /**
     * not used
     * @param noOfCols
     * @param noOfRows
     * @return 
     */
    
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
    
    
    /**
     * not used
     * @param x
     * @param y
     * @return 
     */
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
           logger.log(Level.INFO, "printing vertices... ");
        logger.log(Level.FINE, "result of sbVertices: \n {0}", sbVertices.toString() );
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
            logger.log(Level.INFO, "printing surfaces... ");
        logger.log(Level.FINE, "result of sbFaces: \n {0}", sbTriFaces.toString() );
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
        
                
        z = new short[this.rectBoxIdx.height][this.rectBoxIdx.width];
        // assume data from srmt

        gt30w020n90Dem.readDem();
        this.z=(gt30w020n90Dem.z).clone();
        
        /*
        // try change the sea value -9999 to -2 but this don't work
        // the array doesn't change
        for (short[] s: this.z){
            for (short t: s){
                if (t == -9999){
                    t = -2; 
                } 
            }
        }
        */
        
       
       
        for (int r=0; r<this.rectBoxIdx.height; r++) {
            
            for (int c = 0; c<this.rectBoxIdx.width; c++){
                    Vertice v = new Vertice(r, c, z[r][c]);
       //         Vertice v = new Vertice(r, c, ((z[r][c] == -9999) ? this.seaRepZ : z[r][c]));
                vertices.add(v);
            }
       }
        StringBuilder sb = new StringBuilder("");
        
        for (int r=0; r<this.rectBoxIdx.height; r++) {
            
            for (int c = 0; c<this.rectBoxIdx.width; c++){
         
                sb.append("c: ").append(c).append(";");
                sb.append("r: ").append(r).append(";");
                sb.append("z: ").append(z[r][c]).append("\n");
            }   
        }
        
        logger.log(Level.INFO, "creating vertices... ");
        logger.log(Level.FINE, "result: \n {0}", sb.toString() );
        
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
        
        logger.log(Level.FINE, "result of .obj: \n {0}", sb.toString() );
        
        // write to file
        UserProperties pro = new UserProperties();
        String sDir = pro.getProperties("my.dear.home");//eg. username="zub"
        logger.log(Level.INFO, "sDir: {0}", sDir);
        
    
    try {

        logger.log(Level.INFO, "Writing to file: {0}....", 
                         sDir+"\\"+fileName);
        
        logger.log(Level.FINE, "Writing sb: {0} \n total length: \n {1}", 
                         new Object[]{sb.toString(),sb.toString().length()} );
        
        FileOutputStream fos = new FileOutputStream(sDir+"\\"+fileName);
        byte[] bytesArray = sb.toString().getBytes();

	  fos.write(bytesArray);
	  fos.flush();
    
        }   catch (IOException ex) {
            Logger.getLogger(WFObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
             
public static void main(String args[]){

     Rectangle2D rect2DBox = new Rectangle2D.Double(-7.1, 58.8, 5.5, 4.5);
  //  Rectangle2D rect2DBox = new Rectangle2D.Double(-3.015, 56.467, 0.5, 0.5);
    WFObjWriter obj = new WFObjWriter(rect2DBox);
    
    obj.CreateVertices();
    obj.CreateSurfaces();
    obj.WriteObjFile();
    
    
    
}    
    
}
