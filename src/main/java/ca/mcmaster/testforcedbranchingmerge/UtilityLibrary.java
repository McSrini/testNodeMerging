package ca.mcmaster.testforcedbranchingmerge;

 
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import java.util.*;
import java.util.Map.Entry;  

public class UtilityLibrary {
    
   
     public static NodeAttachment createChildNode (NodeAttachment parentNode, BranchDirection[ ] directionArray, 
            double[ ] boundArray, String[] varNamesArray  ) {

        //depth of child is 1 more than parent
         
        NodeAttachment child =new NodeAttachment (    parentNode.branchingVariableUpperBounds, 
                parentNode.branchingVariableLowerBounds    ) ;            
                
        //now apply the new bounds to the existing bounds
        for (int index = 0 ; index < varNamesArray.length; index ++) {                           
            mergeBound(child, varNamesArray[index], boundArray[index] , 
                    directionArray[index].equals(BranchDirection.Down));
        }

        return child;
    }   
    
    public static NodeAttachment createChildNode (NodeAttachment parentNode, BranchDirection[ ] directionArray, 
            double[ ] boundArray, IloNumVar[] varArray  ) {

        //depth of child is 1 more than parent
         
        NodeAttachment child =new NodeAttachment (    parentNode.branchingVariableUpperBounds, 
                parentNode.branchingVariableLowerBounds    ) ;            
                
        //now apply the new bounds to the existing bounds
        for (int index = 0 ; index < varArray.length; index ++) {                           
            mergeBound(child, varArray[index].getName(), boundArray[index] , 
                    directionArray[index].equals(BranchDirection.Down));
        }

        return child;
    }
    
    public static boolean mergeBound(NodeAttachment node, String varName, double value, boolean isUpperBound) {
        boolean isMerged = false;

        if (isUpperBound){
            Map< String, Double >  upperBounds = node.branchingVariableUpperBounds ;
            if (upperBounds.containsKey(varName)) {
                if (value < upperBounds.get(varName)){
                    //update the more restrictive upper bound
                    upperBounds.put(varName, value);
                    isMerged = true;
                }
            }else {
                //add the bound
                upperBounds.put(varName, value);
                isMerged = true;
            }
        } else {
            //it is a lower bound
            Map< String, Double >  lowerBounds = node.branchingVariableLowerBounds ;
            if (lowerBounds.containsKey(varName)) {
                if (value > lowerBounds.get(varName)){
                    //update the more restrictive lower bound
                    lowerBounds.put(varName, value);
                    isMerged = true;
                }               
            }else {
                //add the bound
                lowerBounds.put(varName, value);
                isMerged = true;
            }
        }

        return isMerged;
    }
    
    /**
     * 
     * To the CPLEX object ,  apply all the bounds mentioned in attachment
     */
    /*
    public static void  merge ( IloCplex cplex, NodeAttachment attachment   ) throws IloException {

        IloLPMatrix lpMatrix = (IloLPMatrix) cplex .LPMatrixIterator().next();

        //WARNING : we assume that every variable appears in at least 1 constraint or variable bound
        IloNumVar[] variables = lpMatrix.getNumVars();

        for (int index = 0 ; index <variables.length; index ++ ){

            IloNumVar thisVar = variables[index];
            updateVariableBounds(thisVar,attachment.branchingVariableLowerBounds, false );
            updateVariableBounds(thisVar,attachment.branchingVariableUpperBounds, true );

        }       
    }*/
    
    /**
     * 
     *  Update variable bounds as specified    
     */
    /*
    public static   void updateVariableBounds(IloNumVar var, Map< String,Double > newBounds, boolean isUpperBound   ) 
            throws IloException{

        String varName = var.getName();
        boolean isPresentInNewBounds = newBounds.containsKey(varName);

        if (isPresentInNewBounds) {
            double newBound =   newBounds.get(varName)  ;
            if (isUpperBound){
                if ( var.getUB() > newBound ){
                    //update the more restrictive upper bound
                    var.setUB( newBound );
                }
            }else{
                if ( var.getLB() < newBound){
                    //update the more restrictive lower bound
                    var.setLB(newBound);
                }
            }               
        }

    }  */

}
