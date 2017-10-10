/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class ConstantsAndParameters {
        
    public static final String EMPTY_STRING ="";
    public static final String MINUS_ONE_STRING = "-1";
    public static final int ZERO = 0;
    public static final double DOUBLE_ZERO = 0.0;
    public static final int ONE = 1;
    public static final int TWO = 2;    
    public static final int THREE = 3; 
    public static final int FOUR = 4;    
    public static final int FIVE = 5;  
    public static final int EIGHT = 8;  
    public static final int TEN = 10;  
    public static final int SIXTY = 60;  
    public static final int HUNDRED = 100 ;  
    public static final int THOUSAND = 1000 ;  
    public static final int MILLION = 1000000;  
    public static final long PLUS_INFINITY = Long.MAX_VALUE;
    public static final long MINUS_INFINITY = Long.MIN_VALUE;
    public static final double EPSILON = 0.0000000001;
    public static final String DELIMITER = "______";
    
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\rd-rplusc-21.mps";
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\atlanta-ip.mps"; //windows
    public static  String MPS_FILE_ON_DISK =  "p100x588b.mps";  //linux
    

    //public static final String LOG_FOLDER="F:\\temporary files here\\logs\\testing\\ccav1_3\\"; //windows
    public static final String LOG_FOLDER="logs/"; //linux
    public static final String LOG_FILE_EXTENSION = ".log";
    
     
    public static final boolean IS_MAXIMIZATION = false;
     
    
    //used to reject inferior LCA candidates
    public static final double LCA_CANDIDATE_PACKING_FACTOR_LARGEST_ACCEPTABLE=1.20;
    
    
    //CCA subtree allowed to have slightly less good leafs than asked for in NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE 
    public static   double CCA_TOLERANCE_FRACTION =  0.00;
    public static  double CCA_PACKING_FACTOR_MAXIMUM_ALLOWED =  0.0;
    
   
    public static   int TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 4 ;    
    public static   double MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 0.5 ;//30 seconds
    public static final int SOLUTION_CYCLE_TIME_MINUTES = 8;
    
    public static   String MIP_NAME_UNDER_TEST ="timtab1";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 20;   // or 5000    
    public static   double MIP_WELLKNOWN_SOLUTION =  764772;  
    public static  int NUM_PARTITIONS =4;
    
    public static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    
    
    
}
