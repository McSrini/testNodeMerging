/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.testforcedbranchingmerge;

import static ca.mcmaster.spbinarytree.BinaryTree.EMPTY_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.MINUS_ONE_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
        
    public String nodeID;
    public Map< String, Double > branchingVariableUpperBounds  = new HashMap< String, Double >();
    public Map< String, Double > branchingVariableLowerBounds = new HashMap< String, Double >();
        
    public NodeAttachment (String id) {
        nodeID = id;
    }
        
    public NodeAttachment (  Map< String, Double > branchingVariableUpperBounds, Map< String, Double > branchingVariableLowerBounds) {
                
        for (Map.Entry <String, Double> entry : branchingVariableUpperBounds.entrySet()){
            this.branchingVariableUpperBounds.put(entry.getKey(), entry.getValue()  );
        }
        for (Map.Entry <String, Double> entry : branchingVariableLowerBounds.entrySet()){
            this.branchingVariableLowerBounds.put(entry.getKey(), entry.getValue()  );
        }
         
    }
    
    public String toString (){
        String result = EMPTY_STRING ;
        for (Map.Entry<String ,Double> entry : branchingVariableUpperBounds.entrySet()  ){
             result += "("+entry.getKey() + "," +entry.getValue()+ ","+ONE +") ";
        }
        for (Map.Entry<String ,Double> entry : branchingVariableLowerBounds.entrySet()  ){
             result += "("+entry.getKey() + "," +entry.getValue()+ ","+ZERO +") ";
        }
        return result;
    }
}
