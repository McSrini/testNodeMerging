/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.testforcedbranchingmerge;

import ca.mcmaster.spbinarytree.BinaryTree;
import static ca.mcmaster.spbinarytree.BinaryTree.EMPTY_STRING;
import static ca.mcmaster.spbinarytree.BinaryTree.ZERO;
import ca.mcmaster.spbinarytree.BranchingInstructionTree;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
    
    private IloCplex cplex   ;
    
    //this is the mirrot of the solution tree
    private BinaryTree mirrorTree = new BinaryTree();
   
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private NodeHandler nodeHandler;
    
    private List<String> pruneList = new ArrayList<String> ();
    private List <BranchingInstructionTree> instructionTrees = new ArrayList<BranchingInstructionTree> ();  
    
    //map of new node, old node ID
    private Map<String, String> newToOldMap = new HashMap<String, String>();
    //map of old  node, new node ID, for every old node whose kids needs to be created
    private Map<String, String> oldToNewMap = new HashMap<String, String>();
    
    public ActiveSubtree (IloCplex cplex , boolean isMergeComplete  , BranchingInstructionTree iTree ) throws Exception{
            
        this.cplex=cplex;
        
        branchHandler = new BranchHandler(       pruneList  , mirrorTree, instructionTrees  , iTree,  newToOldMap,      oldToNewMap );
        nodeHandler = new NodeHandler(  pruneList,mirrorTree, instructionTrees,   iTree,  newToOldMap,      oldToNewMap, isMergeComplete);
         
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);
         
    
    }
     
    public void solve() throws IloException{
        cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
        cplex.solve();
     
    }
    
    public void setUpperCutoff (double cutoff) throws IloException {
        cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
    }
    
    public double getSolution () throws IloException {
        double cutoff = 100000000;
        boolean isFeasible= this.cplex.getStatus().equals(IloCplex.Status.Feasible);
        boolean isOptimal= this.cplex.getStatus().equals(IloCplex.Status.Optimal);
        if (isFeasible||isOptimal) cutoff=this.cplex.getObjValue();
        return cutoff;
    }
    
    public  List <BranchingInstructionTree>  getInstructions(){
        return instructionTrees;
    }
    
    public String toString ()  {
        
        return this.mirrorTree.toString();
         
    }
    
}
