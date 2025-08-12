package io.github.majianzheng.jarboot.common.utils;

import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES的加密和解密
 * @author majianzheng
 */
@SuppressWarnings({"java:S5542"})
public class AesUtils {
    /** 密钥 (需要前端和后端保持一致) */
    private static final String KEY;
    /** 算法 */
    private static final String ALGORITHMSTR = "AES/ECB/PKCS5Padding";

    static {
        File file = CacheDirHelper.getAesKeyFile();
        String temp = StringUtils.EMPTY;
        try {
            if (file.exists()) {
                temp = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // ignore
        }
        if (StringUtils.isEmpty(temp)) {
            temp = StringUtils.randomString(16);
            try {
                FileUtils.writeStringToFile(file, temp, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // ignore
            }
        }
        KEY = temp;
    }

    /**
     * AES解码
     * @param encrypt 内容
     * @return 解码内容
     */
    public static String decrypt(String encrypt) {
        try {
            return decrypt(encrypt, KEY);
        } catch (Exception e) {
            AnsiLog.error(e);
            throw new JarbootException(e);
        }
    }

    /**
     * AES加密
     * @param content 内容
     * @return 加密内容
     */
    public static String encrypt(String content) {
        try {
            return encrypt(content, KEY);
        } catch (Exception e) {
            AnsiLog.error(e);
            throw new JarbootException(e);
        }
    }

    /**
     * 将byte[]转为各种进制的字符串
     * @param bytes byte[]
     * @param radix 可以转换进制的范围，从Character.MIN_RADIX到Character.MAX_RADIX，超出范围后变为10进制
     * @return 转换后的字符串
     */
    public static String binary(byte[] bytes, int radix){
        return new BigInteger(1, bytes).toString(radix);
    }

    /**
     * base 64 encode
     * @param bytes 待编码的byte[]
     * @return 编码后的base 64 code
     */
    public static String base64Encode(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * base 64 decode
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     */
    public static byte[] base64Decode(String base64Code) {
        return StringUtils.isEmpty(base64Code) ? null : Base64.getDecoder().decode(base64Code);
    }


    /**
     * AES加密
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的byte[]
     */
    public static byte[] encryptToBytes(String content, String encryptKey) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey.getBytes(), "AES"));

            return cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }


    /**
     * AES加密为base 64 code
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的base 64 code
     */
    public static String encrypt(String content, String encryptKey) {
        return base64Encode(encryptToBytes(content, encryptKey));
    }

    /**
     * AES解密
     * @param encryptBytes 待解密的byte[]
     * @param decryptKey 解密密钥
     * @return 解密后的String
     */
    public static String decryptByBytes(byte[] encryptBytes, String decryptKey) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);

            Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey.getBytes(), "AES"));
            byte[] decryptBytes = cipher.doFinal(encryptBytes);
            return new String(decryptBytes);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }


    /**
     * 将base 64 code AES解密
     * @param encryptStr 待解密的base 64 code
     * @param decryptKey 解密密钥
     * @return 解密后的string
     */
    public static String decrypt(String encryptStr, String decryptKey) {
        return StringUtils.isEmpty(encryptStr) ? null : decryptByBytes(base64Decode(encryptStr), decryptKey);
    }

    private AesUtils() {}
}