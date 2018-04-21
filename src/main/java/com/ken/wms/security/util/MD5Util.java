package com.ken.wms.security.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5
 *
 * @author Ken
 */
public class MD5Util {

    /**
     * 使用 MD5 算法对字符串进行处理
     *
     * @param plainString 需要处理的字符串
     * @return 返回处理后的字符串
     */
    public static String MD5(String plainString) {

        if (plainString != null) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(plainString.getBytes());
                byte[] byteData = messageDigest.digest();

                StringBuilder hexString = new StringBuilder();
                for (byte aByteData : byteData) {
                    String hex = Integer.toHexString(0xff & aByteData);
                    if (hex.length() == 1)
                        hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {/* log */}
        }
        return "";
    }
    
    public static void main(String[] args) {
    	String password="123456";
    	String encode=MD5(password);
    	System.out.println(encode);
    }
}
