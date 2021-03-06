package com.hut.util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TeaEncryptionHelper {

    private final int[] key;

    public TeaEncryptionHelper(String key){
        this(key.getBytes());
    }

    private TeaEncryptionHelper(byte[] key){
        //System.out.println(System.getProperty("java.library.path"));
        // TODO: catch the not linked error and output an informational message before closing
        System.loadLibrary("tea_encryption_helper");

        // convert key into 16-bit key
        this.key = new int[4];

        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            ByteBuffer bb = ByteBuffer.wrap(md.digest(key));
            for(int i = 0; i < this.key.length; i++){
                this.key[i] = bb.getInt();
            }

        }catch(NoSuchAlgorithmException e){
            System.out.println("No algorithm here" + e.getMessage());
        }

    }

    public byte[] encrypt(String input){
        return encrypt(input.getBytes());
    }

    public byte[] encrypt(byte[] input){
        // pad input
        int size = (input.length/4) + (input.length % 4);
        byte[] padded = new byte[size*4];
        ByteBuffer bb = ByteBuffer.wrap(padded);
        bb.put(input);

        IntBuffer ib = ByteBuffer.wrap(padded).asIntBuffer();

        int[] buf = new int[size];
        for(int i = 0; i < buf.length; i++){
            buf[i] = ib.get();
        }


        int[] cipherText = new int[2];
        int[] fullCipher;
        if(buf.length % 2 > 0){
            fullCipher = new int[buf.length + 1];
        }else {
            fullCipher = new int[buf.length];
        }

        IntBuffer anotherIB = IntBuffer.wrap(fullCipher);

        for(int i = 0; i < buf.length; i +=2){
            cipherText[0] = buf[i];
            if((i+1) < buf.length){
                cipherText[1] = buf[i+1];
            }else{
                cipherText[1] = 0;
            }

            cEncrypt(cipherText, key);
            anotherIB.put(cipherText);
        }

        ByteBuffer result = ByteBuffer.allocate(fullCipher.length*4);
        IntBuffer intBuffer = result.asIntBuffer();
        intBuffer.put(fullCipher);
        return result.array();
    }

    public byte[] decrypt(byte[] input){

        IntBuffer ib = ByteBuffer.wrap(input).asIntBuffer();
        int[] cipherText = new int[ib.remaining()];
        ib.get(cipherText);

        int[] ptf = new int[2];
        int[] pt = new int[cipherText.length];
        IntBuffer anotherIB = IntBuffer.wrap(pt);

        for(int i = 0; i < cipherText.length; i += 2){
            ptf[0] = cipherText[i];
            ptf[1] = cipherText[i+1];
            cDecrypt(ptf, key);
            anotherIB.put(ptf);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(pt.length * 4);
        IntBuffer thirdIntBuffer = byteBuffer.asIntBuffer();
        thirdIntBuffer.put(pt);
        return byteBuffer.array();
    }

    public String decryptString(byte[] input){
        return new String(decrypt(input)).trim();
    }

    public native void cEncrypt(int[] v, int[] k);

    public native void cDecrypt(int[] v, int[] k);


    /**
     * Simple test since JNI hates junit
     * @param args
     */
    public static void main(String args[]){
        //simpleTest();
        wrongKeyEncryptedTest();
    }

    private static void wrongKeyEncryptedTest(){
        String key1 = "4815162342108lost";
        String key2 = "we_have_to_go_back";
        TeaEncryptionHelper th1 = new TeaEncryptionHelper(key1);
        TeaEncryptionHelper th2 = new TeaEncryptionHelper(key2);

        String text = "john_locke";

        byte[] encryptedText1 = th1.encrypt(text);

        String decrypted1 = th1.decryptString(encryptedText1);
       // String decrypted2 = th2.decryptString(encryptedText1);

        //System.out.println(String.format("Decrypted1: %s\nDecrypted2: %s", decrypted1, decrypted2));
    }

    private static void simpleTest(){
        TeaEncryptionHelper th = new TeaEncryptionHelper("windows 98 is nice");
        String text = "And who are you the proud lord said that I must bow so low";
        byte[] encryptedText = th.encrypt(text);
        byte[] encryptedAgain = th.encrypt(text);
        if(encryptedText != encryptedAgain){
            System.out.println("Encrypting same thing twice yielded different results");
        }
        String decrypted = th.decryptString(encryptedText);
        String decryptedAgain = th.decryptString(encryptedAgain);
        if(!text.equals(decrypted)){
            System.out.println("Decrypted test doesn't equal original");
        }
        if(!text.equals(decryptedAgain)){
            System.out.println("Decrypted again text doesn't equal original");
        }
        System.out.println(String.format("Original: %s\nDecrypted: %s\nDecrypted again: %s",text, decrypted, decryptedAgain));
    }
}
