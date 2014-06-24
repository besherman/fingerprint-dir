/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.besherman.fingerprint;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilePrint implements Comparable<FilePrint> {
    private static final Pattern pattern = Pattern.compile("([^ ]+) ([^ ]+) (.+)");
    private final String hash;
    private final String hashSource;
    private final String path;
    
    public FilePrint(String serialized) {
        Matcher matcher = pattern.matcher(serialized);
        if(matcher.find()) {
            hashSource = matcher.group(1);
            hash = matcher.group(2);
            path = matcher.group(3);
        } else {
            throw new RuntimeException("Could not parse serialized FilePrint: " + serialized);
        }
    }
    
    public FilePrint(String path, String hash, String hashSource) {
        this.hash = hash;
        this.hashSource = hashSource;
        this.path = path;
    }    

    public String getPath() {
        return path;
    }
    
    public String serialize() {
        return String.format("%s %s %s", hashSource, hash, path);
    }

    @Override
    public String toString() {
        return "FilePrint{" + "hash=" + hash + ", hashSource=" + hashSource + ", path=" + path + '}';
    }

    public String getHash() {
        return hash;
    }

    public String getHashSource() {
        return hashSource;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.hash);
        hash = 97 * hash + Objects.hashCode(this.hashSource);
        hash = 97 * hash + Objects.hashCode(this.path);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FilePrint other = (FilePrint) obj;
        if (!Objects.equals(this.hash, other.hash)) {
            return false;
        }
        if (!Objects.equals(this.hashSource, other.hashSource)) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(FilePrint o) {
        return this.getPath().compareTo(o.getPath());
    }
}

