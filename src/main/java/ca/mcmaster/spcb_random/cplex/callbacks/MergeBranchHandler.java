/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex.callbacks;

import ca.mcmaster.spcb_random.utilities.BranchHandlerUtilities;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.*;
import ca.mcmaster.spcb_random.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spcb_random.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class MergeBranchHandler extends IloCplex.BranchCallback {
    
    private static Logger logger=Logger.getLogger(MergeBranchHandler.class);
    
    private IloNumVar[]  modelVars;
    private double lpRelaxValue;
          
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    //here are the instructions for the child node we want to create
    public List<BranchingInstruction> cumulativeInstructions ;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+MergeBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
    
    public MergeBranchHandler ( List<BranchingInstruction> cumulativeInstructions, IloNumVar[]  modelVars, double lpRelaxValue) {
        this. cumulativeInstructions =   cumulativeInstructions;
        this.modelVars= modelVars;
    }
 
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);                
                
            } 
             
            //now create the 1 kid we want to create
            for (int childNum = ZERO ;childNum<ONE;  childNum++) {   

                //apply the bound changes specific to this child

                //first create the child node attachment
                NodeAttachment thisChild  =  new NodeAttachment (); 
                
                /* do not populate parent data and depth
                thisChild.parentData = nodeData;
                thisChild.depthFromSubtreeRoot=nodeData.depthFromSubtreeRoot + ONE;
                */
                
                //convert branching instructions into cplex format
                //                
                // branch about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                BranchHandlerUtilities.mergeBranchingInstructionIntoArray (  this.cumulativeInstructions , vars,   bounds ,  
                                                                                 dirs, childNum,  modelVars);

                //record child node ID
                IloCplex.NodeId nodeid = makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
                
                //let the node id stay at -1
                //thisChild.nodeID =nodeid.toString();
                
                thisChild.estimatedLPRelaxationValue = this.lpRelaxValue;

                logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " varname " +   vars[childNum][ZERO].getName() + " bound " + bounds[childNum][ZERO] +   (dirs[childNum][ZERO].equals( IloCplex.BranchDirection.Down) ? " U":" L") ) ;

            }//end for  
            
            this.bestReamining_LPValue = getBestObjValue();
            
            //we have created a single child, we are done
            abort();
            
        } // end if getNbranches()> 0
        
    }//end main
            
}
