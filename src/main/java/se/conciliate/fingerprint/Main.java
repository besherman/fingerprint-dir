/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.conciliate.fingerprint;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * TODO:
 *   - compare fingerprint file to directory
 *   - detect renamed/moved files (deleted+added with same hash)
 *   - detect new copies (added + same hash)
 * 
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        
        options.addOption( OptionBuilder.withLongOpt("create")                                
                                .withDescription("creates a map file for the directory")
                                .hasArg()
                                .withArgName("dir")
                                .create('c'));
        
        options.addOption( OptionBuilder.withLongOpt("diff-fingerprint")                                
                                .withDescription("shows a diff between two fingerprint files")
                                .withArgName("old-fingerprint> <new-fingerprint")
                                .hasArgs(2)                                
                                .create('d'));
        
        options.addOption( OptionBuilder.withLongOpt("diff-fingerprint-dir")                                
                                .withDescription("shows a diff between a fingerprint and a directory")
                                .withArgName("fingerprint> <dir")
                                .hasArgs(2)                                
                                .create('t'));

        options.addOption( OptionBuilder.withLongOpt("out")                                
                                .withDescription("output to file")
                                .withArgName("output-file")
                                .hasArg()                                
                                .create('o'));
        
        
        
        
        PosixParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException ex) {
            System.out.println(ex.getMessage());
            System.out.println("");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fingerprint-dir", options, true);            
            System.exit(7);
        }
        
        
        if(cmd.hasOption("create")) {
            Path root = Paths.get(cmd.getOptionValue("create"));
            
            if(!Files.isDirectory(root)) {
                System.err.println("root is not a directory");
                System.exit(7);
            }
            
            OutputStream out = System.out;
            
            if(cmd.hasOption("out")) {
                Path p = Paths.get(cmd.getOptionValue("out"));
                out = Files.newOutputStream(p);
            }
                        
            SaveTreeWalker.write(root, out);
            
        } else if(cmd.hasOption("diff-fingerprint")) {
            String[] ar = cmd.getOptionValues("diff-fingerprint");
            Path oldFingerprintFile = Paths.get(ar[0]),
                 newFingerprintFile = Paths.get(ar[1]);
            if(!Files.isRegularFile(oldFingerprintFile)) {
                System.out.printf("%s is not a file%n", oldFingerprintFile);
                System.exit(7);
            }
            if(!Files.isRegularFile(newFingerprintFile)) {
                System.out.printf("%s is not a file%n", newFingerprintFile);
                System.exit(7);
            }
            
            Diff diff = CompareTree.createDiff(oldFingerprintFile, newFingerprintFile);
            printDiff(diff);
        } else if(cmd.hasOption("diff-fingerprint-dir")) {
            throw new RuntimeException("Not yet implemented");
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fingerprint-dir", options, true);            
            
        }
    }
    
    private static void printDiff(Diff diff) {
        boolean change = false;
        
        System.out.println("Comparing: ");
        System.out.println("\t" + diff.oldDescription);
        System.out.println("\t" + diff.newDescription);
        System.out.println("");
        if(!diff.addedFiles.isEmpty()) {
            System.out.printf("%d added file(s) %n", diff.addedFiles.size());
            diff.addedFiles.stream()
                    .sorted()
                    .forEach(f -> System.out.printf("   %s%n", f));
            System.out.println("");
            change = true;
        }
        
        if(!diff.removedFiles.isEmpty()) {
            System.out.printf("%d removed file(s) %n", diff.removedFiles.size());
            diff.removedFiles.stream()
                    .sorted()
                    .forEach(f -> System.out.printf("   %s%n", f));
            System.out.println("");
            change = true;
        }
        
        if(!diff.movedFiles.isEmpty()) {
            System.out.printf("%d moved file(s) %n", diff.movedFiles.size());
            diff.movedFiles.forEach((k, v) -> System.out.printf("\t%s -> %s %n", k, v));
            System.out.println("");
            change = true;
        }
        
        
        if(!diff.changedFiles.isEmpty()) {
            System.out.printf("%d changed file(s) %n", diff.changedFiles.size());
            diff.changedFiles.stream()
                    .sorted()
                    .forEach(f -> System.out.printf("   %s%n", f));
            System.out.println("");
            change = true;
        }
        
        if(change == false) {
            System.out.println("No difference");
        }
    }
    
 

}
