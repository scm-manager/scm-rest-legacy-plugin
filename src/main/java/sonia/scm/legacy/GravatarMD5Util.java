package sonia.scm.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

/**
 * Gravatar MD5 hash util from https://de.gravatar.com/site/implement/images/java/
 */
final class GravatarMD5Util {

    private static final Logger logger = LoggerFactory.getLogger(GravatarMD5Util.class);

    private GravatarMD5Util() {
    }

    static String md5Hex(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(email.trim().toLowerCase().getBytes("CP1252")));
        } catch (Exception e) {
            logger.warn("could not create MD5 hash for email {}", email, e);
            return null;
        }
    }

    private static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte b : array) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
