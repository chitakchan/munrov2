/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import utility.UtilsD;

/**
 *
 * @author Think
 */
public class SquarePlane {
    ArrayList<MyVertice> vertices = new ArrayList<MyVertice>();
    
    public SquarePlane(MyVertice v, double widthM, double heightM){
        
        // add four corners in sequence of right hand rule
        // in unit of lon lat,
        
        // convert from meter into arc second
        double width = widthM/UtilsD.convDegToENFactor(v.getXYZ()[1])[0];
        double height = heightM/UtilsD.convDegToENFactor(v.getXYZ()[1])[1];
        
        double x = v.getXYZ()[0];
        double y = v.getXYZ()[1];
        double z = v.getXYZ()[2];
        vertices.add(new MyVertice(x-width/2, y+height/2, z));  //ul corner
        vertices.add(new MyVertice(x-width/2, y-height/2, z));    //ll
        vertices.add(new MyVertice(x+width/2, y-height/2, z));    //lr
        vertices.add(new MyVertice(x+width/2, y+height/2, z));    //ur
    }
    
    public String toString(Properties prop){
        StringBuilder sb = new StringBuilder("");
        Iterator<MyVertice> iter = vertices.iterator();
        while (iter.hasNext()){
            sb.append(iter.next().toString(prop));
        }
        // surface here
        sb.append("f -4 -3 -2 -1");
        
        return sb.toString();
    }
}
