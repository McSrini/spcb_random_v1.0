/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random;
   
import ca.mcmaster.spcb_random.cplex.datatypes.SolutionVector;
import ca.mcmaster.spcb_random.cplex.NodeSelectionStartegyEnum;
import ca.mcmaster.spcb_random.cplex.ActiveSubtree;
import ca.mcmaster.spcb_random.cplex.ActiveSubtreeCollection;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.*;
import ca.mcmaster.spcb_random.cb.CBInstructionTree;
import ca.mcmaster.spcb_random.cca.CCANode;
import static ca.mcmaster.spcb_random.cplex.NodeSelectionStartegyEnum.STRICT_BEST_FIRST;
import ca.mcmaster.spcb_random.cplex.datatypes.*;
import ca.mcmaster.spcb_random.rddFunctions.*;
import ilog.concert.IloException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.System.exit;
import java.lang.management.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
  

/**
 *
 * @author tamvadss
 * 
 *   
 * This is the spark driver for the CB random test
 */
public class TestDriver_CB_Random_Spark {
    
    private static  Logger logger = null;
     
     
    public static void main(String[] args) throws Exception {
       
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting the test." + LOG_FOLDER);
            exit(ONE);
        }
            
        logger=Logger.getLogger(TestDriver_CB_Random_Spark.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CB_Random_Spark.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
        //MPS_FILE_ON_DISK =  "F:\\temporary files here\\"+MIP_NAME_UNDER_TEST+".mps"; //windows
        MPS_FILE_ON_DISK =   MIP_NAME_UNDER_TEST +".mps";  //linux
                
        //Driver for distributing the CPLEX  BnB solver on Spark
        SparkConf conf = new SparkConf().setAppName("SparcPlex CCA V1.4");
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        
        /*
        Step 1:  we do the ramp up in the Driver
        */
        logger.debug ("starting ramp up " + MPS_FILE_ON_DISK + " partitions " + NUM_PARTITIONS +
                " solution cycle time " + SOLUTION_CYCLE_TIME_MINUTES + " tree slice " + TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE) ;  
        ActiveSubtree activeSubtreeForRampUp = new ActiveSubtree () ;
        activeSubtreeForRampUp.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
                       
        //find the best known solution after ramp up
        SolutionVector bestKnownSolutionAfterRampup  = null;
        double incumbentValueAfterRampup = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        if (activeSubtreeForRampUp.isFeasible()) {
            bestKnownSolutionAfterRampup =             activeSubtreeForRampUp.getSolutionVector();
            incumbentValueAfterRampup = activeSubtreeForRampUp.getObjectiveValue();
            logger.debug("best known solution after ramp up is "+ activeSubtreeForRampUp.getObjectiveValue()) ;
        } else {
            logger.debug("NO known solution after ramp up   " ) ;
        }
        
        /*
         Step 2: now get the CCA nodes and corresponding controlled branching instructions
        */
        logger.debug("getting CCA candidates ...") ;
        
        //get random CCA condidates, and the leafs they represent  
        //note that we accept all candidates
        List<List<String>> listOfComponentLeafsForCCANodes = new ArrayList<List<String>>();
        List<CCANode> candidateCCANodes =  getRandomCCACandidates (activeSubtreeForRampUp, NUM_PARTITIONS,   listOfComponentLeafsForCCANodes) ;
        
        if (candidateCCANodes.size() < NUM_PARTITIONS) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
        
        
        /*
        Step 3 : for all the CCA nodes, get their corresponding CB instruction
        
        Also collect the leafs each CCA node represents, for solving with Round-Robin ( SBF, BEF etc).
        
        Use these to prepare work assignments for each partition
        */
        List< CBInstructionTree> instructionTreeList = new ArrayList< CBInstructionTree>();
        List<WorkAssignment > workAssignmentsSBF  = new ArrayList<WorkAssignment>();
        List<WorkAssignment > workAssignmentsBEF  = new ArrayList<WorkAssignment>();
        List<WorkAssignment > workAssignmentsCB  = new ArrayList<WorkAssignment>();
        for (int index = ZERO; index < candidateCCANodes.size(); index ++){

            CCANode ccaNode= candidateCCANodes.get(index);
            
            logger.debug (""+ccaNode.nodeID + " has packing factor " +ccaNode.getPackingFactor() + 
                        " and component list size " + listOfComponentLeafsForCCANodes.get(index).size() + 
                        " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
            
            instructionTreeList.add(
                    activeSubtreeForRampUp.getCBInstructionTree(ccaNode, listOfComponentLeafsForCCANodes.get(index))
            );
            
            List<CCANode> leafNodeList = activeSubtreeForRampUp.getActiveLeafsAsCCANodes(  listOfComponentLeafsForCCANodes.get(index));   
            
            //prepare work assignments for partition "index", for all three strategies namely SBF, BEF, CB
            workAssignmentsCB.add (new WorkAssignment(ccaNode, instructionTreeList.get(index), index)) ;
            workAssignmentsBEF.add (new WorkAssignment(leafNodeList, index));
            workAssignmentsSBF.add (new WorkAssignment(leafNodeList, index));
            
        }
        
                
        //PREPARATIONS COMPLETE
        logger.debug ("Ramp up created " +activeSubtreeForRampUp.getActiveLeafCount() +" leafs.") ;         
        activeSubtreeForRampUp.end();
  
        //at this point, we have farmed out CCA nodes with CB instructions, and also
        //have the corresponding BEF and SBF work assignments 
        //
        
        /* Step 4 : 
            Now distribute collected lists across the cluster    
        */
               
        //these are the RDDs that need to be prepared and then solved
        JavaPairRDD < Integer, ActiveSubtreeCollection > frontierCB ; 
        JavaPairRDD < Integer,  ActiveSubtreeCollection > frontierSBF ; 
        JavaPairRDD < Integer,  ActiveSubtreeCollection > frontierBEF ; 
        
        frontierCB = sc.parallelize( workAssignmentsCB)
            .mapToPair(new PartitionIdAppender() )
            /*   convert work assignment into an ActiveSubTreeCollection object, note partition preservation*/
            .mapValues(new ActiveSubtreeCollectionCreator( activeSubtreeForRampUp.instructionsFromOriginalMip , 
                    incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null  ))  
            //Frontier is used many times, so cache it.
            .cache();
        
        frontierSBF = sc.parallelize( workAssignmentsSBF)
            .mapToPair(new PartitionIdAppender() )
            /*   convert work assignment into an ActiveSubTreeCollection object, note partition preservation*/
            .mapValues(new ActiveSubtreeCollectionCreator( activeSubtreeForRampUp.instructionsFromOriginalMip , 
                    incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null  ))  
            //Frontier is used many times, so cache it.
            .cache();
        
        frontierBEF = sc.parallelize( workAssignmentsBEF)
            .mapToPair(new PartitionIdAppender() )
            /*   convert work assignment into an ActiveSubTreeCollection object, note partition preservation*/
            .mapValues(new ActiveSubtreeCollectionCreator( activeSubtreeForRampUp.instructionsFromOriginalMip , 
                    incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null  ))  
            //Frontier is used many times, so cache it.
            .cache();
        
        //frontiers are ready, all we have to do now is solve them
        
        
        /*
        Step 5 : solve the partitions using 3 strategies
        */
        
        //initialize our best known incumbent so far
        Double  incumbentGlobal= incumbentValueAfterRampup;
        int iterationNumber=ZERO;
       
        //CB test
        for (iterationNumber=ZERO;   ;iterationNumber++){            
            logger.debug("starting CB iteration Number "+iterationNumber);            
            ResultOfPartitionSolve result = runOneSolutionCycle (   frontierCB, incumbentGlobal,
                                                                        SOLUTION_CYCLE_TIME_MINUTES  ,  TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE,
                                                                        STRICT_BEST_FIRST, iterationNumber==ZERO /*first iteration is for reincarnation*/); 
            
            incumbentGlobal = result.bestKnownSolution;
            if (result.isComplete) {
                 logger.debug(" CB test ended at iteration Number "+iterationNumber + " with incumbent "+incumbentGlobal );
                 break;
            } else logger.debug(" Best known solution is "+incumbentGlobal );
        }
        
        //repeat test for all node selection strategies
        //defaults to SBF
        JavaPairRDD < Integer,  ActiveSubtreeCollection > frontier=frontierSBF;
        for(NodeSelectionStartegyEnum nodeSelectionStrategy  :NodeSelectionStartegyEnum.values()){
            
            //skip LSI and BEF
            if(NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(nodeSelectionStrategy ))  continue; 
            if(NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST.equals(nodeSelectionStrategy ))  continue; 
            
            if(NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST.equals(nodeSelectionStrategy )){
               frontier = frontierBEF;                 
            } 
            
            logger.info(" \n\n\ntest started for Selection Strategy " + nodeSelectionStrategy  );
            
            //reset incumbent to value after ramp up
            incumbentGlobal= incumbentValueAfterRampup;
            
            //now run the iterations
                 
            for (  iterationNumber=ZERO;   ; iterationNumber++){
                logger.debug("starting " +nodeSelectionStrategy+ " iteration Number "+iterationNumber);            
                ResultOfPartitionSolve result = runOneSolutionCycle (   frontier, incumbentGlobal,
                                                                            SOLUTION_CYCLE_TIME_MINUTES  ,  TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE,
                                                                            STRICT_BEST_FIRST, false); 

                //update incumbent
                incumbentGlobal = result.bestKnownSolution;
                if (result.isComplete) {
                     logger.debug(" " + nodeSelectionStrategy +" test ended at iteration Number "+iterationNumber + " with incumbent "+incumbentGlobal );
                     break;
                } else logger.debug(" Best known solution is "+incumbentGlobal );
            }
            
        }//all node selection strategies
        
        logger.info("all parts of the test completed");
        
    } //end main
    
    private static ResultOfPartitionSolve runOneSolutionCycle ( JavaPairRDD < Integer, ActiveSubtreeCollection > frontier, 
            Double cutoff,  double solutionCycleTimeMinutes,   double timeSlicePerTreeInMInutes ,  
            NodeSelectionStartegyEnum nodeSelectionStartegy , boolean reincarnationFlag) {
        
        int numRemainingPartitions = ZERO;
        Double updatedGlobalIncumbent =cutoff;
        
        if(!isHaltFilePresent()) {
            
            JobSolver jobSolver = new JobSolver (cutoff, SOLUTION_CYCLE_TIME_MINUTES,   TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE ,  
                                                     STRICT_BEST_FIRST , reincarnationFlag ) ;
                
            List<Tuple2<Integer,ResultOfPartitionSolve>> resultsOfSolving =frontier.mapValues(jobSolver).collect();

            //note , the old RDD is cached again, because its values would have changed.
            //the new RDD created by the Map job is discarded , since it contains the same values as the old one
            frontier.cache();

            for (int index = ZERO; index < resultsOfSolving.size() ; index++){
                ResultOfPartitionSolve result = resultsOfSolving.get(index)._2;
                int partitionID  = resultsOfSolving.get(index)._1;
                logger.debug(" Partition ID " + partitionID + " is " + (result.isComplete ? "Complete": "Pending"));
                if (!result.isComplete) numRemainingPartitions ++;

                if (IS_MAXIMIZATION) {
                    updatedGlobalIncumbent= Math.max(updatedGlobalIncumbent,  result.bestKnownSolution);
                }else {
                    updatedGlobalIncumbent= Math.min(updatedGlobalIncumbent,  result.bestKnownSolution);
                }

            }

            logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);    
            logger.debug ("Global incumbent is "+updatedGlobalIncumbent);
        }
         
        return new ResultOfPartitionSolve(numRemainingPartitions==ZERO,     updatedGlobalIncumbent);
    }
        
    private static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }  
      
    private static List< CCANode>   getRandomCCACandidates (ActiveSubtree activeSubtreeForRampUp, int NUM_PARTITIONS,
                                                            List<List<String>> listOfComponentLeafs) throws IloException{
        List<CCANode> result = new ArrayList<CCANode> ();
        
        //first get the list of leafs in the ramped up tree
        List<NodeAttachment> nodeList = activeSubtreeForRampUp.getActiveLeafList();
        List<String> node_ID_List  = new ArrayList<String>();
        for (NodeAttachment node :nodeList ){
            node_ID_List.add(node.nodeID);
        }
        
        //logger.debug ("printing all leafs in ramp up") ;
        for (String id : node_ID_List) {
            logger.debug (id ) ;
        }
        
                
        //now get NUM_PARTITIONS sublists
        // this is done by shuffling the leafs and then creating equal sized sub-lists
        //note the psuedo random seed for reproducability of the tests
        Collections.shuffle( node_ID_List,  new Random((long) ONE));
         
        
        int subListSize = node_ID_List.size()/NUM_PARTITIONS ;
        
        for  (int subListIndex = ZERO;node_ID_List.size()>ZERO ; subListIndex++) {
            
            //remove subListSize leafs and add into this subList
            // in the last iteration, absorb the reaminder into th elast candidate
            if (node_ID_List.size()-subListSize< subListSize) subListSize+=(node_ID_List.size()-subListSize);
            
            List<String> subList = new ArrayList<String> ();
            int removeCount = subListSize;
            while (removeCount>ZERO && node_ID_List.size()>ZERO) {
                subList.add(node_ID_List.remove( -ONE + node_ID_List.size()));
                removeCount --;
            }
               
            //get the CCA node for this sublist of leafs
            result.add( activeSubtreeForRampUp.getCandidateCCANodes( subList, true).get(ZERO));
            //make a note of which leafs this CCA node represents
            listOfComponentLeafs.add(subList);
            
            //logger.debug ("printing all leafs in CCA node " + result.get(-ONE+ result.size())) ;
            //for (String id : subList) {
                //logger.debug (id) ;
            //}
            
        }
            
        logger.debug (" Number of CCA nodes "+ result.size());
        logger.debug (" Requested NUM_PARTITIONS "+ NUM_PARTITIONS );
        logger.debug (" Number of items in component leaf list "+ listOfComponentLeafs.size());
        logger.debug (" Sizes of each component leaf list " );
        for ( List<String> strList :listOfComponentLeafs){
            logger.debug (" Sizes   " +strList.size());
        }
                  
        return result;
    }

    private static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");
         
        return file.exists();
    }
}