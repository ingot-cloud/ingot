package com.ingot.framework.security.common.utils;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.utils.AssertionUtils;

/**
 * <p>Description  : RSAUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/27.</p>
 * <p>Time         : 11:27 上午.</p>
 */
public final class RSAUtils {

    /**
     * 生成RSA公钥
     *
     * @param password 密码
     * @return bytes
     * @throws Exception NoSuchAlgorithmException
     */
    public static RSAPublicKey generatePublicKey(String password) throws Exception {
        return (RSAPublicKey) generateKey(password).getPublic();
    }

    /**
     * 生成RSA私钥
     *
     * @param password 密码
     * @return bytes
     * @throws Exception NoSuchAlgorithmException
     */
    public static RSAPrivateKey generatePrivateKey(String password) throws Exception {
        return (RSAPrivateKey) generateKey(password).getPrivate();
    }

    /**
     * 生成RSA秘钥{@link KeyPair}
     *
     * @return {@link KeyPair}
     * @throws Exception NoSuchAlgorithmException
     */
    public static KeyPair generateKey() throws Exception {
        return generateKey(2048);
    }

    /**
     * 生成RSA秘钥{@link KeyPair}
     *
     * @param password 密码
     * @return {@link KeyPair}
     * @throws Exception NoSuchAlgorithmException
     */
    public static KeyPair generateKey(String password) throws Exception {
        return generateKey(2048, password);
    }

    /**
     * 生成RSA秘钥{@link KeyPair}
     *
     * @param keySize 密钥大小，这是一个特定于算法的度量，例如模数长度，以位数指定。
     * @return {@link KeyPair}
     * @throws Exception NoSuchAlgorithmException
     */
    public static KeyPair generateKey(int keySize) throws Exception {
        return generateKey(keySize, null);
    }

    /**
     * 生成RSA秘钥{@link KeyPair}
     *
     * @param keySize  密钥大小，这是一个特定于算法的度量，例如模数长度，以位数指定。
     * @param password 密码
     * @return {@link KeyPair}
     * @throws Exception NoSuchAlgorithmException
     */
    public static KeyPair generateKey(int keySize, String password) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        if (StrUtil.isNotEmpty(password)) {
            SecureRandom secureRandom = new SecureRandom(password.getBytes());
            keyPairGenerator.initialize(keySize, secureRandom);
        } else {
            keyPairGenerator.initialize(keySize);
        }
        return keyPairGenerator.generateKeyPair();
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
        keyPairGenerator.initialize(2048, secureRandom);
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

    public static String toHexString(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static byte[] toBytes(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static InputStream getResourceAsStream(String filename) {
        InputStream stream = RSAUtils.class.getClassLoader().getResourceAsStream(filename);
        AssertionUtils.checkArgument(stream != null);
        return stream;
    }
}
