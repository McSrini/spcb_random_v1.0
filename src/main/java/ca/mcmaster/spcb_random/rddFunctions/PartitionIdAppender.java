/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcb_random.rddFunctions;
 
import ca.mcmaster.spcb_random.cplex.datatypes.WorkAssignment;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 * 
 * used to create a pair RDD by extracting out the id
 * 
 */
public class PartitionIdAppender implements PairFunction <WorkAssignment, Integer, WorkAssignment>{
 
    public Tuple2<Integer, WorkAssignment> call(WorkAssignment wa) throws Exception {
        return new Tuple2<Integer, WorkAssignment> (wa.id, wa) ;
    }
 
     
    
}
