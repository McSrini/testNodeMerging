/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.testforcedbranchingmerge;

import static ca.mcmaster.spbinarytree.BinaryTree.*;
import ca.mcmaster.spbinarytree.BranchingInstructionTree;
import ilog.cplex.IloCplex;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.log4j.*;
import org.apache.log4j.Logger;

/**
 *
 * @author tamvadss
 */
public class Main {
    
    public static final String FILE_MPS =  "F:\\temporary files here\\msc98-ip.mps";
    
    public static void main(String[] args) throws Exception {
        
        Logger logger=Logger.getLogger(ActiveSubtree.class);
        logger.setLevel(Level.INFO);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+Main.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
        
        
        IloCplex cplex= new IloCplex();   
        cplex.importModel(FILE_MPS);
        
        ActiveSubtree activeSubTree = new ActiveSubtree(cplex, true, null);
        
        //solve till 2 farming instructions available
        activeSubTree.solve();
        double cutoff0 = activeSubTree.getSolution();
        
        //logger.info(activeSubTree.toString());
        
        List <BranchingInstructionTree> instructions = activeSubTree.getInstructions() ;
         
        //create the two merged trees
        IloCplex cplexM1= new IloCplex();   
        cplexM1.importModel( FILE_MPS);
        ActiveSubtree activeSubTreeM1 = new ActiveSubtree(cplexM1, false,  instructions.get(ZERO));
        activeSubTreeM1.setUpperCutoff(cutoff0) ;
        logger.info("Merging M1 ..." + LocalDateTime.now());
        activeSubTreeM1.solve();
        logger.info("Print M1 " + LocalDateTime.now());
        
        logger.info(activeSubTreeM1.toString());
        
        IloCplex cplexM2= new IloCplex();   
        cplexM2.importModel(FILE_MPS);
        ActiveSubtree activeSubTreeM2 = new ActiveSubtree(cplexM2, false,  instructions.get(ONE));
         double cutoff1 = activeSubTreeM1.getSolution();
         if (cutoff1<cutoff0) cutoff0=cutoff1;
        activeSubTreeM2.setUpperCutoff(cutoff0) ;
          logger.info("Merging M2 ..." + LocalDateTime.now());
        activeSubTreeM2.solve();
        logger.info("Print M2"+ LocalDateTime.now());
        logger.info(activeSubTreeM2.toString());
        
        //solve  each of the three trees in turn
        
    }
    
}
