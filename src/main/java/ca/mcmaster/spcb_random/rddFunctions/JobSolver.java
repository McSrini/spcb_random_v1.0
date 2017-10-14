/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.rddFunctions;

import static ca.mcmaster.spcb_random.ConstantsAndParameters.*;
import ca.mcmaster.spcb_random.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spcb_random.cplex.NodeSelectionStartegyEnum; 
import ca.mcmaster.spcb_random.cplex.datatypes.ResultOfPartitionSolve;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 * solvers work assigned to a partition using cplex
 * solving strategies could be cb, or sbf, or bef
 *  
 * returns whether ASTC is completely solved
 * 
 */
public class JobSolver implements Function <ActiveSubtreeCollection, ResultOfPartitionSolve>{
    
    private Double cutoff;
    private  double solutionCycleTimeMinutes;
    private    double timeSlicePerTreeInMInutes ;
    private             NodeSelectionStartegyEnum nodeSelectionStartegy ;
    private  boolean reincarnationFlag;
    
    public JobSolver (Double cutoff,  double solutionCycleTimeMinutes,   double timeSlicePerTreeInMInutes ,  
            NodeSelectionStartegyEnum nodeSelectionStartegy , boolean reincarnationFlag){
        
        this.cutoff=cutoff;
        this.solutionCycleTimeMinutes=solutionCycleTimeMinutes;
        this.nodeSelectionStartegy=nodeSelectionStartegy;
        this.timeSlicePerTreeInMInutes=timeSlicePerTreeInMInutes;
        this.reincarnationFlag=reincarnationFlag;
         
                
    }
 
    public ResultOfPartitionSolve   call(ActiveSubtreeCollection astc) throws Exception {
        astc.setCutoff( cutoff);
        ResultOfPartitionSolve result = null;
        if (ZERO!=astc.getNumTrees()+astc.getPendingRawNodeCount()){
            result =astc.solve( solutionCycleTimeMinutes,     timeSlicePerTreeInMInutes , nodeSelectionStartegy ,   reincarnationFlag );
        }else {
            result =new ResultOfPartitionSolve(true, astc.getIncumbentValue());
        }
        return result;
    }
    
}
