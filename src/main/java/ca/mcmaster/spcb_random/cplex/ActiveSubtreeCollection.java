/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex;

import static ca.mcmaster.spcb_random.ConstantsAndParameters.IS_MAXIMIZATION;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.LOG_FILE_EXTENSION;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.LOG_FOLDER;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.MINUS_INFINITY;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.ONE;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.PLUS_INFINITY;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.*;
import ca.mcmaster.spcb_random.cb.CBInstructionTree;
import ca.mcmaster.spcb_random.cca.CCANode;
import static ca.mcmaster.spcb_random.cplex.NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST;
import static ca.mcmaster.spcb_random.cplex.NodeSelectionStartegyEnum.STRICT_BEST_FIRST;
import ca.mcmaster.spcb_random.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spcb_random.cplex.datatypes.ResultOfPartitionSolve;
import ca.mcmaster.spcb_random.cplex.datatypes.SolutionVector;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.Status;
import java.io.File;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * rd-rplusc-21.mps was solved using round robin in             165395.2753
 * 
 */
public class ActiveSubtreeCollection {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
        
    private List<ActiveSubtree> activeSubTreeList = new ArrayList<ActiveSubtree>();
    private List<CCANode> rawNodeList = new ArrayList<CCANode>();
    //in case we have only CCA node to solve, we will also be getting CB instructions for it
    private CBInstructionTree cbInstruction = null;
    
    
    
    //record the branching instructions required to arrive at the subtree root node under which these CCA nodes lie
    //To promote a CCA node into an IloCplex, we need to apply these branching conditions and then the CCA branching conditions
    // the call is activeSubtree.mergeVarBounds(ccaNode,  instructionsFromOriginalMip, true);  
    private  List<BranchingInstruction> instructionsFromOriginalMIP ;
    
    private double incumbentLocal= IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    private SolutionVector incumbentSolution = null;
    
    //astc id
    private int PARTITION_ID;
    
    //keep track of max trees created in this collection during solution
    public    int maxTreesCreatedDuringSolution = ONE;
    public long numNodeRelaxationsSolvedByCompletedTrees=ZERO;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public ActiveSubtreeCollection (List<CCANode> ccaNodeList, CBInstructionTree cbInstruction , List<BranchingInstruction> instructionsFromOriginalMip, double cutoff, boolean useCutoff, int id) throws Exception {
        rawNodeList=ccaNodeList;
        this.cbInstruction= cbInstruction;
        
        this.instructionsFromOriginalMIP = instructionsFromOriginalMip;
        if (useCutoff) this.incumbentLocal= cutoff;
        //create 1 tree
        //this.promoteCCANodeIntoActiveSubtree( this.getRawNodeWithBestLPRelaxation(), false);
        
        PARTITION_ID=id;
    }
    
    public void setCutoff (double cutoff) {
        this.incumbentLocal= cutoff;
    }
    
    public void setMIPStart(SolutionVector solutionVector) throws IloException {
        for (ActiveSubtree ast: activeSubTreeList){
            ast.setMIPStart(solutionVector);
        }        
    }
    


    //calculate MIP gap using global incumbent, which will be updated as this collection' s incumbent, and the best LP relax value
    //invoke this method only if computation has an incumbent
    public double getRelativeMIPGapPercent ()  {
        double result = -ONE;
        
        try {
            double bestInteger=this.incumbentLocal;
            double bestBound = this.getBestReaminingLPRElaxValue() ;

            double relativeMIPGap =  bestBound - bestInteger ;        
            if (! IS_MAXIMIZATION)  {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestInteger  ));
            } else {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestBound));
            }

            result = Math.abs(relativeMIPGap)*HUNDRED;
        }catch (Exception ex){
            logger.error("Error calculating mipgap "+ ex.getMessage() );
        }
        
        return  result;
    }
    
    public  long getNumActiveLeafs () throws IloException {
        long count = ZERO;
         
        for (ActiveSubtree ast : this.activeSubTreeList){
            count += ast.numActiveLeafsAfterSimpleSolve;
        }
        return count;
    }
    public  long getNumActiveLeafsWithGoodLP () throws IloException {
        long count = ZERO;
         
        for (ActiveSubtree ast : this.activeSubTreeList){
            count += ast.numActiveLeafsWithGoodLPAfterSimpleSolve;
        }
        return count;
    }
        

    
    public void endAll(){
        for (ActiveSubtree ast : this.activeSubTreeList){
            ast.end();
        }
    }
     
    //reincarnate flag is used to reincarnate the cca node into its component leafs using controlled branching
    //use this flag only when not using round-robin
    public ResultOfPartitionSolve solve ( double timeLimitMinutes,   double timeSlicePerTreeInMInutes ,  
            NodeSelectionStartegyEnum nodeSelectionStartegy , boolean reincarnationFlag ) throws Exception {
        logger.info(" \n solving ActiveSubtree Collection ... " + PARTITION_ID); 
        Instant startTime = Instant.now();
        
        
        while (activeSubTreeList.size()+ this.rawNodeList.size()>ZERO && Duration.between( startTime, Instant.now()).toMillis()< timeLimitMinutes*SIXTY*THOUSAND){
            
            double timeUsedUpMInutes = ( DOUBLE_ZERO+ Duration.between( startTime, Instant.now()).toMillis() ) / (SIXTY*THOUSAND) ;
                
            logger.info("time in seconds left = "+ (timeLimitMinutes -timeUsedUpMInutes)*SIXTY );
            
                        
            //pick tree with best lp
            ActiveSubtree tree = this.getTreeWithBestRemainingMetric(nodeSelectionStartegy );
            //pick raw node with best LP
            CCANode rawNode = this.getRawNodeWithBestMetric( nodeSelectionStartegy);
            //check if promotion required
            if (null != rawNode && tree !=null){
                if ((IS_MAXIMIZATION  && rawNode.lpRelaxationValue> tree.getBestRemaining_LPValue() )  || 
                    (!IS_MAXIMIZATION && rawNode.lpRelaxationValue< tree.getBestRemaining_LPValue() ) ){
                    //promotion needed
                    tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
                } 
            }else if (tree ==null){
                //promotion needed
                tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
            }else if (null==rawNode){
                //just solve the best tree available
            }

            //keep track of max trees created on this partition during solution
            maxTreesCreatedDuringSolution = Math.max(maxTreesCreatedDuringSolution ,  activeSubTreeList.size());
            
            
            //set best known LOCAL solution, if any, as MIP start
            if (incumbentLocal != MINUS_INFINITY  && incumbentLocal != PLUS_INFINITY){
                if (tree.isFeasible()){
                    if (  (IS_MAXIMIZATION  && incumbentLocal> tree.getObjectiveValue())  || (!IS_MAXIMIZATION && incumbentLocal< tree.getObjectiveValue()) ) {                
                        //tree.setMIPStart(incumbentSolution);
                        tree.setCutoffValue( incumbentLocal);
                    }
                } else{
                    //tree.setMIPStart(incumbentSolution);
                    tree.setCutoffValue( incumbentLocal);
                }
            }


            //solve for remaining time
            double timeSlice = timeSlicePerTreeInMInutes; //default
            if (  timeLimitMinutes -timeUsedUpMInutes < timeSlicePerTreeInMInutes ) {
                timeSlice= timeLimitMinutes -timeUsedUpMInutes;
                if (timeSlice < MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE) timeSlice = MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE; //15 second least count
            }
            if (timeSlice>ZERO) {                    
                if (reincarnationFlag) {
                   logger.info("Reincarnating tree seeded by cca node "+ tree.seedCCANodeID + " with " + tree.guid  );  
                   tree.reincarnate(this.cbInstruction.asMap(), tree.seedCCANodeID ,  PLUS_INFINITY , false );
                }else {
                   logger.info("Solving tree seeded by cca node "+ tree.seedCCANodeID + " with " + tree.guid  + " for minutes " +  timeSlice);  
                   //tree.simpleSolve(timeSlice,  true,   null); 
                   tree.traditionalSolve(timeSlice, COLLECT_ALL_METRICS,   COLLECT_NUM_NODES_SOLVED, SAVE_NODE_FILE_TO_DISK);
                }                   
            }
            
            //update LOCAL incumbent if needed            
            if (tree.isFeasible()|| tree.isOptimal()){
                double objVal =tree.getObjectiveValue();
                if ((IS_MAXIMIZATION && incumbentLocal< objVal)  || (!IS_MAXIMIZATION && incumbentLocal> objVal) ){
                    incumbentLocal = objVal;
                    this.incumbentSolution=tree.getSolutionVector();
                    logger.info("Incumbent updated to  "+ this.incumbentLocal + " by tree " + tree.guid + " on this partition " + PARTITION_ID);
                }
            }
            
            //remove   tree from list of jobs, if tree is solved to completion
            if (tree.isUnFeasible()|| tree.isOptimal()) {
                logger.info("Tree completed "+ tree.seedCCANodeID + ", " + tree.guid + ", " +   tree.getStatus()) ;
                numNodeRelaxationsSolvedByCompletedTrees += tree.getNumNodeRelaxationsSolved();
                tree.end();
                this.activeSubTreeList.remove( tree);                

            }           
            logger.info("Number of trees left is "+ this.activeSubTreeList.size());  
            printStatus();
            
            //if reincarnation complete, end solution cycle
            if (reincarnationFlag) break;
            
        } //while solution cycle still has time
        
        int countReamining = this.rawNodeList.size() + this.activeSubTreeList.size();
        if (ZERO==countReamining){
            logger.info(" ActiveSubtree Collection solved to completion  "+PARTITION_ID );
        } else{
            logger.info(" ActiveSubtree Collection solved for solution cycle time  "+PARTITION_ID );
        }
        
        return new ResultOfPartitionSolve (ZERO==countReamining , incumbentLocal) ;
    }
        
    public double getIncumbentValue (){
        return new Double (this.incumbentLocal);
    }
    
    
    
    public long getPendingRawNodeCount (){
        return this.rawNodeList.size();
    }
    
     
    
    public int getNumTrees() {
        return  activeSubTreeList.size();
    }
    
    public long getNumNodeRelaxationsSolved (){
        
        return numNodeRelaxationsSolvedByCompletedTrees;
    }
    
    private void printStatus() throws IloException {
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
            logger.debug( "Active tree " + activeSubtree.seedCCANodeID + ", " + activeSubtree.guid + ", " +   
                           activeSubtree.getStatus() +", BestRemaining_LPValue=" +activeSubtree.getBestRemaining_LPValue() +
                    " BestofBestEstimate="+activeSubtree.bestOFTheBestEstimates + " LowestSumofInfeasibilities="+ activeSubtree.lowestSumOFIntegerInfeasibilities);
        }
        logger.debug("Number of pending raw nodes " + getPendingRawNodeCount());
    }
    
    private double getBestReaminingLPRElaxValue () throws Exception{
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        
        ActiveSubtree tree= getTreeWithBestRemainingMetric(NodeSelectionStartegyEnum.STRICT_BEST_FIRST);
        if (tree!=null) bestReamining_LPValue =     tree.getBestRemaining_LPValue();
        
        CCANode rawNode = this.getRawNodeWithBestMetric(STRICT_BEST_FIRST);
        if( rawNode!=null){
            if (IS_MAXIMIZATION){
                bestReamining_LPValue = Math.max(bestReamining_LPValue, rawNode.lpRelaxationValue) ;
            }else {
                bestReamining_LPValue = Math.min(bestReamining_LPValue, rawNode.lpRelaxationValue) ;
            }
        }
        
        return     bestReamining_LPValue;
    }
    
    private ActiveSubtree getTreeWithBestRemainingMetric (NodeSelectionStartegyEnum strategyEnum) throws Exception{
                                                   
        double   bestReamining_metric =  NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) ? PLUS_INFINITY: (IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY);
        ActiveSubtree result = null;
        
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
            if (IS_MAXIMIZATION) {
                if (STRICT_BEST_FIRST.equals(strategyEnum) && bestReamining_metric<  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.getBestRemaining_LPValue();
                } else if ( BEST_ESTIMATE_FIRST.equals(strategyEnum) && bestReamining_metric<  activeSubtree.bestOFTheBestEstimates){
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.bestOFTheBestEstimates;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > activeSubtree.lowestSumOFIntegerInfeasibilities) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.lowestSumOFIntegerInfeasibilities;
                }
            }else {
                if (STRICT_BEST_FIRST.equals(strategyEnum) && bestReamining_metric>  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.getBestRemaining_LPValue();
                }else if ( BEST_ESTIMATE_FIRST.equals(strategyEnum) && bestReamining_metric>  activeSubtree.bestOFTheBestEstimates){
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.bestOFTheBestEstimates;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > activeSubtree.lowestSumOFIntegerInfeasibilities) {
                    result = activeSubtree;
                    bestReamining_metric=activeSubtree.lowestSumOFIntegerInfeasibilities;
                }
            }
          
        }
        return result;
    }
    
    private CCANode getRawNodeWithBestMetric  (NodeSelectionStartegyEnum strategyEnum) {
        double   bestReamining_metric =  NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) ? PLUS_INFINITY: (IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY);
        CCANode result = null;
        
        for (CCANode ccaNode : this.rawNodeList){
            if (IS_MAXIMIZATION) {
                if (STRICT_BEST_FIRST.equals(strategyEnum) &&  bestReamining_metric<  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.lpRelaxationValue;
                } else if (NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST .equals(strategyEnum) &&  bestReamining_metric<  ccaNode.bestEstimateValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.bestEstimateValue;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > ccaNode.sumOfIntegerInfeasibilities) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.sumOfIntegerInfeasibilities;
                }
            }else {
                if (STRICT_BEST_FIRST.equals(strategyEnum) &&  bestReamining_metric>  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.lpRelaxationValue;
                }else if ( NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST .equals(strategyEnum) &&  bestReamining_metric>  ccaNode.bestEstimateValue) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.bestEstimateValue;
                } else if (NodeSelectionStartegyEnum.LOWEST_SUM_INFEASIBILITY_FIRST.equals(strategyEnum ) && bestReamining_metric > ccaNode.sumOfIntegerInfeasibilities) {
                    result = ccaNode;
                    bestReamining_metric=ccaNode.sumOfIntegerInfeasibilities;
                }
            }
            
        }
        
        return result;
    }
    
    //remove cca node from raw node list and promote it into an active subtree.
    private ActiveSubtree promoteCCANodeIntoActiveSubtree (CCANode ccaNode, boolean useBranch) throws Exception{
        ActiveSubtree activeSubtree  = new ActiveSubtree () ;
        activeSubtree.mergeVarBounds(ccaNode,  this.instructionsFromOriginalMIP, useBranch);  
        activeSubTreeList.add(activeSubtree);      
        this.rawNodeList.remove( ccaNode);
        logger.debug ("promoted raw node "+ ccaNode.nodeID +" into tree"+ activeSubtree.guid) ;
        return activeSubtree;
    }
   
     
}
