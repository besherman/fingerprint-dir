/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.conciliate.fingerprint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Richard
 */
class SaveTreeWalker extends SimpleFileVisitor<Path> {
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final XMLStreamWriter writer;
    private int indent = 1;
    private String algo = "SHA-256";

    public static void write(Path root, OutputStream output) throws IOException {
        SaveTreeWalker saver = new SaveTreeWalker(root, output);
        Files.walkFileTree(root, saver);
        saver.close();        
    }

    public SaveTreeWalker(Path root, OutputStream out) throws IOException {        
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        try {
            writer = factory.createXMLStreamWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("tree");
            writer.writeAttribute("algo", algo);
            writer.writeAttribute("root", root.toString());
            writer.writeAttribute("date", df.format(new Date()));
            writer.writeCharacters("\n");
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
    }

    public void close() throws IOException {
        try {
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        try {
            writeIndent();
            writer.writeStartElement("directory");
            writer.writeAttribute("name", dir.getFileName().toString());
            writer.writeCharacters("\n");
            indent++;
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        try {
            indent--;
            writeIndent();
            writer.writeEndElement();
            writer.writeCharacters("\n");
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (Files.isRegularFile(file)) {
            String name = file.getFileName().toString();
            try {
                writeIndent();
                writer.writeEmptyElement("file");
                writer.writeAttribute("name", name);
                if (isImage(file)) {
                    writer.writeAttribute("hash", Hashes.createImageHash(file, "SHA-256"));
                    writer.writeAttribute("hash-source", "image");
                } else {
                    writer.writeAttribute("hash", Hashes.createRawHash(file, "SHA-256"));
                    writer.writeAttribute("hash-source", "raw");
                }
                writer.writeCharacters("\n");
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    private boolean isImage(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".png");
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Logger.getLogger(SaveTreeWalker.class.getName()).log(Level.SEVERE, "Failed to visit " + file, exc);
        return FileVisitResult.CONTINUE;
    }

    private void writeIndent() throws XMLStreamException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            builder.append("    ");
        }
        writer.writeCharacters(builder.toString());
    }
    
}
