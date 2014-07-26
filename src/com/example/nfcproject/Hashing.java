package com.example.nfcproject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class has a method that takes two strings and returns a SHA256 Hash of
 * the concatenation of both strings
 */
public class Hashing {

    /**
     * This is a utility function to suit our needs of hash two passwords
     * together consistently, so the hashes will be the same in every part of
     * our application.
     *
     * @param password1
     * @param password2
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String passwordToSHA256(String password1, String password2) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return stringToSHA256(password1.concat(password2));
    }

    /**
     * This function takes a string of input and returns a SHA-256 hash of that
     * string. It was adapted/copied from this Stack Overflow post
     * http://stackoverflow.com/questions/9661008/compute-sha256-hash-in-android-java-and-c-sharp
     *
     * @param input
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String stringToSHA256(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();

        byte[] byteData = digest.digest(input.getBytes("UTF-8"));
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
