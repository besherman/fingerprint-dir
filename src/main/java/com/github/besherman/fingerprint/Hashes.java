/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.besherman.fingerprint;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
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

    
    private static final ThreadLocal<ByteBuffer> localBuffer = 
            ThreadLocal.withInitial(() -> ByteBuffer.allocate(1024 * 1024));
    
    /** Creates a SHA-1 hash of the file content. */
    public static String createRawHash(Path path, String alg) throws IOException {            
        try {
            MessageDigest digest = MessageDigest.getInstance(alg);
            RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
            
            FileChannel channel = file.getChannel();            
            ByteBuffer buff = localBuffer.get();            
            
            while(channel.read(buff) > -1) {
                buff.flip();
                digest.update(buff);
                buff.clear();
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
