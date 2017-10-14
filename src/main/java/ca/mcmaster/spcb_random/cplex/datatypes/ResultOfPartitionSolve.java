/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.cplex.datatypes;

import java.io.Serializable;

/**
 *
 * @author tamvadss
 * 
 * return value of solving a partition
 * 
 */
public class ResultOfPartitionSolve implements Serializable {
    public Boolean isComplete;
    public Double bestKnownSolution ;
    public ResultOfPartitionSolve (Boolean isComplete,      Double localIncumbent){
        this.isComplete=isComplete;
        this.bestKnownSolution=localIncumbent;
    }
}
