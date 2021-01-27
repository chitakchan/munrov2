/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
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
    double[][] x, y;
    short [][] z;
    double xyIncStep, xULCorner, yULCorner, xLRCorner, yLRCorner; // lat 57, lon -5 around fort william
    double xBoxWidth, yBoxWidth, xOffset, yOffset;

    int nofRows, nofCols;
    ArrayList<Vertice> vertices = new ArrayList<Vertice>();
    ArrayList<TriFace> triFaces = new ArrayList<TriFace>();

    Dem dem;
    Rectangle rectBoxIdx = null;
    Rectangle2D adjRect2DBox;
    public final int seaRepZ;
    // private final double xScale, yScale, zScale;
    private final double measUnit;
    private Properties boxProp;
    private final double eFalseO;
    private final double nFalseO;
    private final double xTrueO;
    private final double yTrueO;
    private final double baseDepth;


    /**
     * this constructor with properties as input allow keep individual region to 
     * provide all necessary parameters in a file 
     * @param boxProp 
     */
    private WFObjWriter(Properties boxProp) {
      
        this.boxProp = boxProp;
                
        Rectangle2D rect2DBox = new Rectangle2D.Double(
        Double.parseDouble(boxProp.getProperty("ULXMAP")),
        Double.parseDouble(boxProp.getProperty("ULYMAP")),
        Double.parseDouble(boxProp.getProperty("WIDTH")),
        Double.parseDouble(boxProp.getProperty("HEIGHT"))
        );

        this.seaRepZ = Integer.parseInt(boxProp.getProperty("SEAREPZ","0"));
        // sea was represented by usgs as -9999.  changed to a new value here.
        //   xyIncStep = 30.0/3600;  // need to use 30.0 otherwise the division would be treated as int and as result xyIncStep become zero.
        // ground distance for 30-arc seconds         
        // get the scale to translate deg to meter;   0.001 refers to km, 1 refers to m.
        this.measUnit = Double.parseDouble(boxProp.getProperty("DISTSCALE","0.001"));
        
        this.xTrueO = Double.parseDouble(boxProp.getProperty("XTRUEO","0.0"));
        this.yTrueO = Double.parseDouble(boxProp.getProperty("YTRUEO","0.0"));
        // offset to falso origin:  eFalseOff, nFalse e' = e + eFalseOff; n' = n + nFalseOff
        this.eFalseO = Double.parseDouble(boxProp.getProperty("EFALSEO","0.0"));
        this.nFalseO = Double.parseDouble(boxProp.getProperty("NFALSEO","0.0"));
        
        // create a dem object and retrieve the relevant idx of the box from its calculation
         
        this.dem = new Dem(rect2DBox, 
                boxProp.getProperty("Gtopo30.dem.dir"), 
                boxProp.getProperty("default.demFileNamePt1")); 
        this.rectBoxIdx = dem.rectBoxIdx;
        this.xyIncStep = dem.xDim;  // should be 30/3600 deg between two idx
        this.adjRect2DBox = dem.adjRect2DBox;
        
        // calculate the depth of the base
        
        this.baseDepth = Double.parseDouble(boxProp.getProperty("BASEDEPTH","0.01")) *
                this.adjRect2DBox.getWidth()*convDegToEN(this.adjRect2DBox.getY())[0];
        
        logger.setLevel(Level.INFO);
 
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
     */
        
  public int[] convDegToEN(double lat){
  
      // this table is the ground distance in meter per 30-arc seconds
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
                  /*
                  Math.round((lookUpTble[i][1] * (1- prorata) + lookUpTble[i-1][1] * prorata)*120),
                  Math.round((lookUpTble[i][2] * (1- prorata) + lookUpTble[i-1][2] * prorata)*120)
                  */
                  Math.round((lookUpTble[i][1] * (1- prorata) + lookUpTble[i-1][1] * prorata)*120),
                  Math.round((lookUpTble[i][2] * (1- prorata) + lookUpTble[i-1][2] * prorata)*120)
              };
              logger.log(Level.FINER, "arrayEN for lat {0} is [{1}, {2}]", new Object[]{lat, arrayEN[0], arrayEN[1]});
            return arrayEN;
       
              }
          }
      
          return null;
      }
      
  /**
   * to return the vertice and its position from the array vertices
   * @param r
   * @param c
   * @return 
   */
    public Vertice getVerticeFromList (int r, int c){
        int position = r * this.rectBoxIdx.width + c; 
        
        return this.vertices.get(position);
        
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
        
    /**
     * convert the 2D index of the position into sequence number in the obj file
     * @param r
     * @param c
     * @return 
     */
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

    private String CreateBaseSurfaces() {
        StringBuilder sb = new StringBuilder("");
        sb.append("f -8 -6 -5 -7").append("\n");
        sb.append("f -4 -3 -1 -2").append("\n");
        sb.append("f -8 -7 -3 -4").append("\n");
        sb.append("f -6 -8 -4 -2").append("\n");
        sb.append("f -5 -6 -2 -1").append("\n");
        sb.append("f -7 -5 -1 -3").append("\n");
        
        return sb.toString();
    }

    /**
     * print 
     * @return 
     */
    private String PrintSideSurfaces() {
        /*
         // public String PrintSurfaces() {
         StringBuilder sbTriFaces = new StringBuilder("");
        // Create an iterator for the list 
        // using iterator() method 
        Iterator<PolFace> iter  = triFaces.iterator(); 
  
        // Displaying the values after iterating 
        // through the list +

        while (iter.hasNext()) { 
            sbTriFaces.append(iter.next().toString());
        }
            logger.log(Level.INFO, "printing surfaces... ");
        logger.log(Level.FINE, "result of sbFaces: \n {0}", sbTriFaces.toString() );
        return sbTriFaces.toString();
   
        */
        return null;
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

           double x2E = convDegToEN(this.y)[0];
           double y2N = convDegToEN(this.y)[1];
           double easting = (this.x - xTrueO) * x2E;
           double northing = (this.y - yTrueO) * y2N;
           double eastingFalse = easting - eFalseO;
           double northingFalse = northing - nFalseO;
           sb.append("v ").append(eastingFalse * measUnit);
           sb.append(" ").append(northingFalse * measUnit);
           sb.append(" ").append(((z == -9999) ? seaRepZ  : z) * measUnit ).append("\n");
           return sb.toString();
        }
   
    }
     /**
     * not used
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
    
    public String PrintVertices (ArrayList<Vertice> vertices) {
        StringBuilder sbVertices = new StringBuilder("");
          // Create an iterator for the list 
        // using iterator() method 
        Iterator<Vertice> iter  = vertices.iterator(); 
  
        // Displaying the values after iterating 
        // through the list 

        while (iter.hasNext()) { 
            // sbVertices.append(iter.next().toString());
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
        // resize z based on the array width and height of z returned from
        // Dem dem;
        z = new short[this.rectBoxIdx.height][this.rectBoxIdx.width];
        // assume data from srmt and copy to the z array;
        dem.readDem();
        this.z=(dem.z).clone();
        
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
      /**
       * to create the vertices for the base plane and show the number only
       * but I think I would try to create a cube like class for this
       * see e.g. http://paulbourke.net/dataformats/obj/
       * 
       */
      public String CreateBaseVertices (Double depth) {
          
          ArrayList<Vertice> vertices = new ArrayList<Vertice>();
          // add four corners, as if it is the map but just four points
          vertices.add(new Vertice(0, 0, depth));
          vertices.add(new Vertice(0,this.rectBoxIdx.width-1, depth));
          vertices.add(new Vertice(this.rectBoxIdx.height-1,0 ,depth));
          vertices.add( new Vertice(rectBoxIdx.height-1, this.rectBoxIdx.width-1, depth));
         
          StringBuilder sbBaseVertices = new StringBuilder("");
              sbBaseVertices.append(PrintVertices(vertices));
         
          logger.log(Level.INFO, "working on printing base vertices.. {0} ", sbBaseVertices.toString());
          
          return sbBaseVertices.toString();
      }
    
      
    public void WriteObjFile() {
        String fileName = this.boxProp.getProperty("TITLE") + ".obj";
        String title = "o " + fileName;
        
        StringBuilder sb = new StringBuilder("");
        // write comment
        sb.append(title+"\n");
        
        sb.append(PrintVertices(this.vertices));
        
        sb.append(CreateBaseVertices(-this.baseDepth));
        // write text for surfaces
        
        sb.append(PrintSurfaces());
        
        // write text for sides surfaces
        
        sb.append(PrintSideFace(0));
        sb.append(PrintSideFace(1));
        sb.append(PrintSideFace(2));
        sb.append(PrintSideFace(3));
        sb.append(PrintBaseSurface());
        
        
        // add the base plane
        /*
        sb.append("o baseplane\n").append(CreateBaseVertices(-this.baseDepth*0.2));
        sb.append(CreateBaseVertices(-this.baseDepth));
        sb.append(CreateBaseSurfaces());
        
        */
        
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

    /**
     * sideFace to generate the four side
     */
    public String PrintSideFace(int cornerPosition) {
            
          ArrayList<Integer> sideVertices = new ArrayList<Integer>();
        // for UL corner(0), c+;  UR(1), r+; LL(2), r-; LR(3), c-
            int r, c;
            int startNo = rectBoxIdx.height*rectBoxIdx.width;
            switch(cornerPosition) {
                case 0:
                    // v = getVerticeFromList(0,0);
                    // add vertices along c+
                    // r = 0; c = 0;   // start point, check by applying i to the bracket
                    for (int i=0; i<rectBoxIdx.width; i++){
                        sideVertices.add( i );
                    }
                    // add two vertices at pt 1 then 0
                        sideVertices.add(startNo + 1);
                        sideVertices.add(startNo + 0);
                        
                    break;
                case 1:
                    // r = 0; c = rectBoxIdx.width;
                     // add vertices along r+
                    for (int i=0; i<rectBoxIdx.height; i++){
                        sideVertices.add(i * rectBoxIdx.width + rectBoxIdx.width-1 );
                    }
                    // add two vertices at pt 2 then 1
                        sideVertices.add(startNo + 3);
                        sideVertices.add(startNo + 1);
                    
                    break;
                    
                case 2:      // LL(2), r-
                    // r = rectBoxIdx.height; c = 0;
                    for (int i=rectBoxIdx.height ; i > 0 ; i--){
                        sideVertices.add( rectBoxIdx.width * (i-1));
                    }
                        sideVertices.add(startNo + 0);
                        sideVertices.add(startNo + 2);
                    break;      
                    
                case 3:      // LR(3), c-
                    // r = rectBoxIdx.height; c = rectBoxIdx.width;
                    for (int i=rectBoxIdx.width; i > 0; i--){
                        sideVertices.add((rectBoxIdx.height - 1) * rectBoxIdx.width + i -1);
                    }
                    // add two vertices at pt 3 then 2
                        sideVertices.add(startNo + 2);
                        sideVertices.add(startNo + 3);
                    break;                    
                                  
            }
        
            StringBuilder sb = new StringBuilder("");
         
            Iterator<Integer> iter = sideVertices.iterator();
  
            // Displaying the values after iterating 
            // through the list +
            sb.append("f ");
            while (iter.hasNext()) { 
                sb.append(iter.next().intValue()+1).append(" ");
            }
            sb.append("\n");
            return sb.toString();
    }
    
    /**
     * print the surface for the base which vertices would be the last
     * 4 vertices in c then r direction
     */
    public String PrintBaseSurface() {
        int startNo = rectBoxIdx.height*rectBoxIdx.width;
          
        StringBuilder sb = new StringBuilder("");
        sb.append("f ").append(startNo +1);
        sb.append(" ").append(startNo + 2);
        sb.append(" ").append(startNo + 4);
        sb.append(" ").append(startNo + 3);
        sb.append("\n");
        return sb.toString();
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
         String boxName = "guangdong";
         UserProperties prop = new UserProperties();
         
         Properties boxProp = new Properties();
        try {
            boxProp.load(new FileInputStream(prop.getProperties("my.dear.home")+"\\"+boxName+".hdr"));
            
        } catch (IOException ex) {
            Logger.getLogger(WFObjWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    
      
        WFObjWriter obj = new WFObjWriter(boxProp);
        obj.CreateVertices();
        obj.CreateSurfaces();
        obj.WriteObjFile();
    }   
    
}
