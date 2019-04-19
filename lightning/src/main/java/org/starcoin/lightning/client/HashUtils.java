package org.starcoin.lightning.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class HashUtils {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static byte[] intToByteArray(int value) {
    return new byte[] {
        (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
    };
  }

  public static byte[] longToByteArray(long value) {
    return new byte[] {
        (byte) (value >>> 56),
        (byte) (value >>> 48),
        (byte) (value >>> 40),
        (byte) (value >>> 32),
        (byte) (value >>> 24),
        (byte) (value >>> 16),
        (byte) (value >>> 8),
        (byte) value
    };
  }

  public static byte[] sha256(int src) {
    return sha256(intToByteArray(src));
  }

  public static byte[] sha256(byte[] src) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(src);
      return hash;
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    assert false;
    return null;
  }

  public static byte[] hash160(byte[] src) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("RIPEMD160");
      byte[] hash = digest.digest(src);
      return hash;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String md5Hex(byte[] src) {
    return bytesToHex(md5(src));
  }

  public static byte[] md5(byte[] src) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(src);
      return hash;
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
    return result.toString();
  }

  public static byte[] hexToBytes(String hexString) {
    int len = hexString.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
          + Character.digit(hexString.charAt(i+1), 16));
    }
    return data;
  }
}
