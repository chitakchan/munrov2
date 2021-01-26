/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author Think
 * a routne to read data from DEM file and form the data to feed the 
 * for the WFObjWriter via getZ() 
 * 
 * @ref there are free software, as seen in http://vterrain.org/Packages/NonCom/
 * https://freegeographytools.com/2009/3dem-website-is-gone-but-3dem-still-available-here
 * 
 */
public class Dem {
      
    double[][] x, y;
    short[][] z;
    // int nofRows, nofCols;
    
    private static final Logger logger = Logger.getLogger(Dem.class.getName());
    
    // constructure to create a dem class with digital elevation data read from a
    // designated dem file
    String byteOrder, layout;
    int nRows, nCols, nBands, nBits, bandRowBytes, totalRowBytes, bandGapBytes, noData;
    double ulXmap, ulYmap, xDim, yDim;

    String strDemDir, strDemFileName;
    Rectangle2D rectBox = new Rectangle2D.Double();
    Rectangle2D adjRect2DBox = new Rectangle2D.Double();
    Rectangle rectBoxIdx = new Rectangle();
    /*
    *  constructor to get the upper left corner then the width and length of the boundary
    * 1. read parameter settings from header file
     * 2. read dataset from dem file
    * 
    */
    
    // https://stackoverflow.com/questions/581873/best-way-to-handle-multiple-constructors-in-java
      
      
    public Dem (Rectangle2D rectBox){
         
       this (rectBox, 
               new UserProperties().getProperties("Gtopo30.dem.dir"), 
               new UserProperties().getProperties("default.demFileNamePt1"));
    }
    
    
    
    public Dem (Rectangle2D rectBox, String strDemDir, String strDemFileName){
        
        this.rectBox = rectBox;
        
        this.strDemDir = strDemDir;
        this.strDemFileName = strDemFileName;
        readHdr();
        // focus on the box boundary defined in the constructor
        this.rectBoxIdx = new Rectangle (
                (int) ((this.rectBox.getX() - this.ulXmap)/this.xDim),
                (int) ((- this.rectBox.getY() + this.ulYmap)/this.yDim),
                (int) (this.rectBox.getWidth()/this.xDim), 
                (int) (this.rectBox.getHeight()/this.yDim)
            );
        
        // recalculate the Box boundary in lat and long degree.
        this.adjRect2DBox.setRect(
                this.rectBoxIdx.x * this.xDim + this.ulXmap, 
                - this.rectBoxIdx.y * this.yDim + this.ulYmap,
                this.rectBoxIdx.width * this.xDim, this.rectBoxIdx.height * this.yDim
                );
                
            //    = new Rectangle2D (0,0,0,0
        
    }
    
    public short getZ(int c, int r){
        return z[c][r];
    }
    
    /**
     * read header file
     * source:  https://stackoverflow.com/questions/27880245/reading-input-delimited-by-spaces-in-java
     */
    public void toStringHdr(){

        StringBuilder sb = new StringBuilder("");
        sb.append("BYTEORDER ").append(byteOrder).append("\n");
        sb.append("LAYOUT ").append(layout).append("\n");
        sb.append("NROWS ").append(nRows).append("\n");
        sb.append("NCOLS ").append(nCols).append("\n");
        sb.append("NBITS ").append(nBits).append("\n");
        sb.append("BANDROWBYTES ").append(bandRowBytes).append("\n");
        sb.append("TOTALROWBYTES ").append(totalRowBytes).append("\n");
        sb.append("NODATA ").append(noData).append("\n");
        sb.append("ULXMAP ").append(ulXmap).append("\n");
        sb.append("ULYMAP ").append(ulYmap).append("\n");
        sb.append("XDIM ").append(xDim).append("\n");
        sb.append("YDIM ").append(yDim).append("\n");
        
        logger.log(Level.INFO, sb.toString());
        
    }
    
    private void readHdr() {
         Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(strDemDir+"\\"+strDemFileName+".hdr"));
            byteOrder=prop.getProperty("BYTEORDER", "M");
            layout = prop.getProperty("LAYOUT", "BIL"); 
            nRows = Integer.parseInt(prop.getProperty("NROWS")); 
            nCols = Integer.parseInt(prop.getProperty("NCOLS")); 
            nBands = Integer.parseInt(prop.getProperty("NBANDS")); 
            nBits = Integer.parseInt(prop.getProperty("NBITS")); 
            bandRowBytes = Integer.parseInt(prop.getProperty("BANDROWBYTES")); 
            totalRowBytes = Integer.parseInt(prop.getProperty("TOTALROWBYTES")); 
            bandGapBytes = Integer.parseInt(prop.getProperty("BANDROWBYTES")); 
            noData = Integer.parseInt(prop.getProperty("NODATA", "-9999")); // value used for masking  
            ulXmap = Double.parseDouble(prop.getProperty("ULXMAP")); 
            ulYmap = Double.parseDouble(prop.getProperty("ULYMAP")); 
            xDim  = Double.parseDouble(prop.getProperty("XDIM")); 
            yDim  = Double.parseDouble(prop.getProperty("YDIM")); 
            
            // Band interleaved by line.  note that the DEM is a single band image
            
        } catch (IOException ex) {
            Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*
    source: https://docs.oracle.com/javase/7/docs/api/javax/imageio/stream/ImageInputStream.html#setByteOrder(java.nio.ByteOrder)
    
    source: https://stackoverflow.com/questions/29614825/reading-uint16-to-image-java
    
    source: https://www.programcreek.com/java-api-examples/?api=javax.imageio.stream.ImageInputStream
    */
    public void readDem()  {
        FileInputStream fis = null;
        try {
            File file = new File(strDemDir+"\\"+strDemFileName+".dem");        
            //   ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // FileInputStream obtains streams of bytes
            fis = new FileInputStream(file);
//            DataInputStream dis = new DataInputStream(fis);
          //  ImageInputStream dis = new ImageInputStream(fis);
            ImageInputStream in = ImageIO.createImageInputStream(fis);
            
            BufferedImage theImage = 
                    new BufferedImage(nCols, nRows, BufferedImage.TYPE_USHORT_GRAY);
            
            
            DataBuffer db = theImage.getRaster().getDataBuffer();
            
            short[] pixels = ((DataBufferUShort) db).getData();
            in.setByteOrder(ByteOrder.BIG_ENDIAN);
            in.readFully(pixels, 0, nCols * nRows);
            StringBuilder sb = new StringBuilder("");
            for (short p : pixels){
  //              sb.append(p).append(",");
            }
            showImg(theImage);
            //
            
            BufferedImage subBufferImage = 
                    theImage.getSubimage(rectBoxIdx.x, rectBoxIdx.y, rectBoxIdx.width, rectBoxIdx.height);
            
            DataBuffer dbSub = (subBufferImage.getRaster()).getDataBuffer();
            
            // copy a region using .getData(rectBoxIdx)
            // see https://www.tutorialspoint.com/java_dip/understand_image_pixels.htm
            // https://stackoverflow.com/questions/36462710/bufferedimage-extract-subimage-with-same-data
            //
            // Rectangle rectBoxIdx = new Rectangle(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
            // the .getData() method on the subImage would also returns the same data array as the original image,
            // hence the size of array is the same as the parent one.
            // see https://stackoverflow.com/questions/36462710/bufferedimage-extract-subimage-with-same-data
            //
            short[] pixelsSub = ((DataBufferUShort) dbSub).getData();
            sb = new StringBuilder("");
            for (short p : pixelsSub){
//                sb.append(p).append(",");
            }
//            logger.log(Level.INFO, "pixels with length {0}: {1}", new Object[]{pixels.length, sb.toString()} );
            // 

            // create new raster, cast to writable and translate it to 0,0
            WritableRaster subRaster = 
                    ((WritableRaster) theImage.getData(rectBoxIdx)).createWritableTranslatedChild(0,0);
                    // ((WritableRaster) theImage.getData(rectBoxIdx)).createWritableTranslatedChild(xBoxULCols, yBoxULRows);
            
            BufferedImage subOne = new BufferedImage(theImage.getColorModel(), 
                    subRaster, theImage.isAlphaPremultiplied(), null);
            
            short[] pixelsSubOne = ((DataBufferUShort) subOne.getRaster().getDataBuffer()).getData();
            
            showImg(subOne);
            logger.log(Level.INFO, "new subOne has data length of {0}", pixelsSubOne.length);
            
            z = new short[rectBoxIdx.height][rectBoxIdx.width];
            
            for (int i=0; i<rectBoxIdx.height; i++ ) {
                for (int j=0; j<rectBoxIdx.width; j++){
                    z[i][j] = pixelsSubOne[i*rectBoxIdx.width + j];
                }
            }
            logger.log(Level.INFO, "boundary[x {0}][y {1}][width {2}][height {3}]\n", 
                    new Object[]{rectBoxIdx.x,rectBoxIdx.y,rectBoxIdx.width,rectBoxIdx.height});
            logger.log(Level.INFO, "z[i][j] has height [{0}] width [{1}]", new Object[]{z.length, z[0].length});
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }        


    /**
     * a quick method to show the Buffered Image
     * to check whether Buffered Images are read correctly
     * @ref https://stackoverflow.com/questions/1626735/how-can-i-display-a-bufferedimage-in-a-jframe
     * @param img 
     */
    private void showImg(BufferedImage img){
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // use x button to close the app
    }
    
    private void showImg(Raster img){
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
//        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // use x button to close the app
    }
    public static void main(String args[]){
        
        // get directory where source data is    
        UserProperties pop = new UserProperties("munroprojecthk");
        String sDir = pop.getProperties("my.dear.home");
        String FileDir=sDir + "\\Bulk Order Scotlad GMTED2010 30Arc SECAND GTOPO30 Geotiff\\Dem\\GTOPO30";
        String FileNamePt1 = "gt30w020n90";
        // to use Rectangle with double instead of int you need to use Rectangle2D
       // https://stackoverflow.com/questions/2214456/rectangle-and-rectangle2d-difference
       // 
        Rectangle2D rect2DBox = new Rectangle2D.Double(-7.1, 58.8, 5.5, 4.5);
        // Rectangle rectBoxIdx = new Rectangle(-7.1, 58.8, 5.5, 4.5);
        // double[] boxBoundary = {-7.1, 58.8, 5.5, 4.5};
        
       // Dem gt30w020n90Dem = new Dem(boxBoundary, FileDir, FileNamePt1);
       
       Dem gt30w020n90Dem = new Dem(rect2DBox);
        gt30w020n90Dem.toStringHdr();
        gt30w020n90Dem.readDem();
        
        
    }

    
}
