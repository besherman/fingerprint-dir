/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.conciliate.fingerprint;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Richard
 */
public class Diff {

    public Diff(String oldDesc, String newDesc, List<String> addedFiles, List<String> removedFiles, List<String> changedFiles,
            Map<String, String> movedFiles) {
        this.oldDescription = oldDesc;
        this.newDescription = newDesc;
        this.addedFiles = Collections.unmodifiableList(addedFiles);
        this.removedFiles = Collections.unmodifiableList(removedFiles);
        this.changedFiles = Collections.unmodifiableList(changedFiles);
        this.movedFiles = Collections.unmodifiableMap(movedFiles);
    }    
    
    public final String oldDescription;
    public final String newDescription;
    public final List<String> addedFiles;
    public final List<String> removedFiles;
    public final List<String> changedFiles;
    public final Map<String, String> movedFiles;
}
