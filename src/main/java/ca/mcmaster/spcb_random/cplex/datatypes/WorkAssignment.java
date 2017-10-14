/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex.datatypes;

import ca.mcmaster.spcb_random.cb.CBInstructionTree;
import ca.mcmaster.spcb_random.cca.CCANode;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * This is the work assignment to a partition.
 * This class is only used by the Spark driver right now - should be used by other drivers too.
 * 
 * We could have 1 or more leafs to solve
 * In case of 1 leaf, we could also have a CB instruction
 * 
 * 
 * Right now using CCA nodes, should change it to use Tree Nodes
 * 
 */
public class WorkAssignment    implements Serializable {
    
    public int id;
    public List<CCANode> ccaNodeList = new ArrayList<CCANode>();
    public  CBInstructionTree cbInstructionTree = null;
    
    public WorkAssignment (List<CCANode> ccaNodeList, int id ) {
        this.ccaNodeList.addAll(ccaNodeList);
        this.id = id; 
    }
    public WorkAssignment (CCANode ccaNode,   CBInstructionTree cbInstructionTree, int id) {
        this.ccaNodeList.add(ccaNode);
        this.cbInstructionTree= cbInstructionTree;
        this.id = id;
    }
}
