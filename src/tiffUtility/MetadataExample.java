  
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tiffUtility;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;

public class MetadataExample {
    
    public static void tiffMetaData(final File file) throws ImageReadException, IOException {
        final ImageMetadata metadata = Imaging.getMetadata(file);
        System.out.println("Running metadataExample...\n");
        System.out.println(metadata);
        
        if ((metadata instanceof TiffImageMetadata) ) {
            final TiffImageMetadata tiffMetadata = (TiffImageMetadata) metadata;
            List<TiffField> fieldList = new ArrayList<>();
            fieldList.addAll(tiffMetadata.getAllFields());
            StringBuilder sb = new StringBuilder("list of tiffMetaData\n");
            for (int i=0; i<fieldList.size(); i++) {
                sb.append("Tag name: " + fieldList.get(i).getTagName()+ 
                        ", field type name: "+ fieldList.get(i).getFieldTypeName() + 
                        ", field type: " + fieldList.get(i).getFieldType() + 
                        ", description : "+ fieldList.get(i).getDescriptionWithoutValue()  +
                        // ", string value: "+ fieldList.get(i).getStringValue() +
                        ", value desc: "+ fieldList.get(i).getValueDescription()+
                        ", tagInfo: "+ fieldList.get(i).getTagInfo()+
                        "\n");
            }
            System.out.println(sb.toString());
            
            
            
            printTagValueTiff(tiffMetadata, TiffTagConstants.TIFF_TAG_SAMPLES_PER_PIXEL);
            printTagValueTiff(tiffMetadata, TiffTagConstants.TIFF_TAG_BITS_PER_SAMPLE);
            printTagValueTiff(tiffMetadata, TiffTagConstants.TIFF_TAG_BITS_PER_SAMPLE);
        }
        
        
    }
    
    public static void metadataExample(final File file) throws ImageReadException,
            IOException {
        // get all metadata stored in EXIF format (ie. from JPEG or TIFF).
        final ImageMetadata metadata = Imaging.getMetadata(file);

        // System.out.println(metadata);


        if ((metadata instanceof JpegImageMetadata) )
        
        {
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

            // Jpeg EXIF metadata is stored in a TIFF-based directory structure
            // and is identified with TIFF tags.
            // Here we look for the "x resolution" tag, but
            // we could just as easily search for any other tag.
            //
            // see the TiffConstants file for a list of TIFF tags.

            System.out.println("file: " + file.getPath());

            // print out various interesting EXIF tags.
            printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_XRESOLUTION);
            printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_DATE_TIME);
            printTagValue(jpegMetadata,
                    ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
            printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_ISO);
            printTagValue(jpegMetadata,
                    ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
            printTagValue(jpegMetadata,
                    ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
            printTagValue(jpegMetadata,
                    ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
            printTagValue(jpegMetadata,
                    GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
            printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE);
            printTagValue(jpegMetadata,
                    GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
            printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE);

            System.out.println();

            // simple interface to GPS data
            final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (null != exifMetadata) {
                final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
                if (null != gpsInfo) {
                    final String gpsDescription = gpsInfo.toString();
                    final double longitude = gpsInfo.getLongitudeAsDegreesEast();
                    final double latitude = gpsInfo.getLatitudeAsDegreesNorth();

                    System.out.println("    " + "GPS Description: "
                            + gpsDescription);
                    System.out.println("    "
                            + "GPS Longitude (Degrees East): " + longitude);
                    System.out.println("    "
                            + "GPS Latitude (Degrees North): " + latitude);
                }
            }

            // more specific example of how to manually access GPS values
            final TiffField gpsLatitudeRefField = jpegMetadata.findEXIFValueWithExactMatch(
                    GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
            final TiffField gpsLatitudeField = jpegMetadata.findEXIFValueWithExactMatch(
                    GpsTagConstants.GPS_TAG_GPS_LATITUDE);
            final TiffField gpsLongitudeRefField = jpegMetadata.findEXIFValueWithExactMatch(
                    GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
            final TiffField gpsLongitudeField = jpegMetadata.findEXIFValueWithExactMatch(
                    GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
            if (gpsLatitudeRefField != null && gpsLatitudeField != null &&
                    gpsLongitudeRefField != null &&
                    gpsLongitudeField != null) {
                // all of these values are strings.
                final String gpsLatitudeRef = (String) gpsLatitudeRefField.getValue();
                final RationalNumber[] gpsLatitude = (RationalNumber[]) (gpsLatitudeField.getValue());
                final String gpsLongitudeRef = (String) gpsLongitudeRefField.getValue();
                final RationalNumber[] gpsLongitude = (RationalNumber[]) gpsLongitudeField.getValue();

                final RationalNumber gpsLatitudeDegrees = gpsLatitude[0];
                final RationalNumber gpsLatitudeMinutes = gpsLatitude[1];
                final RationalNumber gpsLatitudeSeconds = gpsLatitude[2];

                final RationalNumber gpsLongitudeDegrees = gpsLongitude[0];
                final RationalNumber gpsLongitudeMinutes = gpsLongitude[1];
                final RationalNumber gpsLongitudeSeconds = gpsLongitude[2];

                // This will format the gps info like so:
                //
                // gpsLatitude: 8 degrees, 40 minutes, 42.2 seconds S
                // gpsLongitude: 115 degrees, 26 minutes, 21.8 seconds E

                System.out.println("    " + "GPS Latitude: "
                        + gpsLatitudeDegrees.toDisplayString() + " degrees, "
                        + gpsLatitudeMinutes.toDisplayString() + " minutes, "
                        + gpsLatitudeSeconds.toDisplayString() + " seconds "
                        + gpsLatitudeRef);
                System.out.println("    " + "GPS Longitude: "
                        + gpsLongitudeDegrees.toDisplayString() + " degrees, "
                        + gpsLongitudeMinutes.toDisplayString() + " minutes, "
                        + gpsLongitudeSeconds.toDisplayString() + " seconds "
                        + gpsLongitudeRef);

            }

            System.out.println();

            final List<ImageMetadataItem> items = jpegMetadata.getItems();
            for (final ImageMetadataItem item : items) {
                System.out.println("    " + "item: " + item);
            }

            System.out.println();
        }
    }

    private static void printTagValue(final JpegImageMetadata jpegMetadata,
            final TagInfo tagInfo) {
        final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
        if (field == null) {
            System.out.println(tagInfo.name + ": " + "Not Found.");
        } else {
            System.out.println(tagInfo.name + ": "
                    + field.getValueDescription());
        }
    }

    private static void printTagValueTiff(final TiffImageMetadata tiffMetadata,
            final TagInfo tagInfo) {
        try {
            final TiffField field = tiffMetadata.findField(tagInfo, true);
            if (field == null) {
                System.out.println(tagInfo.name + ": " + "Not Found.");
            } else {
                System.out.println(tagInfo.name + ": "
                        + field.getValueDescription());
            }
        } catch (ImageReadException ex) {
            Logger.getLogger(MetadataExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

public static void readGMTEDMetadata(File file){
        try {
            final ImageMetadata metadata = Imaging.getMetadata(file);
            // List<ImageMetadataItem> mdList = new ArrayList<ImageMetadataItem>();
            
            Properties propMetadata = new Properties();
            propMetadata.load(new StringReader(metadata.toString()));
            
            
            String[] s = null;
            // get image width and length
            Integer imageWidth = Integer.parseInt(propMetadata.getProperty("ImageWidth"));
            Integer imageLength = Integer.parseInt(propMetadata.getProperty("ImageLength"));
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
            
            
            // get modelTiePointTag
            ArrayList<Double> modelTiepointTagList = new ArrayList<>();
            Arrays.asList(propMetadata.getProperty("ModelTiepointTag").split(",")).forEach((e) ->{
                modelTiepointTagList.add(Double.parseDouble(e));
                
            });
            
            
            // print out
            StringBuilder sbProp = new StringBuilder("\nImageMetadata read as properties:\n");
            sbProp.append("width: "+imageWidth+ " length: "+ imageLength + "\n");
            sbProp.append("xResolation: " + modelPixelScaleTagList.get(0) + ", yResolation: " + modelPixelScaleTagList.get(1) + "\n");
            sbProp.append("xMap: " + modelTiepointTagList.get(3) + ", yMap: " + modelTiepointTagList.get(4) + "\n");
            System.out.println(sbProp.toString());
            
            System.out.println("\nmetadata:\n");
            System.out.println(metadata);
        } catch (ImageReadException ex) {
            Logger.getLogger(MetadataExample.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MetadataExample.class.getName()).log(Level.SEVERE, null, ex);
        }
            
           
    }
    
}