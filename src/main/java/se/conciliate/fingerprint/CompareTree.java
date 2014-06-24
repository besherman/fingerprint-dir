/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.conciliate.fingerprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Richard
 */
public class CompareTree {

    public static Diff createDiff(Path oldFile, Path newFile) throws XMLStreamException, IOException {
        DirectoryNode oldRoot = parse(oldFile),
                      newRoot = parse(newFile);
        
        List<FileNode> oldFiles = new ArrayList<>();
        oldRoot.accept(n -> {
            if(n instanceof FileNode) {
                oldFiles.add((FileNode)n);
            }
        });
        
        List<FileNode> newFiles = new ArrayList<>();
        newRoot.accept(n -> {
            if(n instanceof FileNode) {
                newFiles.add((FileNode)n);
            }
        });
        
        Map<String, FileNode> oldPathToFile = oldFiles.stream()
                .collect(Collectors.toMap(Node::getPath, n -> n));
        
        Map<String, FileNode> newPathToFile = newFiles.stream()
                .collect(Collectors.toMap(Node::getPath, n -> n));
        
        ////////////////
        
        List<String> newNodes = newPathToFile.keySet().stream()
                .filter(k -> !oldPathToFile.containsKey(k))
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        List<String> removedNodes = oldPathToFile.keySet().stream()
                .filter(k -> !newPathToFile.containsKey(k))
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        List<String> changedNodes = newPathToFile.keySet().stream()
                .filter(oldPathToFile::containsKey)
                .filter(k -> !oldPathToFile.get(k).equals(newPathToFile.get(k)))
                .collect(Collectors.toCollection(()-> new ArrayList<>()));
        
        
        ////////////////////////////////
        
        //
        // Moved files
        //
        
        Map<String, String> movedNodes = new HashMap<>();
        
        
        List<FileNode> newFileNodes = newNodes.stream()
                .map(newPathToFile::get)                
                .collect(Collectors.toList());
        
        List<FileNode> removedFileNodes = removedNodes.stream()
                .map(oldPathToFile::get)
                .collect(Collectors.toList());
        
        // find files with the same hash that are in both added and removed
        for(FileNode of: removedFileNodes) {
            Optional<FileNode> movedTo = newFileNodes.stream().filter(n -> n.getHash().equals(of.getHash())).findFirst();
            if(movedTo.isPresent()) {            
                // don't show this as removed
                removedNodes.remove(of.getPath());

                // don't show this a new
                newNodes.remove(movedTo.get().getPath());

                movedNodes.put(of.getPath(), movedTo.get().getPath());
            }
        }   
                
        return new Diff(oldRoot.getDescription(), newRoot.getDescription(), newNodes, removedNodes, changedNodes, movedNodes);        
    }
    
    private static DirectoryNode parse(Path path) throws XMLStreamException, IOException {
        XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(Files.newInputStream(path));

        Builder builder = new Builder();
        while (xmlReader.hasNext()) {
            int eventType = xmlReader.next();
            switch (eventType) {
                case XMLEvent.CDATA:
                case XMLEvent.SPACE:
                case XMLEvent.CHARACTERS:                    
                    break;
                case XMLEvent.END_ELEMENT:
                    builder.endElement();
                    break;
                case XMLEvent.START_ELEMENT:
                    Map<String, String> attrs;
                    int attributes = xmlReader.getAttributeCount();
                    if(attributes > 0) {
                        attrs = new HashMap<>();
                        for (int i = 0; i < attributes; ++i) {
                            attrs.put(xmlReader.getAttributeLocalName(i), xmlReader.getAttributeValue(i));
                        }
                    } else {
                        attrs = Collections.emptyMap();
                    }
                    builder.startElement(xmlReader.getLocalName(), attrs);                    
                    break;
            }
        }

        return builder.getRoot();        
    }
    
    private static class Builder {
        private final Stack<Object> stack = new Stack<>();
        private final DirectoryNode root = new DirectoryNode();
        private String srcDate;
        private String srcRoot;
        
        private void startElement(String element, Map<String, String> attrs) {
            if("tree".equals(element)) {
                this.srcDate = attrs.get("date");
                this.srcRoot = attrs.get("root");
                stack.push(root);
            } else if("directory".equals(element)) {
                DirectoryNode parent = (DirectoryNode)stack.peek();
                DirectoryNode dir = new DirectoryNode(parent, attrs);
                parent.addChild(dir);
                stack.push(dir);
            } else if("file".equals(element)) {
                DirectoryNode parent = (DirectoryNode)stack.peek();
                FileNode file = new FileNode(parent, attrs);
                parent.addChild(file);
                stack.push(file);
            } else {
                throw new RuntimeException("Unknown element: " + element);
            }
        }
        
        public void endElement() {
            stack.pop();
        }   
        
        public DirectoryNode getRoot() {
            root.setDescription(String.format("%s %s", srcRoot, srcDate));
            return root;
        }
    }

    private interface Node {
        String getPath();
    }
    
    private static class DirectoryNode implements Node {
        private final String name;
        private final List<DirectoryNode> dirs = new ArrayList<>();
        private final List<FileNode> files = new ArrayList<>();
        private final String path;
        private String description; 
        
        private DirectoryNode(DirectoryNode parent, Map<String, String> attrs) {
            this.name = attrs.get("name");
            this.path = String.format("%s/%s", parent.getPath(), name);
        }

        private DirectoryNode() {
            this.name = "";
            this.path = "";
        }

        private void addChild(DirectoryNode dir) {
            dirs.add(dir);
        }

        private void addChild(FileNode file) {
            files.add(file);
        }
        
        public List<DirectoryNode> getChildDirectories() { return dirs; }
        public List<FileNode> getChildFiles() { return files; }
        
        public String getName() { return name; };

        @Override
        public String getPath() {
            return path;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public void accept(Consumer<Node> c) {
            c.accept(this);
            dirs.stream().forEach(d -> d.accept(c));
            files.stream().forEach(c);
        }
        
        @Override
        public String toString() {
            return "DirectoryNode{" + "name=" + name + '}';
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.name);
            hash = 29 * hash + Objects.hashCode(this.path);
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
            final DirectoryNode other = (DirectoryNode) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.path, other.path)) {
                return false;
            }
            return true;
        }
    }

    private static class FileNode implements Node {
        private final String name;
        private final String hash;
        private final String hashSource;
        private final String path;

        private FileNode(DirectoryNode parent, Map<String, String> attrs) {
            this.name = attrs.get("name");
            this.hash = attrs.get("hash");
            this.hashSource = attrs.get("hash-source");                    
            this.path = String.format("%s/%s", parent.getPath(), name);
        }
        
        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "FileNode{" + "name=" + name + ", hash=" + hash + ", hashSource=" + hashSource + '}';
        }
        
        public String getHash() {
            return hash;
        }
        
        public String getHashSource() {
            return hashSource;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            hash = 97 * hash + Objects.hashCode(this.hash);
            hash = 97 * hash + Objects.hashCode(this.hashSource);
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
            final FileNode other = (FileNode) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.hash, other.hash)) {
                return false;
            }
            if (!Objects.equals(this.hashSource, other.hashSource)) {
                return false;
            }
            return true;
        }
        
        
    }
}

