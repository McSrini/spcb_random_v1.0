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
public class PruneBranchHandler extends IloCplex.BranchCallback {
    
    private static Logger logger=Logger.getLogger(PruneBranchHandler.class);
         
    //list of nodes to be pruned
    public List<String> pruneList  ;
    
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    //let us print the number of node relaxations solved in this tree
    public long numNodeRelaxationsSolved = ZERO;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+PruneBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
    
    public PruneBranchHandler (List<String> pruneList){
        this.  pruneList=  pruneList;
    }
 
    protected void main() throws IloException {
        
        //if we are here, we must be branching, i.e. node LP has been solved
        //However, we do not count leafs which are pruned, see later.
        numNodeRelaxationsSolved ++;
        
        if (pruneList!=null && pruneList.size()>ZERO){
            if (  pruneList.contains( getNodeId().toString()) ) {
                pruneList.remove(  getNodeId().toString() );
                //logger.debug("Pruning migrated node" + getNodeId().toString() + " prine list reamaining size "+pruneList.size()) ;
                prune();
                numNodeRelaxationsSolved --;
            } 
        }
        
    }//end main
    
 
}
