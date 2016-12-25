/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.testforcedbranchingmerge;

import ca.mcmaster.spbinarytree.BinaryTree;
import static ca.mcmaster.spbinarytree.BinaryTree.MINUS_ONE_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.*;
import ca.mcmaster.spbinarytree.BranchingInstruction;
import ca.mcmaster.spbinarytree.BranchingInstructionNode;
import ca.mcmaster.spbinarytree.BranchingInstructionTree;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.NodeId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.*;

/**
 *
 * @author tamvadss
 */
public class BranchHandler extends BranchCallback{
    
    private List<String> pruneList ;
    private BinaryTree mirrorTree;
    private List <BranchingInstructionTree> instructionTrees ;  
    
    private BranchingInstructionTree iTree;
    private Map<String, String> newToOldMap;
    private Map<String, String> oldToNewMap;
     
    private static Logger logger=Logger.getLogger(BranchHandler.class);
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
    
    public BranchHandler (List<String> pruneList, BinaryTree mirrorTree, List <BranchingInstructionTree> instructionTrees,
        BranchingInstructionTree iTree,Map<String, String> newToOldMap,      Map<String, String> oldToNewMap ){
       
        this.pruneList = pruneList;
        this.  mirrorTree=  mirrorTree;
        this.instructionTrees = instructionTrees;
        
        this.iTree= iTree;
        this.newToOldMap=newToOldMap;
        this.oldToNewMap= oldToNewMap;
    }

    @Override
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
             
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (  MINUS_ONE_STRING   );  
                setNodeData(nodeData);
                
                //root node must have been selected
                mirrorTree.recordNodeSelectedForSolution(MINUS_ONE_STRING );
                
            } 
           
            if (pruneList.contains(nodeData.nodeID )) {
                prune();
            } else {
                
                if ( NodeHandler.isMergeComplete) {
                    //accumulate branching conditions
                    createTwoChildNodes(nodeData);
          
                } else {
                    CreateChildrenAsPerInstructions(nodeData);
                }
               
            }
            
        }
        
    }
        
    private void createTwoChildNodes(NodeAttachment parentNodeData) throws IloException{
        
        //First, append the 
        //branching conditions so we can pass them on to the kids

        //get the branches about to be created
        IloNumVar[][] vars = new IloNumVar[2][] ;
        double[ ][] bounds = new double[2 ][];
        IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ 2][];
        getBranches(  vars, bounds, dirs);
        
        //these two variables used for mirror tree construction
        List<String> childNodeIDList = new ArrayList<String> ();
        List<BranchingInstruction> branchingInstructions = new ArrayList<BranchingInstruction>();
 
        //now allow  both kids to spawn
        for (int childNum = 0 ;childNum<getNbranches();  childNum++) {    
            //apply the bound changes specific to this child
            NodeAttachment thisChild  = UtilityLibrary.createChildNode( parentNodeData,
                    dirs[childNum], bounds[childNum], vars[childNum]   ); 
           
            NodeId nodeid = makeBranch(childNum,thisChild );
            thisChild.nodeID =nodeid.toString();
            
            childNodeIDList.add(thisChild.nodeID);
            branchingInstructions.add( new BranchingInstruction(  gerVarnames(vars[childNum])  , getVarDirections(  dirs[childNum]),   bounds[childNum]));
            
            logger.info(" branched child "+ thisChild.nodeID);
            
        }//end for 2 kids
        
        mirrorTree.recordChildCreation( childNodeIDList , branchingInstructions);
        
    }
    
    private String[] gerVarnames (IloNumVar[]  vars) {
        String[]  names = new String [vars.length] ;
        
        int count = ZERO;
        for (IloNumVar var : vars){
            names [count++] = var.getName();
        }
        return names ;
    }
    
    private Boolean[] getVarDirections (BranchDirection[ ] dirArray) {
        Boolean[]  dirs = new Boolean [dirArray.length ] ;
         
        int count = ZERO;
        for (BranchDirection direction : dirArray){
            dirs [count++] = direction.equals(BranchDirection.Down );
        }
         
        return dirs ;
        
    }
    
        
    //create two child nodes, as per instructions for this node in iTree
    //update old to new map, and new to old map
    private void  CreateChildrenAsPerInstructions(NodeAttachment nodeData) throws IloException{
        
        String newNodeID = nodeData.nodeID;
        String oldNodeID = newToOldMap.get(newNodeID );
        //find the variables we should branch on
        BranchingInstructionNode instructionNode= iTree.nodeMap.get( oldNodeID);
                
        //these two variables used for mirror tree construction
        List<String> childNodeIDList = new ArrayList<String> ();
        List<BranchingInstruction> branchingInstructions = new ArrayList<BranchingInstruction>();
         
        //create the 2 kids by branching as instructed
        for (int index = ZERO; index < instructionNode.childList.size(); index ++){
            String oldChildNodeID = instructionNode.childList.get(index).nodeID;
            BranchingInstruction instruction = instructionNode.branchingInstructionList.get(index);
            
            //apply the bound changes specific to this child
            NodeAttachment thisChild  = UtilityLibrary.createChildNode( nodeData,
                    getDirectionOfBound(instruction.isBranchDirectionDown) , 
                    getBoundValues(instruction.varBounds) , 
                    getVarNames(instruction.varNames  )  ); 
           
            IloCplex.NodeId nodeid = makeBranch(index,thisChild );
            thisChild.nodeID =nodeid.toString();
            
            this.oldToNewMap.put( oldChildNodeID,  thisChild.nodeID );
            this.newToOldMap.put( thisChild.nodeID , oldChildNodeID );
            logger.info(oldChildNodeID + " and new child id " + thisChild.nodeID) ;
            
            childNodeIDList.add(thisChild.nodeID);
            branchingInstructions.add( new BranchingInstruction(   instruction ) );
             
        }
        
        mirrorTree.recordChildCreation( childNodeIDList , branchingInstructions);
    }
    
    private BranchDirection[] getDirectionOfBound ( List< Boolean > isBranchDirectionDown){
        BranchDirection[] dirs = new BranchDirection[isBranchDirectionDown.size()];
        for (int index = ZERO; index < isBranchDirectionDown.size(); index ++){
            dirs[index] = ( isBranchDirectionDown.get(index) ?  BranchDirection.Down : BranchDirection.Up) ;
        }
        return dirs ;
    }
    
    private double[] getBoundValues (List< Double>  varBounds) {
        double[] values = new double[varBounds.size()];
        for (int index = ZERO; index < varBounds.size(); index ++){
             values[index]=varBounds.get(index);
        }
        return values;
    }
    
    private String[] getVarNames (List<String>  varNames) {
        String[] varNamesArray = new String[varNames.size()];
        for (int index = ZERO; index < varNames.size(); index ++){
             varNamesArray[index]=varNames.get(index);
        }
        return varNamesArray;
    }

}
