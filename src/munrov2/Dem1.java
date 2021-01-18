/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package munrov2;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
 * 
 */
public class Dem1 {
      
    double[][] x, y;
    short[][] z;
    int nofRows, nofCols;
    
    private static final Logger logger = Logger.getLogger(Dem1.class.getName());
    
    // constructure to create a dem class with digital elevation data read from a
    // designated dem file
    String byteOrder, layout;
    int nRows, nCols, nBands, nBits, bandRowBytes, totalRowBytes, bandGapBytes, noData;
    double ulXmap, ulYmap, xDim, yDim;
    double ulXBox, ulYBox, xBoxWidth, yBoxWidth;
    String strDemDir, strDemFileName;
    
    /*
    *  constructor to get the upper left corner then the width and length of the boundary
    * 1. read parameter settings from header file
     * 2. read dataset from dem file
    * 
    */
    
    // https://stackoverflow.com/questions/581873/best-way-to-handle-multiple-constructors-in-java
    /*
    
    public Dem (Rectangle rect){
               this ({rect.x, rect.y, rect.width, rect.height}, 
               new UserProperties().getProperties("Gtopo30.dem.dir"), 
               new UserProperties().getProperties("default.demFileNamePt1"));
    }
    
    */
    
    public Dem1 (Rectangle rect, String strDemDir, String strDemFileName){
        
        ulXBox =rect.x;
        ulYBox = rect.y;
        xBoxWidth = rect.width;
        yBoxWidth = rect.height;
        this.strDemDir = strDemDir;
        this.strDemFileName = strDemFileName;
        readHdr();
     //   readDem();
        
    }
    
    public Dem1 (double[] boundary){
         
       this (boundary, 
               new UserProperties().getProperties("Gtopo30.dem.dir"), 
               new UserProperties().getProperties("default.demFileNamePt1"));
    }
    
    
    
    public Dem1 (double[] boundary, String strDemDir, String strDemFileName){
        
        ulXBox = boundary[0];
        ulYBox = boundary[1];
        xBoxWidth = boundary[2];
        yBoxWidth = boundary[3];
        this.strDemDir = strDemDir;
        this.strDemFileName = strDemFileName;
        readHdr();
     //   readDem();
        
    }
 /*
    public short[][] getZ(){
        return z;
    }
    
    */   
    
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
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*
    source: https://docs.oracle.com/javase/7/docs/api/javax/imageio/stream/ImageInputStream.html#setByteOrder(java.nio.ByteOrder)
    
    source: https://stackoverflow.com/questions/29614825/reading-uint16-to-image-java
    
    source: https://www.programcreek.com/java-api-examples/?api=javax.imageio.stream.ImageInputStream
    */
    public void readDemV2()  {
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
            
            
//            logger.log(Level.INFO, "pixels with length {0}: {1}", new Object[]{pixels.length, sb.subSequence(0, 1000).toString()} );
            showImg(theImage);
            //
            // focus on the box boundary defined in the constructor
            int xBoxULCols = (int) ((this.ulXBox - this.ulXmap)/this.xDim);    
            int yBoxULRows = (int) ((- this.ulYBox + this.ulYmap)/this.yDim);    
            int xBoxWidthCols = (int) (this.xBoxWidth/this.xDim); 
            int yBoxWidthRows = (int) (this.yBoxWidth/this.yDim); 
            
            BufferedImage subBufferImage = 
                    theImage.getSubimage(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
            
            DataBuffer dbSub = (subBufferImage.getRaster()).getDataBuffer();
            
            // copy a region using .getData(rect)
            // see https://www.tutorialspoint.com/java_dip/understand_image_pixels.htm
            // https://stackoverflow.com/questions/36462710/bufferedimage-extract-subimage-with-same-data
            //
            Rectangle rect = new Rectangle(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
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
                    ((WritableRaster) theImage.getData(rect)).createWritableTranslatedChild(0,0);
                    // ((WritableRaster) theImage.getData(rect)).createWritableTranslatedChild(xBoxULCols, yBoxULRows);
            
            BufferedImage subOne = new BufferedImage(theImage.getColorModel(), 
                    subRaster, theImage.isAlphaPremultiplied(), null);
            
            short[] pixelsSubOne = ((DataBufferUShort) subOne.getRaster().getDataBuffer()).getData();
            
            showImg(subOne);
            logger.log(Level.INFO, "new subOne has data length of {0}", pixelsSubOne.length);
            
            z = new short[yBoxWidthRows][xBoxWidthCols];
            
            for (int i=0; i<yBoxWidthRows; i++ ) {
                for (int j=0; j<xBoxWidthCols; j++){
                    z[i][j] = pixelsSubOne[i*xBoxWidthCols + j];
                }
            }
            logger.log(Level.INFO, "boundary[x {0}][y {1}][width {2}][height {3}]\n", 
                    new Object[]{rect.x,rect.y,rect.width,rect.height});
            logger.log(Level.INFO, "z[i][j] has height [{0}] width [{1}]", new Object[]{z.length, z[0].length});
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }        


    
    private void readDemV1a() {
        File file = new File(strDemDir+"\\"+strDemFileName+".dem");
     //   ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // FileInputStream obtains streams of bytes
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            BufferedImage theImage = 
                    new BufferedImage(nCols, nRows, BufferedImage.TYPE_USHORT_GRAY);



            
            showImg(theImage);        // checking            
            // focus on the box boundary defined in the constructor
            int xBoxULCols = (int) ((this.ulXBox - this.ulXmap)/this.xDim);    
            int yBoxULRows = (int) ((- this.ulYBox + this.ulYmap)/this.yDim);    
            int xBoxWidthCols = (int) (this.xBoxWidth/this.xDim); 
            int yBoxWidthRows = (int) (this.yBoxWidth/this.yDim); 
            
            // copy a region using .getData(rect)
            // see https://www.tutorialspoint.com/java_dip/understand_image_pixels.htm
            Rectangle rect = new Rectangle(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);



            
            BufferedImage subBufferImage = 
                    theImage.getSubimage(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
            showImg(subBufferImage);        // checking
            short[] subBufferImageData = 
                    ((DataBufferUShort) subBufferImage.getRaster().getDataBuffer()).getData();
            
            short[] pixels = ((DataBufferUShort) 
                    theImage.getSubimage(xBoxULCols, yBoxULRows, xBoxWidthCols, 
                            yBoxWidthRows).getRaster().getDataBuffer()).getData();
            
            StringBuilder sbSubImage = new StringBuilder("");
            
            // load elevation data to z[][]
            short[][] z = new short[xBoxWidthCols][yBoxWidthRows];
            
            // read all the subbox data from file
            
                 outerloop:
            for (int i=0; i < pixels.length; i++) {
             
            //    for (int j=0; j< 1000; j++){
                    pixels[i] = dis.readShort();
                    if (i>1000) break;
             //   }
             
            }
            
            
            
            for (int i=0; i<yBoxWidthRows; i++){
                // dont worry about cumulating memory use for creating new instance of sbOneRow, 
                // the GC garbage collect will handle the job
                StringBuilder sbOneRow = new StringBuilder("");     
                sbOneRow.append(i).append(": ");
                // System.out.println("i: "+i + "\n");
                for (int j=0; j<xBoxWidthCols; j++){
            //        z[i][j]=subBufferImageData[i*yBoxWidthRows+j];
                          
                          // z[i][j]=dis.readShort();
                          z[i][j]=pixels[i*yBoxWidthRows+j];
                    sbOneRow.append(z[i][j]).append(',');
                    // System.out.println(z[i][j] + ",");
                }
                sbSubImage.append(sbOneRow.toString()).append('\n');
                logger.log(Level.INFO, "i={0}: {1}", new Object[]{i, sbOneRow.toString()});
            }
            /*
            for (int i=0; i<subBufferImageData.length; i++){
                sbSubImage.append(subBufferImageData[i]).append(";");
                
            }
            */
            StringBuilder sbBoxParameters = new StringBuilder("");
            sbBoxParameters.append("xBoxULCols: ").append(xBoxULCols).append("\n");
            sbBoxParameters.append("yBoxULRows: ").append(yBoxULRows).append("\n");
            sbBoxParameters.append("xBoxWidthCols: ").append(xBoxWidthCols).append("\n");
            sbBoxParameters.append("yBoxWidthRows: ").append(yBoxWidthRows).append("\n");
            sbBoxParameters.append("ulXBox: ").append(ulXBox).append("\n");
            sbBoxParameters.append("ulYBox: ").append(ulYBox).append("\n");
            sbBoxParameters.append("xBoxWidth: ").append(xBoxWidth).append("\n");
            sbBoxParameters.append("yBoxWidth: ").append(yBoxWidth).append("\n");
            sbBoxParameters.append("xDim: ").append(xDim).append("\n");
            sbBoxParameters.append("yDim: ").append(yDim).append("\n");
            logger.log(Level.INFO, "subImage Parameters: \n {0}\n", sbBoxParameters.toString());
            
            
            } catch (IOException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
private void readDemV1() {
        File file = new File(strDemDir+"\\"+strDemFileName+".dem");
     //   ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // FileInputStream obtains streams of bytes
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            BufferedImage theImage = 
                    new BufferedImage(nCols, nRows, BufferedImage.TYPE_USHORT_GRAY);


            
            
            showImg(theImage);        // checking            
            // focus on the box boundary defined in the constructor
            int xBoxULCols = (int) ((this.ulXBox - this.ulXmap)/this.xDim);    
            int yBoxULRows = (int) ((- this.ulYBox + this.ulYmap)/this.yDim);    
            int xBoxWidthCols = (int) (this.xBoxWidth/this.xDim); 
            int yBoxWidthRows = (int) (this.yBoxWidth/this.yDim); 
            
            // copy a region using .getData(rect)
            // see https://www.tutorialspoint.com/java_dip/understand_image_pixels.htm
            Rectangle rect = new Rectangle(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
            
            BufferedImage subBufferImage = 
                    theImage.getSubimage(xBoxULCols, yBoxULRows, xBoxWidthCols, yBoxWidthRows);
            showImg(subBufferImage);        // checking
            short[] subBufferImageData = 
                    ((DataBufferUShort) subBufferImage.getRaster().getDataBuffer()).getData();
            
            short[] pixels = ((DataBufferUShort) 
                    theImage.getSubimage(xBoxULCols, yBoxULRows, xBoxWidthCols, 
                            yBoxWidthRows).getRaster().getDataBuffer()).getData();
            
            StringBuilder sbSubImage = new StringBuilder("");
            
            // load elevation data to z[][]
            short[][] z = new short[xBoxWidthCols][yBoxWidthRows];
            
            // read all the subbox data from file
            
                 outerloop:
            for (int i=0; i < pixels.length; i++) {
             
            //    for (int j=0; j< 1000; j++){
                    pixels[i] = dis.readShort();
                    if (i>1000) break;
             //   }
             
            }
            
            
            
            for (int i=0; i<yBoxWidthRows; i++){
                // dont worry about cumulating memory use for creating new instance of sbOneRow, 
                // the GC garbage collect will handle the job
                StringBuilder sbOneRow = new StringBuilder("");     
                sbOneRow.append(i).append(": ");
                // System.out.println("i: "+i + "\n");
                for (int j=0; j<xBoxWidthCols; j++){
            //        z[i][j]=subBufferImageData[i*yBoxWidthRows+j];
                          
                          // z[i][j]=dis.readShort();
                          z[i][j]=pixels[i*yBoxWidthRows+j];
                    sbOneRow.append(z[i][j]).append(',');
                    // System.out.println(z[i][j] + ",");
                }
                sbSubImage.append(sbOneRow.toString()).append('\n');
                logger.log(Level.INFO, "i={0}: {1}", new Object[]{i, sbOneRow.toString()});
            }
            /*
            for (int i=0; i<subBufferImageData.length; i++){
                sbSubImage.append(subBufferImageData[i]).append(";");
                
            }
            */
            StringBuilder sbBoxParameters = new StringBuilder("");
            sbBoxParameters.append("xBoxULCols: ").append(xBoxULCols).append("\n");
            sbBoxParameters.append("yBoxULRows: ").append(yBoxULRows).append("\n");
            sbBoxParameters.append("xBoxWidthCols: ").append(xBoxWidthCols).append("\n");
            sbBoxParameters.append("yBoxWidthRows: ").append(yBoxWidthRows).append("\n");
            sbBoxParameters.append("ulXBox: ").append(ulXBox).append("\n");
            sbBoxParameters.append("ulYBox: ").append(ulYBox).append("\n");
            sbBoxParameters.append("xBoxWidth: ").append(xBoxWidth).append("\n");
            sbBoxParameters.append("yBoxWidth: ").append(yBoxWidth).append("\n");
            sbBoxParameters.append("xDim: ").append(xDim).append("\n");
            sbBoxParameters.append("yDim: ").append(yDim).append("\n");
            logger.log(Level.INFO, "subImage Parameters: \n {0}\n", sbBoxParameters.toString());
            /*
            logger.log(Level.INFO, "SubImage parameters:\nxBoxULCols {0} \nyBoxULRows {1} \nxBoxWidthCols {2} \nyBoxWidthRows {3} \nulXBox {4} \nulYBox {5} \nxBoxWidth {6} \nyBoxWidth {7) \n", 
                    new Object[]{xBoxULCols, yBoxULRows, xBoxWidthCols, 
                        yBoxWidthRows, ulXBox, ulYBox, xBoxWidth, yBoxWidth});
            
            */
            
            
    //        logger.log(Level.INFO, "sbSubImage:\n{0}", sbSubImage.toString());

                
            // StringBuilder sb = new StringBuilder("");
            /*
                  ArrayList<StringBuilder> arraySb = new ArrayList<StringBuilder>();
            outerloop:
            for (int i=0; i < pixels.length; i++) {
                StringBuilder sb = new StringBuilder("");
                for (int j=0; j< 1000; j++){
                    pixels[i] = dis.readShort();
                    sb.append(pixels[i]).append(";");
                    
                
                // if (i > 1000) {break outerloop;}
                }
                arraySb.add(sb);
            }

          logger.log(Level.INFO, "no of pixels: {0} \n", 
                        pixels.length);
          
          for (int i =0; i<arraySb.size(); i++){
            logger.log(Level.INFO, "i={0} \n arraySb.get(i): {1}\n", 
                        new Object[]{i, arraySb.get(i).toString()});
            
          }
          
            */
            
      
            
            } catch (IOException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readDemByByte(String strDemDir, String strDemFileName) {
        File file = new File(strDemDir+"\\"+strDemFileName+".dem");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            // FileInputStream obtains streams of bytes
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[1024];
            int count = 0;
            outerloop:
            for (int readNum; (readNum = fis.read(buf))!=-1;){
                bos.write(buf, 0, readNum);
                StringBuilder sb = new StringBuilder("");
                for (int i=0; i<buf.length; i++){
                    
                    sb.append(buf[i]).append(";");
                }
                logger.log(Level.INFO, "readNum: {0} \n input: {1}\n", 
                        new Object[]{readNum, sb});
                count += 1;
                if (count > 100) {break outerloop;}
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Dem1.class.getName()).log(Level.SEVERE, null, ex);
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
        UserProperties pop = new UserProperties();
        String sDir = pop.getProperties("my.dear.home");
        String FileDir=sDir + "\\Bulk Order Scotlad GMTED2010 30Arc SECAND GTOPO30 Geotiff\\Dem\\GTOPO30";
        String FileNamePt1 = "gt30w020n90";
        
        double[] boxBoundary = {-7.1, 58.8, 5.5, 4.5};
        
       // Dem gt30w020n90Dem = new Dem(boxBoundary, FileDir, FileNamePt1);
       
       Dem1 gt30w020n90Dem = new Dem1(boxBoundary);
        gt30w020n90Dem.toStringHdr();
        gt30w020n90Dem.readDemV2();
        
        
    }

    
}
