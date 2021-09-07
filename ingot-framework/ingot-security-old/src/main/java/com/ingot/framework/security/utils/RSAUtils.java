package com.ingot.framework.security.utils;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : RSAUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/9.</p>
 * <p>Time         : 下午3:00.</p>
 */
@Slf4j
public final class RSAUtils {
    public static final String KEY_PUB = "pub";
    public static final String KEY_PRI = "pri";

    /**
     * 获取RSA公钥
     *
     * @param filename 文件名
     * @return {@link PublicKey}
     * @throws Exception NoSuchAlgorithmException, InvalidKeySpecException, IOException
     */
    public static PublicKey getPublicKey(String filename) throws Exception {
        InputStream resourceAsStream = getResourceAsStream(filename);
        DataInputStream dis = new DataInputStream(resourceAsStream);
        byte[] keyBytes = new byte[resourceAsStream.available()];
        dis.readFully(keyBytes);
        dis.close();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * 获取RAS私钥
     *
     * @param filename 文件名
     * @return {@link PrivateKey}
     * @throws Exception NoSuchAlgorithmException, InvalidKeySpecException, IOException
     */
    public static PrivateKey getPrivateKey(String filename) throws Exception {
        InputStream resourceAsStream = getResourceAsStream(filename);
        DataInputStream dis = new DataInputStream(resourceAsStream);
        byte[] keyBytes = new byte[resourceAsStream.available()];
        dis.readFully(keyBytes);
        dis.close();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    /**
     * 获取RSA公钥
     *
     * @param publicKey 公钥
     * @return {@link PublicKey}
     * @throws Exception NoSuchAlgorithmException, InvalidKeySpecException
     */
    public static PublicKey getPublicKey(byte[] publicKey) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * 获取RSA私钥
     *
     * @param privateKey 私钥
     * @return {@link PrivateKey}
     * @throws Exception NoSuchAlgorithmException, InvalidKeySpecException
     */
    public static PrivateKey getPrivateKey(byte[] privateKey) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    /**
     * 生成RSA公钥和密钥
     *
     * @param publicKeyFilename  公钥文件名
     * @param privateKeyFilename 私钥文件名
     * @param password           密码
     * @throws Exception IOException, NoSuchAlgorithmException
     */
    public static void generateKey(String publicKeyFilename,
                            String privateKeyFilename,
                            String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(password.getBytes());
        keyPairGenerator.initialize(1024, secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        FileOutputStream fos = new FileOutputStream(publicKeyFilename);
        fos.write(publicKeyBytes);
        fos.close();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        fos = new FileOutputStream(privateKeyFilename);
        fos.write(privateKeyBytes);
        fos.close();
    }

    /**
     * 生成RSA公钥
     *
     * @param password 密码
     * @return bytes
     * @throws Exception NoSuchAlgorithmException
     */
    public static byte[] generatePublicKey(String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(password.getBytes());
        keyPairGenerator.initialize(1024, secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        return keyPair.getPublic().getEncoded();
    }

    /**
     * 生成RSA私钥
     *
     * @param password 密码
     * @return bytes
     * @throws Exception NoSuchAlgorithmException
     */
    public static byte[] generatePrivateKey(String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(password.getBytes());
        keyPairGenerator.initialize(1024, secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        return keyPair.getPrivate().getEncoded();
    }

    /**
     * 生成RSA秘钥
     *
     * @param password 密码
     * @return Map
     * @throws Exception NoSuchAlgorithmException
     */
    public static Map<String, byte[]> generateKey(String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(password.getBytes());
        keyPairGenerator.initialize(1024, secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        Map<String, byte[]> map = new HashMap<>();
        map.put(KEY_PUB, publicKeyBytes);
        map.put(KEY_PRI, privateKeyBytes);
        return map;
    }

    public static String toHexString(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static byte[] toBytes(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static InputStream getResourceAsStream(String filename) {
        InputStream stream = RSAUtils.class.getClassLoader().getResourceAsStream(filename);
        Preconditions.checkArgument(stream != null);
        return stream;
    }

//    public static void main(String[] args) throws NoSuchAlgorithmException {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        SecureRandom secureRandom = new SecureRandom("123".getBytes());
//        keyPairGenerator.initialize(1024, secureRandom);
//        KeyPair keyPair = keyPairGenerator.genKeyPair();
//        System.out.println(keyPair.getPublic().getEncoded());
//    }
}
