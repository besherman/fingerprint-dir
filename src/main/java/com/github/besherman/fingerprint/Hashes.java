/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.besherman.fingerprint;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

/**
 *
 * @author Richard
 */
public class Hashes {
    
    /** Creates a SHA-1 hash of the image pixels. */
    public static String createImageHash(Path imageFile, String alg) throws IOException {            
        try {
            MessageDigest digest = MessageDigest.getInstance(alg);
            BufferedImage image = ImageIO.read(imageFile.toFile());
            int width = image.getWidth(),
                height = image.getHeight();
            int[] rowPixels = new int[width];
            ByteBuffer byteBuffer = ByteBuffer.allocate(width * 4);   
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for(int y = 0; y < height; y++) {
                image.getRGB(0, y, width, 1, rowPixels, 0, width);
                intBuffer.clear();
                intBuffer.put(rowPixels, 0, rowPixels.length);                                        
                digest.update(byteBuffer.array());                                
            }
            
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(2*hash.length); 
            for(byte b : hash) { 
                sb.append(String.format("%02x", b&0xff)); 
            } 

            return sb.toString();

        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }            
    }    

    
    /** Creates a SHA-1 hash of the file content. */
    public static String createRawHash(Path path, String alg) throws IOException {            
        try {
            MessageDigest digest = MessageDigest.getInstance(alg);
//            RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
//            MappedByteBuffer buff = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, Files.size(path));
            
            try(InputStream input = Files.newInputStream(path)) {
                byte[] buff = new byte[512];
                int read;
                while((read = input.read(buff)) != -1) {
                    digest.update(buff, 0, read);
                } 
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(2*hash.length); 
            for(byte b : hash) { 
                sb.append(String.format("%02x", b&0xff)); 
            } 

            return sb.toString();

        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }            
    }
    
    
}
