/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex.datatypes;
 
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class SolutionVector implements Serializable {
    
    public List<String> variableNames = new ArrayList<String>();
    public double[] values  ;
    
    public void add (String name ) {
        variableNames.add(name);
        
    }
    public void setvalues ( double[] values ) {
        this .   values =   values ;
    }
    
}
