/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.besherman.fingerprint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Richard
 */
public class Fingerprint {
    private static final String ALGORITHM = "SHA-256";   

    // https://bugs.openjdk.java.net/browse/JDK-8041360
    private LocalDateTime date; 
    
    private Path sourceRoot;
    private String comment;
    private final List<FilePrint> files;
    
    public Fingerprint(Path root, String comment) throws IOException {
        this.sourceRoot = root.toAbsolutePath();
        this.comment = comment;
        this.date = LocalDateTime.now();        
        this.files = Files.walk(root)
                .parallel()
                .filter(p -> !Files.isDirectory(p))
                .map(this::createFilePrint)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    public Fingerprint(InputStream input) throws IOException {
        files = new ArrayList<>();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));        
        String line; 

        while((line = reader.readLine()) != null && line.startsWith("#")) {
            String[] kv = line.split("=");            
            String key = kv[0].substring(1).trim();
            String value = kv.length > 1 ? kv[1] : "";
            switch(key) {
                case "date":
                    date = LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
                    break;
                case "source-root":
                    sourceRoot = Paths.get(value);
                    break;
                case "comment":
                    comment = value;
                    break;
                case "hash-algorithm":
                    // todo: check that we are the same
                    break;
            }
        }        
        
        // the newline between the comment and the contents is lost here
        
        while((line = reader.readLine()) != null) {
            files.add(new FilePrint(line));
        }
    }
    
    public void write(OutputStream out) throws IOException {        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.write(String.format("# date=%s%n", date.format(DateTimeFormatter.ISO_DATE_TIME)));
        writer.write(String.format("# source-root=%s%n", sourceRoot));
        writer.write(String.format("# comment=%s%n", comment));
        writer.write(String.format("# hash-algorithm=%s%n", ALGORITHM));
        writer.write("\n");
        Iterator<FilePrint> it = files.iterator();
        while(it.hasNext()) {
            FilePrint fp = it.next();
            writer.write(fp.serialize());
            writer.write("\n");
        }
        writer.flush();
    }    

    public String getRoot() {
        return sourceRoot.toString();
    }

    public LocalDateTime getDate() {
        return date;
    }
    
    public String getComment() {
        return comment;
    }
    
    public Stream<FilePrint> stream() {
        return files.stream();
    }
   
      
    private Optional<FilePrint> createFilePrint(Path path) {
        try {
            String hash, source;
            if(isImage(path)) {
                hash = Hashes.createImageHash(path, ALGORITHM);
                source = "image";
            } else {
                hash = Hashes.createRawHash(path, ALGORITHM);
                source = "raw";
            }
            return Optional.of(new FilePrint(sourceRoot.relativize(path).toString(), hash, source));
        } catch(IOException ex) {
            Logger.getLogger(Fingerprint.class.getName()).log(Level.SEVERE, "Failed to create FilePrint", ex);
            return Optional.empty();
        }
    }

    private boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.equals(".jpeg");
    }                
}
