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
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingConstants;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.examples.ImageReadExample.ManagedImageBufferedImageFactory;
import org.apache.commons.imaging.formats.tiff.TiffContents;
import org.apache.commons.imaging.formats.tiff.TiffDirectory;
import org.apache.commons.imaging.formats.tiff.TiffElement.DataElement;
import org.apache.commons.imaging.formats.tiff.TiffImageData;
import org.apache.commons.imaging.formats.tiff.TiffReader;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import tiffUtility.MetadataExample;

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
    private int nRows, nCols, nBands, nBits, bandRowBytes, totalRowBytes, bandGapBytes, noData;
    double ulXmap, ulYmap, xDim, yDim;

    String strDemDir, strDemFileName;
    Rectangle2D rectBox = new Rectangle2D.Double();
    public Rectangle2D adjRect2DBox = new Rectangle2D.Double();
    Rectangle rectBoxIdx = new Rectangle();
    
    File file;
    /*
    *  constructor to get the upper left corner then the width and length of the boundary
    * 1. read parameter settings from header file
     * 2. read dataset from dem file
    * 
    */
    
    // https://stackoverflow.com/questions/581873/best-way-to-handle-multiple-constructors-in-java
          
      
    public Dem (Rectangle2D rectBox) {
         
       this (rectBox, 
               new UserProperties().getProperties("Gtopo30.dem.dir"), 
               new UserProperties().getProperties("default.demFileNamePt1"), 0);
                
               
    }
    
    
    
    public Dem (Rectangle2D rectBox, Integer lrExt) {
         
       this (rectBox, 
               new UserProperties().getProperties("Gtopo30.dem.dir"), 
               new UserProperties().getProperties("default.demFileNamePt1"), lrExt);
                
               
    }
    
     public Dem (Rectangle2D rectBox, String strDemDir, String strDemFileName) {
       this (rectBox, strDemDir, strDemFileName, 0);
                
         
     }
    
     public Dem (Rectangle2D rectBox, String strDemDir, String strDemFileName, Integer lrExt) {
         this (rectBox, strDemDir, strDemFileName, 0, 0);
     }
     
    public Dem (Rectangle2D rectBox, String strDemDir, String strDemFileName,
            Integer lrExt, Integer mapFileType) {
        
        this.rectBox = rectBox;
        
        this.strDemDir = strDemDir;
        this.strDemFileName = strDemFileName;
        
        if (mapFileType == 0) {
            this.file = new File(strDemDir+"\\"+strDemFileName+".dem");     
        } else if (mapFileType == 1) {
            this.file = new File(strDemDir+"\\"+strDemFileName+".tif");     
        }
        
        
        readHdr(mapFileType);
       
        
        // adjust the width and height if the upper left corner of the target map is //
        // to the left and north of the upper left corner of the tile
        
        double xShift, yShift;
        xShift = Math.min(this.rectBox.getX() - this.ulXmap, 0 ); 
        yShift = Math.min(this.ulYmap - this.rectBox.getY(), 0 ); 
        
        
        
        // redefine the ul and lr corner to match the matrix points as 
        // contained in the goto 30sec map
        // it ensures the original ul and lr corner are covered.
        
        int ulIdX, ulIdY, lrIdX, lrIdY, idW, idH;
        // the ul corner should be tended leftward and upward towards the map's UL corner, hence floor
        ulIdX = (int) Math.max(0, Math.floor((this.rectBox.getX() - this.ulXmap)/this.xDim));
        ulIdY = (int) Math.max(0, Math.floor((this.ulYmap - this.rectBox.getY())/this.yDim));
        // conversely the lr corner should be tended rightward and downward away from the maps's UL corner, hence ceiling
        
        // attempt to seam the tile with the other one
        
        // idW = (int) Math.round(this.rectBox.getWidth() / this.xDim) +(lrExt == 1? 1: 0);
        // idH = (int) Math.round(this.rectBox.getHeight() / this.yDim) +(lrExt == 1? 1: 0);
       
        idW = (int) Math.round((this.rectBox.getWidth() + xShift)/ this.xDim) +(lrExt == 1? 1: 0);
        idH = (int) Math.round((this.rectBox.getHeight() + yShift) / this.yDim) +(lrExt == 1? 1: 0);
       
        
        // but idW and idH must be within the bound of the width and height of the map
        
   //     idW = Math.min(idW, this.nCols-ulIdX);
   //     idH = Math.min(idH, this.nRows-ulIdY);
         idW = Math.min(idW, this.nCols-ulIdX);
        idH = Math.min(idH, this.nRows-ulIdY);
      
   
//
        lrIdX = ulIdX + idW;
        lrIdY = ulIdY + idH;
        
        this.rectBoxIdx = new Rectangle (
            ulIdX, ulIdY,idW, idH
                
        );
        
        this.adjRect2DBox.setRect(
            this.ulXmap+ulIdX*this.xDim, 
                this.ulYmap-ulIdY*this.yDim, 
                idW*this.xDim, 
                idH*this.yDim
        );
        
        
        StringBuilder sb = new StringBuilder("");
        
        sb.append("rectangle of map:\n");
        sb.append("(").append(this.ulXmap).append(", ").append(this.ulYmap).append(", ")
                .append(this.nCols).append(", ").append(this.nRows).append(")");
        
        sb.append("\nrectangle of rectBox input:\n");
        sb.append("(").append(this.rectBox.getX()).append(", ").append(this.rectBox.getY()).append(", ")
                .append(this.rectBox.getWidth()).append(", ").append(this.rectBox.getHeight()).append(")");
        
        sb.append("\nlower right corner of rectBox input in degree:\n");
        sb.append("(").append(this.rectBox.getX() + this.rectBox.getWidth())
                .append(", ").append(this.rectBox.getY()-this.rectBox.getHeight())
                .append(")");
        
        sb.append("\nrectangle of rectBox adjusted in degree:\n");
        sb.append("(").append(this.adjRect2DBox.getX()).append(", ").append(this.adjRect2DBox.getY()).append(", ")
                .append(this.adjRect2DBox.getWidth()).append(", ").append(this.adjRect2DBox.getHeight()).append(")");
        
        sb.append("\nlower right corner of rectBox adjusted in degree:\n");
        sb.append("(").append(this.adjRect2DBox.getX() + this.adjRect2DBox.getWidth())
                .append(", ").append(this.adjRect2DBox.getY()-this.adjRect2DBox.getHeight())
                .append(")");
        
        sb.append("\nrectangle of rectBox adjusted in idx:\n");
        sb.append("(").append(this.rectBoxIdx.x).append(", ").append(this.rectBoxIdx.y).append(", ")
                .append(this.rectBoxIdx.width).append(", ").append(this.rectBoxIdx.height).append(")");
        
        logger.log(Level.INFO, sb.toString());
        
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
    
    private void readHdr(Integer mapFileType)  {
        
        if (mapFileType == 0){
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
        } else if (mapFileType == 1) {
   
                try {
                    
                File file = new File(strDemDir+"\\"+strDemFileName+".tif");
                final ImageMetadata metadata = Imaging.getMetadata(file);
                // List<ImageMetadataItem> mdList = new ArrayList<ImageMetadataItem>();

                Properties propMetadata = new Properties();
                propMetadata.load(new StringReader(metadata.toString()));


                String[] s = null;
                // get image width and length
                this.nCols = Integer.parseInt(propMetadata.getProperty("ImageWidth"));
                this.nRows = Integer.parseInt(propMetadata.getProperty("ImageLength"));
                Integer bitsPerSample = Integer.parseInt(propMetadata.getProperty("BitsPerSample"));
                Integer compression = Integer.parseInt(propMetadata.getProperty("Compression"));
                Integer photometricInterpretation = Integer.parseInt(propMetadata.getProperty("PhotometricInterpretation"));
                Integer samplesPerPixel = Integer.parseInt(propMetadata.getProperty("SamplesPerPixel"));
                Integer planarConfiguration = Integer.parseInt(propMetadata.getProperty("PlanarConfiguration"));
                Integer sampleFormat = Integer.parseInt(propMetadata.getProperty("SampleFormat"));
                String geoAsciiParamsTag = propMetadata.getProperty("GeoAsciiParamsTag");
                String gDALNoData = propMetadata.getProperty("GDALNoData");


                // get modelPixelScaleTag

                ArrayList<Double> modelPixelScaleTagList = new ArrayList<>();
                Arrays.asList(propMetadata.getProperty("ModelPixelScaleTag").split(",")).forEach((e) ->{
                    modelPixelScaleTagList.add(Double.parseDouble(e));

                });

                this.xDim = modelPixelScaleTagList.get(0);
                this.yDim = modelPixelScaleTagList.get(1);

                // get modelTiePointTag
                ArrayList<Double> modelTiepointTagList = new ArrayList<>();
                Arrays.asList(propMetadata.getProperty("ModelTiepointTag").split(",")).forEach((e) ->{
                    modelTiepointTagList.add(Double.parseDouble(e));

                });
                this.ulXmap = modelTiepointTagList.get(3);
                this.ulYmap = modelTiepointTagList.get(4);

                // print out
                StringBuilder sbProp = new StringBuilder("\nImageMetadata read as properties:\n");
                sbProp.append("width: "+this.nCols + " length: "+ this.nRows + "\n");
                sbProp.append("xResolation: " + this.xDim + ", yResolation: " + this.yDim + "\n");
                sbProp.append("xMap: " + this.ulXmap + ", yMap: " + this.ulYmap + "\n");
                sbProp.append("\nmetadata:\n"+metadata);
                
                logger.log(Level.INFO, sbProp.toString());
                
                
            } catch (ImageReadException ex) {
                Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
            }


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
            // File file = new File(strDemDir+"\\"+strDemFileName+".dem");        
            //   ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // FileInputStream obtains streams of bytes
            fis = new FileInputStream(this.file);
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
       
       Dem gt30w020n90Dem;
       gt30w020n90Dem = new Dem(rect2DBox);
       gt30w020n90Dem.toStringHdr();
       gt30w020n90Dem.readDem();

        
        
    }

    
    /*
    read from dmted2010 source in tiff format
    */
    void readTiff() {
        
        
        try {
            
            
            TiffReader tiffReader = new TiffReader(true);
            ByteSourceFile byteSource = new ByteSourceFile(this.file);
            
            // read the directories in the tiff file,
            // may include image and metadata
            
            TiffContents contents = tiffReader.readDirectories(
                    byteSource,
                    true,
                    FormatCompliance.getDefault());
            
            // read the first image File Directory (IFD)
            // as the first IFD is the full resolution image plus its meta data
            // the other directories may be of lessor resolution
            // but for GMTED2010 there should be just one directory
            // and without raster dataset
            
            TiffDirectory firstDir = contents.directories.get(0);
            if (firstDir.hasTiffImageData()){
                logger.log(Level.INFO, "first directory got TiffImageData in the file: {0}", file.toString());
                
                
                TiffImageData tiffImageData = firstDir.getTiffImageData();
                DataElement[] de = tiffImageData.getImageData();
                
                this.z = new short[rectBoxIdx.height][rectBoxIdx.width];
                this.z = ExtractFromDataElement(de, rectBoxIdx).clone();
                
                /*
                final Map<String, Object> params = new HashMap<>();
                // try sub image on the tiff data, see if they work
                // http://commons.apache.org/proper/commons-imaging/apidocs/org/apache/commons/imaging/formats/tiff/TiffDirectory.html
                // BufferedImage subBufferImage = theImage.getSubimage(rectBoxIdx.x, rectBoxIdx.y, rectBoxIdx.width, rectBoxIdx.height);
                params.put(TiffConstants.PARAM_KEY_SUBIMAGE_X, rectBoxIdx.x);
                params.put(TiffConstants.PARAM_KEY_SUBIMAGE_Y, rectBoxIdx.y);
                params.put(TiffConstants.PARAM_KEY_SUBIMAGE_WIDTH, rectBoxIdx.width);
                params.put(TiffConstants.PARAM_KEY_SUBIMAGE_HEIGHT, rectBoxIdx.height);
                
                

                BufferedImage subBufferedImage = firstDir.getTiffImage(params);
                subBufferedImage.getData();
//

            WritableRaster subRaster =
                ((WritableRaster) subBufferedImage.getData()).createWritableTranslatedChild(0,0);

                BufferedImage subOne = new BufferedImage(subBufferedImage.getColorModel(),
                        subRaster, subBufferedImage.isAlphaPremultiplied(), null);

            
                
            int[] pixelsSubOneInt = 
                    ((DataBufferInt) subOne.getRaster().getDataBuffer()).getData();
            //    short[] pixelsSubOne = ((DataBufferUShort) 
            //        subOne.getRaster().getDataBuffer()).getData();

            showImg(subOne);
        logger.log(Level.INFO, "new subOne has data length of {0}", pixelsSubOneInt.length);

        z = new short[rectBoxIdx.height][rectBoxIdx.width];

for (int i=0; i<rectBoxIdx.height; i++ ) {
    for (int j=0; j<rectBoxIdx.width; j++){
        z[i][j] = (short) pixelsSubOneInt[i*rectBoxIdx.width + j];
    }
}    
                
                
                */
                
    

               
                
            } else {
                logger.log(Level.SEVERE, "no directories in the file: {0}", file.toString());
                System.exit(-1);
            }

        } catch (ImageReadException ex) {
            Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Dem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    private short[][] ExtractFromDataElement(DataElement[] de, Rectangle rectBoxIdx) {
      short [][] zVal = new short[rectBoxIdx.height][rectBoxIdx.width];   
      
      
            
      for (int r=0; r<rectBoxIdx.height; r++) {
            // stream data row by row, 
            byte[] byteData = de[r+ rectBoxIdx.y].getData();
            
            // short[] shortArray = new short[rectBoxIdx.width];  
            short[] shortArray = new short[byteData.length/2];  
            ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
            
            for (int c = 0; c<rectBoxIdx.width; c++){
                short shortVal = shortArray[c+rectBoxIdx.x];
                zVal[r][c] = shortVal;
            }
                
            
        }
        
      
      return zVal;  
    } 
        
        
    

    
}
