/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex;

import ca.mcmaster.spcb_random.cplex.callbacks.PruneBranchHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.LeafCountingNodeHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.RampUpNodeHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.ReincarnationNodeHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.ReincarnationBranchHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.BranchHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.MergeBranchHandler;
import ca.mcmaster.spcb_random.cplex.callbacks.LeafFetchingNodeHandler;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.*;
import ca.mcmaster.spcb_random.cb.CBInstructionGenerator;
import ca.mcmaster.spcb_random.cca.CCAFinder;
import ca.mcmaster.spcb_random.cca.CCANode;
import ca.mcmaster.spcb_random.cb.CBInstructionTree;
import ca.mcmaster.spcb_random.cb.ReincarnationMaps;
import ca.mcmaster.spcb_random.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spcb_random.cplex.datatypes.NodeAttachment;
import ca.mcmaster.spcb_random.cplex.datatypes.SolutionVector;
import static ca.mcmaster.spcb_random.utilities.BranchHandlerUtilities.getLowerBounds;
import static ca.mcmaster.spcb_random.utilities.BranchHandlerUtilities.getUpperBounds;
import static ca.mcmaster.spcb_random.utilities.CCAUtilities.getBranchingInstructionForCCANode;
import static ca.mcmaster.spcb_random.utilities.CCAUtilities.getCCANodeLPRelaxValue;
import ca.mcmaster.spcb_random.utilities.CplexUtilities;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.Status.*;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
        
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
        
    private IloCplex cplex   ;
    private boolean isEnded = false;
    private double lpRelaxValueAfterCCAMerge ;
    
    //vars in the model
    private IloNumVar[]  modelVars;
        
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private RampUpNodeHandler rampUpNodeHandler;
    private LeafFetchingNodeHandler leafFetchNodeHandler;
    private ReincarnationNodeHandler reincarnationNodeHandler;
    private ReincarnationBranchHandler reincarnationBranchHandler;
    private PruneBranchHandler pruneBranchHandler;
    
    //our list of active leafs after each solve cycle
    private List<NodeAttachment> allActiveLeafs  ;     
    
    //use this object to run CCA algorithms
    private CCAFinder ccaFinder =new CCAFinder();
    
    private CBInstructionGenerator cbInstructionGenerator ;
    
    //this IloCplex object, if constructed by merging variable bounds, is differnt from the original MIP by these bounds
    //When extracting a CCA node from this Active Subtree , keep in mind that the CCA node branching instructions should be combined with these instructions
    public List<BranchingInstruction> instructionsFromOriginalMip = new ArrayList<BranchingInstruction>();
    
    public final String guid =  UUID.randomUUID().toString();
    public String seedCCANodeID = MINUS_ONE_STRING; // this will change if this subtree ws created by importing a CCA , it is used for logging   

    //temporarily, I am introducing these two variables which are used for statistics
    public long numActiveLeafsAfterSimpleSolve=ZERO ;
    public long numActiveLeafsWithGoodLPAfterSimpleSolve=ZERO ;
    public double bestOFTheBestEstimates = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    public double lowestSumOFIntegerInfeasibilities = PLUS_INFINITY;
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public ActiveSubtree (  ) throws Exception{
        
        this.cplex= new IloCplex();   
        cplex.importModel(MPS_FILE_ON_DISK);
        
        this.modelVars=CplexUtilities.getVariablesInModel(cplex);
        
        //create all the call back handlers
        //these are used depending on which method is invoked
        
        branchHandler = new BranchHandler(    );
        rampUpNodeHandler = new RampUpNodeHandler(    );                
        leafFetchNodeHandler = new LeafFetchingNodeHandler(); 
    
    }
    
    public void end(){
        if (!this.isEnded)     {
            this.cplex.end();
            isEnded=true;
        }
    }
    
    
    //create sub problem by changing var bounds
    public void mergeVarBounds (CCANode ccaNode, List<BranchingInstruction> instructionsFromOriginalMip, boolean useBranching) throws IloException {
        List<BranchingInstruction> cumulativeInstructions = new ArrayList<BranchingInstruction>();
        cumulativeInstructions.addAll(ccaNode.branchingInstructionList);
        cumulativeInstructions.addAll(instructionsFromOriginalMip);
        
        this.instructionsFromOriginalMip =cumulativeInstructions;
        this.lpRelaxValueAfterCCAMerge= ccaNode.lpRelaxationValue;
        this.seedCCANodeID = ccaNode.nodeID;
         
        if (useBranching){
            //create sub problem by doing a single branch
            solveForMergingCCA ( cumulativeInstructions,   ccaNode.lpRelaxationValue) ; 
        } else {
            //merge var bounds
            Map< String, Double >   lowerBounds= getLowerBounds(cumulativeInstructions, ccaNode.nodeID);
            Map< String, Double >   upperBounds= getUpperBounds(cumulativeInstructions, ccaNode.nodeID);
            CplexUtilities.merge(cplex, lowerBounds, upperBounds);
        }
    }
        
    public boolean isFeasible () throws IloException {
        return this.cplex.getStatus().equals(Feasible);
    }
    
    public boolean isUnFeasible () throws IloException {
        return this.cplex.getStatus().equals(Infeasible);
    }
        
    public boolean isUnknown () throws IloException {
        return this.cplex.getStatus().equals(Unknown);
    }
        
    public boolean isOptimal () throws IloException {
        return this.cplex.getStatus().equals(Optimal);
    }
    
    public String getStatus () throws IloException {
        return this.cplex.getStatus().toString();
    }
    
    public double getObjectiveValue() throws IloException {
        return this.cplex.getObjValue();
    }
    
    public void setMIPStart(SolutionVector solutionVector) throws IloException {
        cplex.addMIPStart(modelVars, solutionVector.values);
    }
    
    public SolutionVector getSolutionVector() throws IloException {
        
/*80     */   

        SolutionVector  solutionVector= new SolutionVector();
        
        double[] variableValues = cplex.getValues(modelVars);                 

        for ( int index = ZERO; index < variableValues.length; index ++){

            String varName = modelVars[index].getName();            
            solutionVector.add (varName );

        }
        solutionVector.setvalues(variableValues);
        
        return solutionVector;
    }
    
    //a temporary measure for checking ramp ups identical
    public List<String> getNodeCreationInfoList (){
        return this.branchHandler.nodeCreationInfoList;
    }
    public int getMaxBranchingVars () {
        return this.branchHandler.maxBranchingVars;
    }
    
    public List<NodeAttachment> getActiveLeafList() throws IloException {
        return allActiveLeafs ==null? null: Collections.unmodifiableList(allActiveLeafs) ;
    }
    //return # of leafs with superior lp, better than threshold
    public long getActiveLeafCountLP(double treshold) throws IloException {
        long count =  ZERO;
        if(allActiveLeafs !=null){
            for(NodeAttachment node: allActiveLeafs){
                if(IS_MAXIMIZATION){
                    if (node.estimatedLPRelaxationValue>=treshold ) count++;
                }else{
                    if (node.estimatedLPRelaxationValue<=treshold ) count++;
                }
            }
        }
        return count;
    }    
    public long getActiveLeafCount() throws IloException {
         
        return allActiveLeafs ==null? ZERO: allActiveLeafs.size();
    }
    
    //solve using traditional bnb
    //removing callbacks which were used to collect statistics, because with disk saving of nodes, these can cause crashes
    public void traditionalSolve(double timeLimitMinutes, boolean collect_LSI_BEF_Metrics, boolean countNumLeafsSolved, int saveToDisk) throws IloException{
        
        logger.debug("Traditional Solve Started at "+LocalDateTime.now()) ;
        cplex.clearCallbacks();
        
        if (countNumLeafsSolved){
            pruneBranchHandler =new PruneBranchHandler( new ArrayList<String>());
            this.cplex.use(pruneBranchHandler);
            //this.cplex.use(new PruneNodeHandler( pruneList));
        }
        
        setTimeLimitMinutes (  timeLimitMinutes);
        
        cplex.setParam(IloCplex.Param.MIP.Strategy.Search, ONE);
        
        if (saveToDisk>= ZERO) {
            cplex.setParam(IloCplex.Param.MIP.Strategy.File, saveToDisk);
            cplex.setParam(IloCplex.Param.WorkMem,  WORK_MEM);    //low mem!
        }     
        
        cplex.solve();
        
        if (collect_LSI_BEF_Metrics) {
                  
            //get leafs 
                  
            LeafCountingNodeHandler lcnh = new LeafCountingNodeHandler(MIP_WELLKNOWN_SOLUTION);
            this.cplex.use(lcnh);  
            cplex.solve();

            //this.allActiveLeafs= lcnh.allLeafs;

            numActiveLeafsAfterSimpleSolve =lcnh.numLeafs;
            numActiveLeafsWithGoodLPAfterSimpleSolve =lcnh.numLeafsWithGoodLP;
            this.bestOFTheBestEstimates = lcnh.bestOFTheBestEstimates;
            this.lowestSumOFIntegerInfeasibilities = lcnh.lowestSumOFIntegerInfeasibilities;

        }
        
        logger.debug("Traditional Solve completed at "+LocalDateTime.now()) ;
        
    }   
    
    //to do : add method to supply MIP starts , and use it while doing round robin and even otherwise
    //we can do this by supplying start vector, or export/import of MIP starts to/from a file
        
    public void solve(long leafCountLimit, double cutoff, int timeLimitMinutes, boolean isRampUp, boolean setCutoff) throws IloException{
        
        logger.debug(" Solve begins at "+LocalDateTime.now()) ;
        
        //before solving , reset the CCA finder object which 
        //has an index built upon the current solution tree nodes
        this.ccaFinder.close();
        
        //set callbacks for regular solution
        cplex.clearCallbacks();
        this.cplex.use(branchHandler);
        
        if (isRampUp) {
            rampUpNodeHandler.setLeafCountLimit(leafCountLimit);
            this.cplex.use(rampUpNodeHandler) ;
        }  
        
        
        if (setCutoff) setCutoffValue(  cutoff);
        setTimeLimitMinutes   (  timeLimitMinutes );
              
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, TWO);  //low mem   
        cplex.solve();
        
        //solve complete - now get the active leafs
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
        
        logger.debug(" Solve concludes at "+LocalDateTime.now()) ;
        
    }
    
    //invoke this method only if feasible solution exists
    public double getRelativeMIPGapPercent (boolean useGlobalIncombentValue, double globalIncombentValue) throws IloException {
        double bestInteger=useGlobalIncombentValue? globalIncombentValue : cplex.getObjValue();
        double bestBound = this.cplex.getBestObjValue();
        
       double relativeMIPGap =  bestBound - bestInteger ;        
        if (! IS_MAXIMIZATION)  {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestInteger  ));
        } else {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestBound));
        }
        
        return Math.abs(relativeMIPGap)*HUNDRED ;
    }
 
    //this method is used when reincarnating a tree in a controlled fashion
    //similar to solve(), but we use controlled branching instead of CPLEX default branching
    public    void reincarnate ( Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID, double cutoff, boolean setCutoff) throws IloException{
        
        logger.debug("Reincarnating tree with cca root node id "+ ccaRootNodeID);
        
        //reset CCA finder
        this.ccaFinder.close();
        
        //set callbacks 
        ReincarnationMaps reincarnationMaps=createReincarnationMaps(instructionTreeAsMap,ccaRootNodeID);
        this.cplex.use( new ReincarnationBranchHandler(instructionTreeAsMap,  reincarnationMaps, this.modelVars));
        this.cplex.use( new ReincarnationNodeHandler(reincarnationMaps));      
        
        if (setCutoff) setCutoffValue(  cutoff);
        setTimeLimitMinutes (  -ONE);//no time limit
         
        cplex.solve();
        
        //solve complete - now get the active leafs
        //restore regular branch handler
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
    }
    
    public void prune(List<String> pruneList, boolean reInitializeCCAFinder) {
        //close CCA finder
        if (reInitializeCCAFinder) this.ccaFinder.close();
        
        //update allActiveLeafs
        List<NodeAttachment> newActiveLeafs = new ArrayList<NodeAttachment> ();
        for (NodeAttachment currentLeaf : this.allActiveLeafs){
            if (!pruneList.contains(currentLeaf.nodeID) ) newActiveLeafs.add(currentLeaf);
        }
        allActiveLeafs=newActiveLeafs;
        
        //these will be removed from the IloCplex object
        this.branchHandler.pruneList.addAll(pruneList);
                
        //re-init the CCA finder
        if (reInitializeCCAFinder) ccaFinder .initialize(allActiveLeafs);
    }
    
 
    //use this method to farm out selected leaf nodes, supply null argument to select all
    //
    //This method is used for testing purposes, to show that selecting individual leafs for 
    //migration is a bad idea, we should always use CCA nodes for migration
    public List<CCANode> getActiveLeafsAsCCANodes (List<String> wantedLeafNodeIDs) {
        List <CCANode> ccaNodeList = new ArrayList <CCANode> ();
        
        for (NodeAttachment leaf : this.allActiveLeafs) {
            if (wantedLeafNodeIDs!=null && !wantedLeafNodeIDs.contains( leaf.nodeID))continue ;
            CCANode ccaNode = new CCANode();   
            leaf.ccaInformation=ccaNode;
            leaf.ccaInformation.nodeID= leaf.nodeID;
            getBranchingInstructionForCCANode( leaf);
                        
            getCCANodeLPRelaxValue(leaf);
            
            //populate CCA node with best-estimate and sum-of-infeasibilities
            //this is not done non-leaf regular CCA nodes
            ccaNode.sumOfIntegerInfeasibilities = leaf.sumOfIntegerInfeasibilities;
            ccaNode.bestEstimateValue= leaf.bestEstimateValue;
            logger.debug(" cca node properties for round robin LP BE SI "+ ccaNode.lpRelaxationValue + ", "+ 
                         ccaNode.bestEstimateValue + ", "+ccaNode.sumOfIntegerInfeasibilities );
            
            ccaNodeList.add( ccaNode);
        }
        
        return ccaNodeList;
    }
    
    //if wanted leafs are not specified, every migratable leaf under this CCA is assumed to be wanted
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode ) {
        List<String> wantedLeafs = new ArrayList<String> ();
        for (NodeAttachment node :  this.allActiveLeafs){
            if (ccaNode.pruneList.contains(node.nodeID) && node.isMigrateable) wantedLeafs.add(node.nodeID);
        }
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
        
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode, List<String> wantedLeafs) {
        
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
 
    public List<CCANode> getCandidateCCANodes (List<String> wantedLeafNodeIDs)   {         
        return ccaFinder.  getCandidateCCANodes ( wantedLeafNodeIDs);       
    }
                                                                                                             
    public List<CCANode> getCandidateCCANodes (int count)   {
        return ccaFinder.  getCandidateCCANodes ( count);        
    }    
        
    //use this method to split the ramped-up tree into roughly equal partitions
    public List<CCANode> getCandidateCCANodesPostRampup (int numPartitions)   {
        return getCandidateCCANodesPostRampup (  numPartitions, TWO)  ;
    }
    public List<CCANode> getCandidateCCANodesPostRampup (int numPartitions, final int MIN_REF_COUNT)   {
        return ccaFinder.  getCandidateCCANodesPostRampup ( numPartitions, MIN_REF_COUNT);        
    }    
    
    
    public List<String> getPruneList ( CCANode ccaNode) {
        return ccaNode.pruneList;
    }
         
    public void setCutoffValue(double cutoff) throws IloException {
        boolean conditionMin = !IS_MAXIMIZATION && cutoff <  cplex.getParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff );
        boolean conditionMax = IS_MAXIMIZATION  && cutoff >  cplex.getParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff );
        
        double currentCutoff = cplex.getParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff );
        if (IS_MAXIMIZATION ) currentCutoff = cplex.getParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff );
        
        logger.warn ("currentCutoff " + currentCutoff + " cutoff " + cutoff + " conditionMin "+ conditionMin) ;
        
        if (conditionMin || conditionMax ) {
            logger.warn ( "Setting cuttoff to "+ cutoff + " for tree " + this.guid + " from current cutoff " + currentCutoff);
        }
        
        if (conditionMin) {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
        }else if (conditionMax){
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff, cutoff);
        }
    }
    
    public void setTimeLimitMinutes (double timeLimitMinutes ) throws IloException {
        
        if (timeLimitMinutes>ZERO) {
            cplex.setParam(IloCplex.Param.TimeLimit, timeLimitMinutes*SIXTY);
        }                else cplex.setParam(IloCplex.Param.TimeLimit, HUNDRED*SIXTY);
         
    }
    
    public double getBestRemaining_LPValue() throws IloException{
        return this.cplex.getBestObjValue();
        //return this.allActiveLeafs==null? this.lpRelaxValueAfterCCAMerge: this.branchHandler.bestReamining_LPValue;
    }
    
    private  ReincarnationMaps createReincarnationMaps (Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID){
        ReincarnationMaps   maps = new ReincarnationMaps ();
                
        for (String key : instructionTreeAsMap.keySet()){
            if (instructionTreeAsMap.get(key).leftChildNodeID!=null){
                //  this  needs to be branched upon , using the branching instructions in the CCA node
                maps.oldToNew_NodeId_Map.put( key,null  );
            }
        }
        
        //both maps can start with original MIP which is always node ID -1
        //but right now we are starting from the root CCA
        maps.oldToNew_NodeId_Map.put( ccaRootNodeID ,MINUS_ONE_STRING  );
        maps.newToOld_NodeId_Map.put( MINUS_ONE_STRING,ccaRootNodeID  );
        
        return maps;
    }
    
    //use the merge branch callback to create a single child 
    public void solveForMergingCCA (List<BranchingInstruction> cumulativeInstructions, double lpRelaxValue) throws IloException {
        
        MergeBranchHandler mbh = new MergeBranchHandler (   cumulativeInstructions, this.  modelVars,   lpRelaxValue) ;
        cplex.clearCallbacks();
        
        this.cplex.use(mbh);
        cplex.solve();
    }
    
    //let us print the number of node relaxations solved in this tree
    public long getNumNodeRelaxationsSolved (){
        return pruneBranchHandler !=null ? this.pruneBranchHandler.numNodeRelaxationsSolved : ZERO;
    }
 
}
