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
import ca.mcmaster.spcb_random.cplex.datatypes.*;
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
  

/**
 *
 * @author tamvadss
 * 
 *  glass4   , 50000:5000   0.3  - TEST 1 confirmed - took 7 hours
 
 *  glass4 1000000, 100 
 
 
 * 
 * run MIP on 5 simulated partitions
 * sub problems created using variable bound merging, and solved using traditional branch and bound
 * 2 ramp ups, one for using CCA and one without CCA
 */
public class TestDriver_CB_Random {
    
    private static  Logger logger = null;
    
    //  50(20), 100(40) , 200(90), 250(112) parts is ok
    
    //wnq-n100-mw99-14  ru=5000, pa=100  size >= 50/4 yeilds 85 candidates with home=42   fast  big-mem-hog-cannot_simulate
    //p100x588b        ru=15000, pa=100  size >= 50/4 yeilds 97 candidates with home=53   fast  mem-hog-cannot_simulate
    //b2c1s1 ru=5000, pa=100  size >= 50/3 yeilds 94 candidates with home=48
    //seymour-disj-10 ru=5000, pa=100  size >= 50/4 yeilds 68 candidates with home=74
    //usAbbrv-8-25_70 ru=10000, pa=100  size >= 50/4 yeilds 96 candidates with home=77
    //neos-847302 ru=10000, pa=100  size >= 50/4 yeilds 94 candidates with home=50
    //janos-us-DDM ru=8000, pa=100  size >= 50/4 yeilds 90 candidates with home=30  fast   leaves lots on home, try increasing packfact to 2.0 or min allowed size
    //
    //seymour ru=8000, pa=100  size >= 50/4 yeilds 99 candidates with home=40
    //rococoB10-011000 ru=5000, pa=100  size >= 50/4 yeilds 95 candidates with home=19
    //  momentum1  ru=5000, pa=100  size >= 50/4 yeilds 90 candidates with home=88
    
    //for big partition counts
    //
    //had1 running janos with 1000 with memcheck and 2 min slices, had2 running seymour with   500 and memory and 2 min slices
    //had 3 running seymour with 250  with memcheck and 2 min slices , had 4 running seymour with 250  with memcheck and 3minute slices
    //had 5 runing ? with 250 with memcheck with 2 min slices CB+LSI+CCA
    //
    //p100x588b ru=60000, NUM_PARTITIONS = 1150 , 580, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m size >=20/4, packfact=1.2
    //wnq-n100-mw99-14 ru=25000, NUM_PARTITIONS = 1150 , 550, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m
    //janos-us-DDM ru=20000, NUM_PARTITIONS = 1150 , 580, 250 , sct = 6m, ts=2m, 6m:3m 6m:1m 
    //seymour ru=10000, NUM_PARTITIONS =  200 , sct = 6m, ts=2m, 6m:3m 6m:1m
    //seymour-disj_10 ru=20000, NUM_PARTITIONS =  1000 , sct = 6m, ts=2m, 6m:3m 6m:1m size >=20/10, packfact=12 
     
    
    public static   String MIP_NAME_UNDER_TEST ="a1c1s1";
    public static   double MIP_WELLKNOWN_SOLUTION =  11503.444125 ;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 10000;   // or 5000
    
    /*
    public static   String MIP_NAME_UNDER_TEST ="timtab1";
    public static   double MIP_WELLKNOWN_SOLUTION =  764772;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 2000;  */
 
    private static  int NUM_PARTITIONS =100;
    private static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    
    //private static final int SOLUTION_CYCLE_Tu           fgggd hjhhIME_MINUTES = THREE;
     
    public static void main(String[] args) throws Exception {
       
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting the test." + LOG_FOLDER);
            exit(ONE);
        }
            
        logger=Logger.getLogger(TestDriver_CB_Random.class);
        logger.setLevel(Level.DEBUG);
        
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CB_Random.class.getSimpleName()+ LOG_FILE_EXTENSION);
           
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
        //MPS_FILE_ON_DISK =  "F:\\temporary files here\\"+MIP_NAME_UNDER_TEST+".mps"; //windows
        MPS_FILE_ON_DISK =   MIP_NAME_UNDER_TEST +".mps";  //linux
                
        logger.debug ("starting ramp up") ;  
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
        
         
        logger.debug("getting CCA candidates ...") ;
        
        //get random CCA condidates, and the leafs they represent
        List<List<String>> listOfComponentLeafsForCandidateCCANodes = new ArrayList<List<String>>();
        List<CCANode> candidateCCANodes =  getRandomCCACandidates (activeSubtreeForRampUp, NUM_PARTITIONS,   listOfComponentLeafsForCandidateCCANodes) ;
        
        if (candidateCCANodes.size() < NUM_PARTITIONS) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
        
        //we accept all generated candidates, see later
        
        //for every accepted CCA node, we create an active subtree collection that has all its leafs
        List<ActiveSubtreeCollection> activeSubtreeCollectionListSBF = new ArrayList<ActiveSubtreeCollection>();
        List<ActiveSubtreeCollection> activeSubtreeCollectionListBEF = new ArrayList<ActiveSubtreeCollection>();
        //we also create an ActiveSubtreeCollectionlist where each collection on a partition only has 1 element, namely the CCA node assigned to it.
        List<ActiveSubtreeCollection> activeSubtreeCollectionListCCA = new ArrayList<ActiveSubtreeCollection>();
        //and here are the CB instruction trees for each CCA node, and a subtree collection for CB
        List< CBInstructionTree>  cbInstructionTreeList  =new ArrayList<CBInstructionTree > () ;   
        List<ActiveSubtreeCollection> activeSubtreeCollectionListCB = new ArrayList<ActiveSubtreeCollection>();
        
        // now lets populate the ActiveSubtreeCollections
        //
        long acceptedCandidateWithLowestNumOfLeafs = PLUS_INFINITY;
        for (int index = ZERO; index < candidateCCANodes.size(); index ++){

            CCANode ccaNode= candidateCCANodes.get(index);
            
            if (ccaNode.pruneList.size() >=  ZERO) { //accept every candidate
                
                logger.debug (""+ccaNode.nodeID + " has good factor " +ccaNode.getPackingFactor() + 
                        " and prune list size " + ccaNode.pruneList.size() + " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
                 
                //          qxxy               dod     
                               
                acceptedCandidateWithLowestNumOfLeafs = Math.min(acceptedCandidateWithLowestNumOfLeafs,ccaNode.pruneList.size()  );
                
                //All leafs need to be converted into CCA node representations, since solvers deal with CCA nodes and not leafs
                //These CCA nodes are of course fake CCA nodes
                List<CCANode> ccaLeafNodeListSBF = activeSubtreeForRampUp.getActiveLeafsAsCCANodes( ccaNode.pruneList);                                      
                //now create an active subtree collection , which represents the work on one partition by doing a round-robin thru these leafs
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaLeafNodeListSBF, activeSubtreeForRampUp.instructionsFromOriginalMip, 
                        incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListSBF.add(astc);
                
                //repeat for BEF  
                List<CCANode> ccaLeafNodeListBEF = new ArrayList<CCANode> () ;
                ccaLeafNodeListBEF.addAll(ccaLeafNodeListSBF) ;
                astc = new ActiveSubtreeCollection (ccaLeafNodeListBEF, activeSubtreeForRampUp.instructionsFromOriginalMip, 
                        incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListBEF .add(astc);
                
                //we also create another collection list, with only the accepted CCA nodes, one on each partition
                List<CCANode> ccaSingletonLeafNodeList = new ArrayList<CCANode> ();
                ccaSingletonLeafNodeList.add(ccaNode);
                astc = new ActiveSubtreeCollection ( ccaSingletonLeafNodeList, activeSubtreeForRampUp.instructionsFromOriginalMip, incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListCCA.add(astc);
                
                //create a list of CB instuction trees, and an active subtree collection with all the corresponding CCA  nodes
                CBInstructionTree tree = activeSubtreeForRampUp.getCBInstructionTree(ccaNode, listOfComponentLeafsForCandidateCCANodes.get(index));
                cbInstructionTreeList.add( tree); 
                astc = new ActiveSubtreeCollection ( ccaSingletonLeafNodeList, activeSubtreeForRampUp.instructionsFromOriginalMip, incumbentValueAfterRampup, bestKnownSolutionAfterRampup!=null, index) ;
                activeSubtreeCollectionListCB.add(astc);
                
            }               
        }
         
        
        //at this point, we have farmed out CCA nodes, and also
        //have the corresponding subtree collections for comparision [ each subtree collection has all the leafs of the corresponding CCA]                 
        logger.debug ("number of CCA nodes collected = "+candidateCCANodes.size() + 
                " . The lowest number of leafs represented by a CCA node is "+ acceptedCandidateWithLowestNumOfLeafs) ;            
        for ( int index = ZERO; index <  candidateCCANodes.size(); index++){
            logger.debug("CCA node is : " + candidateCCANodes.get(index) + 
                    " and its prune list size is " + candidateCCANodes.get(index).pruneList.size()) ;
            logger.debug ("number of leafs in corresponding active subtree collection SBF is = " +     
                    (activeSubtreeCollectionListSBF.get(index).getPendingRawNodeCount() + activeSubtreeCollectionListSBF.get(index).getNumTrees()) );
        }
         
              
 
        
        //PREPARATIONS COMPLETE
        activeSubtreeForRampUp.end();
       
        
        //TEST 1 uses CCA
        //LAter on , TEST 2 will use individual leafs
        
        
        //TEST 1
        
        //init the best known solution value and vector which will be updated as the solution progresses
        //Initialize them to values after ramp up
        //SolutionVector  bestKnownSolution = bestKnownSolutionAfterRampup ==null? null : activeSubtreeONE.getSolutionVector();
        Double  incumbentValue= incumbentValueAfterRampup;
         
         
        //the first test uses CCA , the second test will use raw leafs
        
        //TEST 1 : with CCA
        /******
           CANNOT   RUN CCA TEST WHEN LEAFS SELECTED AT RANDOM !!
         *********/
        int iterationNumber=ZERO;
        for (; iterationNumber<-ONE   ;iterationNumber++){ 
               
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting CCA iteration Number "+iterationNumber);
                 
            //simulate 1 map iteration accross the "cluster", note that selection stratgey does not matter because
            //we have only 1 job per partition
            int numRemainingPartitions = simulateOneMapIteration ( activeSubtreeCollectionListCCA, 
                                                   NodeSelectionStartegyEnum.STRICT_BEST_FIRST,    incumbentValue,
                                                   false, null);
                
            //update driver's copy of incumbent
            for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA){
                if (IS_MAXIMIZATION) {
                    incumbentValue= Math.max(incumbentValue,  astc.getIncumbentValue());
                }else {
                    incumbentValue= Math.min(incumbentValue,  astc.getIncumbentValue());
                }
            }

            logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);                

            //do another iteration involving every partition, unless we are done
            if (ZERO==numRemainingPartitions)  break;
            
        }//for greenFlagForIterations
        
        //print results
        long numNodeRelaxationsSolved = ZERO;
        for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA) {
            numNodeRelaxationsSolved += astc.getNumNodeRelaxationsSolved();
        } 
        logger.debug(" CCA test ended at iteration Number "+iterationNumber + " with incumbent "+
                incumbentValue + " and " + numNodeRelaxationsSolved+" leafs solved");
        
        
        
         
        //HERE is part 2 of the test, where we run individual leafs and compare results with CCA               
           
        List<ActiveSubtreeCollection> activeSubtreeCollectionList =null;
        
        //repeat test for all node selection strategies
        for(NodeSelectionStartegyEnum nodeSelectionStrategy  :NodeSelectionStartegyEnum.values()){
            
            //skip LSI and BEF
            if(NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(nodeSelectionStrategy ))  continue; 
            
            if(NodeSelectionStartegyEnum.STRICT_BEST_FIRST.equals(nodeSelectionStrategy )){
                activeSubtreeCollectionList= activeSubtreeCollectionListSBF;                 
            } else {
                activeSubtreeCollectionList= activeSubtreeCollectionListBEF;  
            }
            
            logger.info(" \n\n\ntest started for Selection Strategy " + nodeSelectionStrategy  );
            
            //reset incumbent to value after ramp up
            incumbentValue= incumbentValueAfterRampup;
            
            //now run the iterations
                 
            for (  iterationNumber=ZERO;   ; iterationNumber++){
   
                if(isHaltFilePresent())  break;//halt
                logger.debug("starting test2 iteration Number "+iterationNumber);

                int numRemainingPartitions = simulateOneMapIteration ( activeSubtreeCollectionList, 
                                                   nodeSelectionStrategy,    incumbentValue,
                                                   false, null);
                
                //update driver's copy of incumbent
                for (ActiveSubtreeCollection astc : activeSubtreeCollectionList){
                    if (IS_MAXIMIZATION) {
                        incumbentValue= Math.max(incumbentValue,  astc.getIncumbentValue());
                    }else {
                        incumbentValue= Math.min(incumbentValue,  astc.getIncumbentValue());
                    }
                }
                
                logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);                
            
                //do another iteration involving every partition, unless we are done
                if (ZERO==numRemainingPartitions)  break;

            }//end     of iterations

            //print results
            numNodeRelaxationsSolved = ZERO;
            for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA) {
                numNodeRelaxationsSolved += astc.getNumNodeRelaxationsSolved();
            } 
            logger.debug(" Individual solve test with "+ nodeSelectionStrategy +" ended at iteration Number "+
                    iterationNumber+ " with incumbent "+incumbentValue + " and number of leafs solved " + numNodeRelaxationsSolved);
            //print status of every partition
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){

                ActiveSubtreeCollection astc= activeSubtreeCollectionList.get(partitionNumber);
                logger.debug ("partition "+partitionNumber   +
                        " trees count " + astc.getNumTrees()+" raw nodes count "+ astc.getPendingRawNodeCount() + " max trees created " + astc.maxTreesCreatedDuringSolution);
                astc.endAll();

            }//print status of every partition
            
            logger.info(" test completed Selection Strategy for " + nodeSelectionStrategy);
            
        }//for all node sequencing strategies
        
        
        //Test 3 , use CB
        for (iterationNumber=ZERO;   ;iterationNumber++){ 
               
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting CB iteration Number "+iterationNumber);
                 
            int numRemainingPartitions = -ONE;
            if (iterationNumber==ZERO) {
                //reincarnate leafs using CB
                numRemainingPartitions = simulateOneMapIteration ( activeSubtreeCollectionListCB, 
                                                   NodeSelectionStartegyEnum.STRICT_BEST_FIRST,    incumbentValue, 
                                                   true,   cbInstructionTreeList);
            }else {
                numRemainingPartitions= simulateOneMapIteration ( activeSubtreeCollectionListCB, 
                                                   NodeSelectionStartegyEnum.STRICT_BEST_FIRST,    incumbentValue, false, null);
            }
                            
            //update driver's copy of incumbent
            for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA){
                if (IS_MAXIMIZATION) {
                    incumbentValue= Math.max(incumbentValue,  astc.getIncumbentValue());
                }else {
                    incumbentValue= Math.min(incumbentValue,  astc.getIncumbentValue());
                }
            }

            logger.debug ( "Number of reamining partitions is "+ numRemainingPartitions);                

            //do another iteration involving every partition, unless we are done
            if (ZERO==numRemainingPartitions)  break;
            
        }//for greenFlagForIterations
        
        //print results
        numNodeRelaxationsSolved = ZERO;
        for (ActiveSubtreeCollection astc : activeSubtreeCollectionListCCA) {
            numNodeRelaxationsSolved += astc.getNumNodeRelaxationsSolved();
        } 
        logger.debug(" CB test ended at iteration Number "+iterationNumber + " with incumbent "+incumbentValue+
                 " and number of leafs solved " + numNodeRelaxationsSolved);
         
        
        
        
        logger.info("all parts of the test completed");
        
    } //end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");
         
        return file.exists();
    }
    
    private static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }
    
    //simulate 1 map iteration on the cluster
    //there is one ActiveSubtreeCollection on each partition
    private static int simulateOneMapIteration (List<ActiveSubtreeCollection> activeSubtreeCollectionList, 
            NodeSelectionStartegyEnum nodeSelectionStrategy, Double  incumbentValue,
            boolean reincarnationFlag, List<CBInstructionTree> cbInstructionTreeList) throws Exception{

        int numRemainingPartitions = activeSubtreeCollectionList.size(); // = NUM_PARTITIONS
         
        //solve every partition for 3 minutes at a time
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                     

            long rawnodeCount = activeSubtreeCollectionList.get(partitionNumber).getPendingRawNodeCount();
            long treeCount = activeSubtreeCollectionList.get(partitionNumber).getNumTrees();

            if (rawnodeCount+treeCount==ZERO)  continue;

            logger.debug("Solving partition for "+ SOLUTION_CYCLE_TIME_MINUTES+" minutes " + 
                         " having " +rawnodeCount + " rawnodes and " + treeCount + " trees " + " ... Partition_" + partitionNumber );

            activeSubtreeCollectionList.get(partitionNumber).solve( true, SOLUTION_CYCLE_TIME_MINUTES  ,     
                        true,    TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE,  nodeSelectionStrategy ,
                        reincarnationFlag,  reincarnationFlag? cbInstructionTreeList.get(partitionNumber ):null);               
        }

        //if better solution found on any partition, update incumbent, and supply MIP start to every partition
        int partitionWithIncumbentUpdate = -ONE;
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ )/*]]*/{

            ActiveSubtreeCollection astc = activeSubtreeCollectionList.get(partitionNumber);
            Double challengerToIncumbent=astc.getIncumbentValue() ;

            if (  (!IS_MAXIMIZATION  && incumbentValue> challengerToIncumbent)  || (IS_MAXIMIZATION && incumbentValue< challengerToIncumbent) ) {     
                //bestKnownSolution =   solutionVector;
                incumbentValue =  challengerToIncumbent;
                partitionWithIncumbentUpdate= partitionNumber;
            }
        }

        //update the MIP start if needed
        if (partitionWithIncumbentUpdate>=ZERO){
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                      
                if (partitionNumber==partitionWithIncumbentUpdate) continue;
                activeSubtreeCollectionList.get(partitionNumber).setCutoff(incumbentValue );//   setMIPStart(bestKnownSolution );                   
            }
            logger.debug (" incumbent was updated to " + incumbentValue);
        }

        //if every partition is done, we can stop the iterations
                    
        //check all the  partitions
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){   
            if (activeSubtreeCollectionList.get(partitionNumber).getPendingRawNodeCount() + activeSubtreeCollectionList.get(partitionNumber).getNumTrees() ==ZERO) {
                logger.info("This partition is complete: " + partitionNumber);
                numRemainingPartitions --;
            }  
        }
        
        return numRemainingPartitions;

    }
    
    private static List< CCANode>   getRandomCCACandidates (ActiveSubtree activeSubtreeForRampUp, int NUM_PARTITIONS,
                                                            List<List<String>> listOfComponentLeafs) throws IloException{
        List<CCANode> result = new ArrayList<CCANode> ();
        
        //first get the list of leafs in the ramped up tree
        List<NodeAttachment> leafList = activeSubtreeForRampUp.getActiveLeafList();
                
        //now get NUM_PARTITIONS sublists
        // this is done by shuffling the leafs and then creating equal sized sub-lists
        //note the psuedo random seed for reproducability of the tests
        Collections.shuffle( leafList,  new Random((long) NUM_PARTITIONS));
         
        
        int subListSize = leafList.size()/NUM_PARTITIONS ;
        
        for  (int subListIndex = ZERO;leafList.size()>ZERO ; subListIndex++) {
            
            //remove subListSize leafs and add into this subList
            // in the last iteration, absorb the reaminder into th elast candidate
            if (leafList.size()-subListSize< subListSize) subListSize+=(leafList.size()-subListSize);
            
            List<String> subList = new ArrayList<String> ();
            int removeCount = subListSize;
            while (removeCount>ZERO && leafList.size()>ZERO) {
                subList.add(leafList.remove( -ONE + leafList.size()).nodeID);
                removeCount --;
            }
               
            //get the CCA node for this sublist of leafs
            result.add( activeSubtreeForRampUp.getCandidateCCANodes( subList).get(ZERO));
            //make a note of which leafs this CCA node represents
            listOfComponentLeafs.add(subList);
        }
                
        return result;
    }
}



