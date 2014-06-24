/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.besherman.fingerprint;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Richard
 */
public class Diff {
    private final Fingerprint left;
    private final Fingerprint right;
    
    private final List<FilePrint> addedFiles;
    private final List<FilePrint> removedFiles;
    private final List<FilePrint> changedFiles;
    private final Map<FilePrint, FilePrint> movedFiles;

    public Diff(Fingerprint left, Fingerprint right) {
        this.left = left;
        this.right = right;
        
        // find new and removed
        
        Map<String, FilePrint> leftPathToFile = left.stream()
                .collect(Collectors.toMap(FilePrint::getPath, n -> n));
        
        Map<String, FilePrint> rightPathToFile = right.stream()
                .collect(Collectors.toMap(FilePrint::getPath, n -> n));
        
        addedFiles = rightPathToFile.keySet().stream()
                .filter(k -> !leftPathToFile.containsKey(k))
                .map(rightPathToFile::get)
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        removedFiles = leftPathToFile.keySet().stream()
                .filter(k -> !rightPathToFile.containsKey(k))
                .map(leftPathToFile::get)
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        changedFiles = rightPathToFile.keySet().stream()
                .filter(leftPathToFile::containsKey)
                .filter(k -> !leftPathToFile.get(k).equals(rightPathToFile.get(k)))
                .map(rightPathToFile::get)
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        
        ////////////////////////////////
        
        // Find moved files
        //
        
        movedFiles = new HashMap<>();
        
        
        // find files with the same hash that are in both added and removed
        ListIterator<FilePrint> it = removedFiles.listIterator();
        while(it.hasNext()) {
            FilePrint removed = it.next();
            
            Optional<FilePrint> movedTo = addedFiles.stream().filter(n -> n.getHash().equals(removed.getHash())).findFirst();
            if(movedTo.isPresent()) {            
                // don't show this as removed
                it.remove();

                // don't show this a new
                addedFiles.remove(movedTo.get());

                movedFiles.put(removed, movedTo.get());
            }
        }         
    }
    
    public void print(PrintStream ps) {
        boolean change = false;
        
        ps.println("Left: ");
        ps.println("< date: " + left.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        ps.println("< root: " + left.getRoot());
        ps.println("< comment: " + left.getComment());
        ps.println("");
        ps.println("Right: ");
        ps.println("> date: " + right.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        ps.println("> root: " + right.getRoot());
        ps.println("> comment: " + right.getComment());
        ps.println("");
        if(!addedFiles.isEmpty()) {
            ps.printf("%d added file(s) %n", addedFiles.size());
            addedFiles.stream()
                    .sorted()
                    .forEach(f -> ps.printf("   %s%n", f.getPath()));
            ps.println("");
            change = true;
        }
        
        if(!removedFiles.isEmpty()) {
            ps.printf("%d removed file(s) %n", removedFiles.size());
            removedFiles.stream()
                    .sorted()
                    .forEach(f -> ps.printf("   %s%n", f.getPath()));
            ps.println("");
            change = true;
        }
        
        if(!movedFiles.isEmpty()) {
            ps.printf("%d moved file(s) %n", movedFiles.size());
            movedFiles.forEach((k, v) -> ps.printf("\t%s -> %s %n", k.getPath(), v.getPath()));
            ps.println("");
            change = true;
        }
        
        
        if(!changedFiles.isEmpty()) {
            ps.printf("%d changed file(s) %n", changedFiles.size());
            changedFiles.stream()
                    .sorted()
                    .forEach(f -> ps.printf("   %s%n", f.getPath()));
            ps.println("");
            change = true;
        }
        
        if(change == false) {
            ps.println("No difference");
        }
    }
    
    
    
    
}
