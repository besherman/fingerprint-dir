/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.besherman.fingerprint;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
        
        options.addOption( OptionBuilder
                                .withDescription("creates a fingerprint file for the directory")
                                .hasArg()
                                .withArgName("dir")
                                .create('c'));
        
        options.addOption( OptionBuilder
                                .withDescription("shows a diff between two fingerprint files")
                                .withArgName("left-file> <right-file")
                                .hasArgs(2)                                
                                .create('d'));
        
        options.addOption( OptionBuilder
                                .withDescription("shows a diff between a fingerprint and a directory")
                                .withArgName("fingerprint> <dir")
                                .hasArgs(2)                                
                                .create('t'));
        
        options.addOption( OptionBuilder
                                .withDescription("shows duplicates in a directory")
                                .withArgName("dir")
                                .hasArgs(1)                                
                                .create('u'));        

        options.addOption( OptionBuilder
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
        

        
        
        if(cmd.hasOption('c')) {
            Path root = Paths.get(cmd.getOptionValue('c'));
            
            if(!Files.isDirectory(root)) {
                System.err.println("root is not a directory");
                System.exit(7);
            }
            
            OutputStream out = System.out;
            
            if(cmd.hasOption('o')) {
                Path p = Paths.get(cmd.getOptionValue('o'));
                out = Files.newOutputStream(p);
            }
                        
            Fingerprint fp = new Fingerprint(root, "");
            fp.write(out);
            
        } else if(cmd.hasOption('d')) {
            String[] ar = cmd.getOptionValues('d');
            Path leftFingerprintFile = Paths.get(ar[0]),
                 rightFingerprintFile = Paths.get(ar[1]);
            if(!Files.isRegularFile(leftFingerprintFile)) {
                System.out.printf("%s is not a file%n", leftFingerprintFile);
                System.exit(7);
            }
            if(!Files.isRegularFile(rightFingerprintFile)) {
                System.out.printf("%s is not a file%n", rightFingerprintFile);
                System.exit(7);
            }
            
            
            Fingerprint left, right;            
            try(InputStream input = Files.newInputStream(leftFingerprintFile)) {
                left = new Fingerprint(input);
            }
            
            try(InputStream input = Files.newInputStream(rightFingerprintFile)) {
                right = new Fingerprint(input);
            }            
            
            Diff diff = new Diff(left, right);
            
            // TODO: if we have redirected output
            diff.print(System.out);
        } else if(cmd.hasOption('t')) {
            throw new RuntimeException("Not yet implemented");
        } else if(cmd.hasOption('u')) {
            Path root = Paths.get(cmd.getOptionValue('u'));
            Fingerprint fp = new Fingerprint(root, "");
            Map<String, FilePrint> map = new HashMap<>();
            fp.stream().forEach(f -> {
                if(map.containsKey(f.getHash())) {
                    System.out.println("  " + map.get(f.getHash()).getPath());
                    System.out.println("= " + f.getPath());
                    System.out.println("");
                } else {
                    map.put(f.getHash(), f);
                }
            });
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("fingd", options, true);            
        }
    }
}
