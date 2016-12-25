/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.testforcedbranchingmerge;

import ca.mcmaster.spbinarytree.BinaryTree;
import static ca.mcmaster.spbinarytree.BinaryTree.EMPTY_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.*;
import static ca.mcmaster.spbinarytree.BinaryTree.MINUS_ONE_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.ONE;
import static ca.mcmaster.spbinarytree.BinaryTree.ZERO;
import ca.mcmaster.spbinarytree.BranchingInstructionNode;
import ca.mcmaster.spbinarytree.BranchingInstructionTree;
import static ca.mcmaster.testforcedbranchingmerge.BranchHandler.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.NodeCallback;
import java.io.File;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class NodeHandler extends NodeCallback{
    
    private final int THRESHOLD = 200;
    private final int FARM_COUNT  = 10;
    private boolean isFarmingComplete = false;
    private List<String> pruneList ;
    
    private BinaryTree mirrorTree;
    private List <BranchingInstructionTree> instructionTrees ;  
    
    //for merging, which is always done expect before ramp up
    private BranchingInstructionTree iTree;
    private Map<String, String> newToOldMap;
    private Map<String, String> oldToNewMap;
    
    //this var needs to be shared with the branch handler somehow, here is a lousy way of doing it
    public static boolean isMergeComplete;
        
    private static Logger logger=Logger.getLogger(NodeHandler.class);
    
    private boolean stopTesting = false;
      
    static {
        logger.setLevel(Level.WARN);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+NodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
            
    public NodeHandler (List<String> pruneList, BinaryTree mirrorTree, List <BranchingInstructionTree> instructionTrees ,
            BranchingInstructionTree iTree,   Map<String, String> newToOldMap,   Map<String, String> oldToNewMap,  boolean isMergeComplete){
        
        this.pruneList = pruneList;
        this.  mirrorTree=  mirrorTree;
        this.instructionTrees = instructionTrees;
        
        this.iTree= iTree;
        this.newToOldMap=newToOldMap;
        this.oldToNewMap= oldToNewMap;
        //for ramp up, supply true for merge already complete
        this.isMergeComplete = isMergeComplete;
        
        //both old and new trees start with same root node - namely the original MIP
        if (! isMergeComplete){
            
            newToOldMap.clear();
            oldToNewMap.clear();
            newToOldMap.put (MINUS_ONE_STRING, MINUS_ONE_STRING) ;
            oldToNewMap.put (MINUS_ONE_STRING, MINUS_ONE_STRING) ;
            for (BranchingInstructionNode biNode : iTree.nodeMap.values()) {
                if (!biNode.nodeID.equals(MINUS_ONE_STRING)) oldToNewMap.put (biNode.nodeID, null) ;
            }
            
        }
        logger.info("");
    }

    @Override
    protected void main() throws IloException {
        
        if(getNremainingNodes64()> ZERO){
            
            if (isHaltFilePresent()) exit(1);
            
            logger.info("current active leaf count is "+getNremainingNodes64());
            
            NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO );
            if (nodeData==null ) { //it will be null for subtree root
                 
                nodeData=new NodeAttachment ( MINUS_ONE_STRING);
                setNodeData(ZERO ,nodeData) ;
                
            }
             
            // merge is complete when there is no old node which needs kids created, where the old node also
            // has a corresponding new node in the getRemainingNodes() of the merged tree
            //
            //migration must consider that some of the migrated nodes will get solved, or the entire tree will get 
            //solved, in which case we need not create any desecndant nodes of solved nodes.
            //
            //Our condition check takes care of both cases. Actually you wont be in the nodehandler if entire tree is solved, Main will report that 
            // the solve() method is complete
            //
            long selectedIndex = ZERO; //default node index, which we change only if we are merging
            if (!isMergeComplete){
                selectedIndex=checkMergeCompletion();
                if (  selectedIndex< ZERO  ) {
                    isMergeComplete= true;
                    stopTesting = true;
                    logger.info("Merge complete ... printing merged tree");
                    logger.info( this.printLeafs( new ArrayList<String> ()));
                }  
            }

            
            if (!isMergeComplete) {
                
                selectNode(selectedIndex);                             
                nodeData = (NodeAttachment) getNodeData(selectedIndex );
                            
            } else {
                //do not interfere with node selection
                if (stopTesting )   abort();
            }
            
            if (getNremainingNodes64()>=THRESHOLD && !isFarmingComplete && !stopTesting) {
                
                logger.info(" leafs before farming "+ printLeafs(pruneList));
                logger.info(EMPTY_STRING);
                logger.info( " mirror leafs " +this.mirrorTree.toString());
                logger.info(EMPTY_STRING);
                
                //farm 30 nodes
                for (int index = ZERO; index < FARM_COUNT; index ++){
                    pruneList.add( getNodeId(index).toString());
                }
                for (int index = ZERO; index < FARM_COUNT; index ++){
                    logger.info(" pruned leaf  "+ pruneList.get(index));
                }
                
                logger.warn("getting farming instructions for bathc 1 at " + LocalDateTime.now()) ;
                
                instructionTrees. add(mirrorTree.getInstructionForFarmingNodes(pruneList));
                
                logger.warn("completed farming instructions for bathc 1 at " + LocalDateTime.now()) ;
                 
                logger.info(instructionTrees.get(ZERO).toString());
                
                //farm 30 more
                for (int index = ZERO; index < FARM_COUNT; index ++){
                    pruneList.add( getNodeId(index+FARM_COUNT).toString());
                }
                for (int index = ZERO; index < FARM_COUNT; index ++){
                    logger.info(" 2nd batch pruned leaf  "+ pruneList.get(FARM_COUNT+index));
                }
                instructionTrees. add(mirrorTree.getInstructionForFarmingNodes(pruneList.subList(FARM_COUNT, FARM_COUNT+FARM_COUNT )));
                logger.info(instructionTrees.get(ONE).toString());
                
                isFarmingComplete= true;   
                logger.info(" leafs after farming "+ printLeafs(pruneList));
                logger.info(EMPTY_STRING);
                logger.info( " mirror leafs now " + this.mirrorTree);
                logger.info(EMPTY_STRING);
                abort();
                
            } else{            
                //regular solve, build up the binary tree
                if (!stopTesting) {
                     
                    mirrorTree.recordNodeSelectedForSolution(nodeData.nodeID);
                }
            }

        }
    }
    
    private boolean doesNodeNeedKidsCreated(int index) throws IloException {
        boolean decision = false;
        
        String newNodeID = getNodeId(index).toString();
        String oldNodeID = newToOldMap.get(newNodeID);
        
        //check if this oldNodeID needs kids created
        BranchingInstructionNode instr = iTree.nodeMap.get(oldNodeID );
 
        if (instr.childList.size() >ZERO){
            //check if any child has null entry in old to new map
            for (BranchingInstructionNode kid: instr.childList){
                decision=(oldToNewMap.get(kid.nodeID) ==null);
                if ( decision) break;
            }
        }
        
        return decision;
    } 
    
    private long checkMergeCompletion() throws IloException {
        long selectedIndex = -ONE;
                        
        //pick up any active leaf, whose corresponding old node needs kids created, i.e. its old kids have null entry for new node
        if (getNremainingNodes64() >= ONE )  {
            for (int index = ZERO; index<getNremainingNodes64(); index ++ ) {
                if (doesNodeNeedKidsCreated(index)) {
                    selectedIndex=index;
                    break;
                    //we have found a candidate node
                }
            }
        }  
        
        return selectedIndex;
    }
    
    public String printLeafs (List<String> pruneList) throws IloException {
       
        String result = EMPTY_STRING;
        for (int index = ZERO; index < getNremainingNodes64() ; index ++){
            NodeAttachment attachment = (NodeAttachment) getNodeData(index );
            if (!pruneList.contains(  attachment.nodeID))  result += "\n"+attachment.toString();
        }
        return result+"\n";
    }
    
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
     
}
