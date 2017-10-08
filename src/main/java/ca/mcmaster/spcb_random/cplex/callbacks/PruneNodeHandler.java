/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex.callbacks;

import static ca.mcmaster.spcb_random.ConstantsAndParameters.IS_MAXIMIZATION;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.LOG_FILE_EXTENSION;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.LOG_FOLDER;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.MINUS_INFINITY;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.PLUS_INFINITY;
import static ca.mcmaster.spcb_random.ConstantsAndParameters.ZERO;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class PruneNodeHandler extends IloCplex.NodeCallback {
    
    //purpose of this class is to select a node, if prune list is not empty
    //Lambs to the slaughter !
    
    private static Logger logger=Logger.getLogger(PruneNodeHandler.class);
         
    //list of nodes to be pruned
    public List<String> pruneList  ;
     
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+PruneNodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }          
    }
    
    public PruneNodeHandler (List<String> pruneList){
        this.  pruneList=  pruneList;
    }

    
    protected void main() throws IloException {
         
        if (pruneList!=null && pruneList.size()>ZERO) {
            long numLeafs = getNremainingNodes64();

            for (long index = ZERO; index < numLeafs; index++){
                if ( pruneList.contains(getNodeId(index).toString())){
                    //select this node
                    selectNode(index);
                    break;
                }
            }
        }
         
    }
    
}
