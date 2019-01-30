package sonia.scm.legacy;

import java.security.MessageDigest;

/**
 * Gravatar MD5 hash util from https://de.gravatar.com/site/implement/images/java/
 */
public class GravatarMD5Util {
    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer(array.length * 2);
        for (byte b : array) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex (String email) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("MD5");
            return hex (md.digest(email.getBytes("CP1252")));
        } catch (Exception e) {
            return null;
        }
    }
}
