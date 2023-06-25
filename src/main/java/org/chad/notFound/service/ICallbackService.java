package org.chad.notFound.service;

import org.chad.notFound.model.vo.CallBack;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @BelongsProject: fistProject
 * @BelongsPackage: org.chad.notFound.service
 * @Author: hyl
 * @CreateTime: 2023-04-25  09:33
 * @Description: service for callback
 * @Version: 1.0
 */
public interface ICallbackService {
    /**
     * deal with the callback from rust server,saga mode
     *
     * @param callBack callBack
     */
    void dealCallBack(CallBack callBack);

    /**
     * don't need to rollback
     *
     * @param callBack callBack
     */
    void ok(CallBack callBack);

    /**
     * decrypt data transfer from rust server
     *
     * @param data data
     * @return {@link String}
     */
    default String decrypt(String data) {
        byte[] encryptedBytes = Base64.getDecoder().decode(data);
        byte[] keyBytes = padKey("1q2w#E$R", 16);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        String ivString = "29, 153, 21, 5, 126, 118, 184, 57, 99, 160, 45, 169, 178, 57, 246, 186";
        String[] ivArray = ivString.split(", ");
        byte[] iv = new byte[ivArray.length];
        for (int i = 0; i < ivArray.length; i++) {
            iv[i] = (byte) Integer.parseInt(ivArray[i].trim());
        }

        byte[] decryptedBytes;
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            decryptedBytes = cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * padding key to 128 bits
     *
     * @param key       key
     * @param keyLength keyLength
     * @return {@link byte[]}
     */
    static byte[] padKey(String key, int keyLength) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[keyLength];
        int repeatedKeyLength = (keyLength + keyBytes.length - 1) / keyBytes.length;
        byte[] repeatedKey = new byte[keyBytes.length * repeatedKeyLength];

        for (int i = 0; i < repeatedKey.length; i += keyBytes.length) {
            System.arraycopy(keyBytes, 0, repeatedKey, i, keyBytes.length);
        }

        System.arraycopy(repeatedKey, 0, paddedKey, 0, keyLength);
        return paddedKey;
    }
}
