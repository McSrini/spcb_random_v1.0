/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.rddFunctions;

import static ca.mcmaster.spcb_random.ConstantsAndParameters.ZERO; 
import ca.mcmaster.spcb_random.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spcb_random.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spcb_random.cplex.datatypes.WorkAssignment;
import java.util.List;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 * 
 * takes a cca node and an optional cb intsruction pair 
 * and creates a active subtree collection out of it
 * 
 */
public class ActiveSubtreeCollectionCreator implements Function <WorkAssignment, ActiveSubtreeCollection>{
    
    private List<BranchingInstruction> instructionsFromOriginalMip;
    private  double cutoff;
    private boolean useCutoff;
    
    public ActiveSubtreeCollectionCreator(List<BranchingInstruction> instructionsFromOriginalMip,  double cutoff, boolean useCutoff) {
        this.instructionsFromOriginalMip = instructionsFromOriginalMip;
        this.useCutoff=   useCutoff;
        this. cutoff= cutoff;
    }
 
    public ActiveSubtreeCollection call(WorkAssignment wa) throws Exception {
                
        ActiveSubtreeCollection astc = new   ActiveSubtreeCollection (wa.ccaNodeList, wa.cbInstructionTree,
                                                                      instructionsFromOriginalMip, cutoff,  useCutoff, wa.id);
        
        return astc;
    }
 
    
    
}
