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
    public static  String MPS_FILE_ON_DISK =  "";  //linux
    

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
    
    public static   String MIP_NAME_UNDER_TEST ="neos-847302" ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 300  ;    
    public static   double MIP_WELLKNOWN_SOLUTION =    4 ;  
    public static  int NUM_PARTITIONS  =50;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="wnq" ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 75000  ;    
    public static   double MIP_WELLKNOWN_SOLUTION =   259   ;  
    public static  int NUM_PARTITIONS  =5;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="r80x800" ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500  ;    
    public static   double MIP_WELLKNOWN_SOLUTION =    5332  ;  
    public static  int NUM_PARTITIONS  =50;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="swath" ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500  ;    
    public static   double MIP_WELLKNOWN_SOLUTION =    467.407   ;  
    public static  int NUM_PARTITIONS  =50;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="p6b" ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 50000  ;    
    public static   double MIP_WELLKNOWN_SOLUTION =   -63    ;  
    public static  int NUM_PARTITIONS  =5;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="p100x";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 250;    
    public static   double MIP_WELLKNOWN_SOLUTION =     47878 ;  
    public static  int NUM_PARTITIONS =50;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="neos-948126";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500;    
    public static   double MIP_WELLKNOWN_SOLUTION =      2607 ;  
    public static  int NUM_PARTITIONS =50;
    public static boolean COLLECT_ALL_METRICS = true;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = true;
    */
     
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="protfold";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 3000;     
    public static   double MIP_WELLKNOWN_SOLUTION =   -31    ;  
    public static  int NUM_PARTITIONS =75;
    public static boolean COLLECT_ALL_METRICS = true;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="wnq";//wnq-n100-mw99-14";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500;    
    public static   double MIP_WELLKNOWN_SOLUTION =   259 ;  
    public static  int NUM_PARTITIONS =50;    
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="b2c1s1";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 200;    
    public static   double MIP_WELLKNOWN_SOLUTION =  25687.9  ;  
    public static  int NUM_PARTITIONS =50;
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
     
    
    
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="atlanta-ip";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 600;     
    public static   double MIP_WELLKNOWN_SOLUTION =   90.009878614   ;  
    public static  int NUM_PARTITIONS =300;
    */
    
    /* 
    public static   String MIP_NAME_UNDER_TEST ="nu120-pr3";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 3000;     
    public static   double MIP_WELLKNOWN_SOLUTION =   28130   ;  
    public static  int NUM_PARTITIONS =300;
     */
        
     /*
    public static   String MIP_NAME_UNDER_TEST ="protfold";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 3000;     
    public static   double MIP_WELLKNOWN_SOLUTION =   -31    ;  
    public static  int NUM_PARTITIONS =75;
    */
     
    /*
    public static   String MIP_NAME_UNDER_TEST ="probportfolio";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 1800 ;     
    public static   double MIP_WELLKNOWN_SOLUTION =  16.7342    ;  
    public static  int NUM_PARTITIONS =150;
    */
        
    /*
    public static   String MIP_NAME_UNDER_TEST ="p6b";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 150 ;     
    public static   double MIP_WELLKNOWN_SOLUTION =   -63   ;  
    public static  int NUM_PARTITIONS =50;    
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    //public static int WORK_MEM = 1024;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
    
    /*
     public static   String MIP_NAME_UNDER_TEST ="d10200";
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 500 ;     
    public static   double MIP_WELLKNOWN_SOLUTION =   12430   ;  
    public static  int NUM_PARTITIONS =50;  
    public static boolean COLLECT_ALL_METRICS = false;
    public static int SAVE_NODE_FILE_TO_DISK = 3;
    public static boolean COLLECT_NUM_NODES_SOLVED = false;
    */
     
    /*
    public static boolean COLLECT_ALL_METRICS = true;
    public static int SAVE_NODE_FILE_TO_DISK = -1;
    //public static int WORK_MEM = 1024;
    public static boolean COLLECT_NUM_NODES_SOLVED = true;
    */
    
     
     
    
    public static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    
    public static int USE_MULTITHREADING_WITH_THIS_MANY_THREADS = 32;
    
}
