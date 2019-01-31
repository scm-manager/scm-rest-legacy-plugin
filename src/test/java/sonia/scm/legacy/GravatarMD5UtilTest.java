package sonia.scm.legacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GravatarMD5UtilTest {

    @Test
    void shouldCreateCorrectHash() {
        String gravatarHash = GravatarMD5Util.md5Hex("myemailaddress@example.com");
        assertEquals("0bc83cb571cd1c50ba6f3e8a78ef1346", gravatarHash);
    }

    @Test
    void shouldCreateCorrectHashWithSpaces() {
        String gravatarHash = GravatarMD5Util.md5Hex(" myemailaddress@example.com ");
        assertEquals("0bc83cb571cd1c50ba6f3e8a78ef1346", gravatarHash);
    }

    @Test
    void shouldCreateCorrectHashWithDifferentCase() {
        String gravatarHash = GravatarMD5Util.md5Hex("MyEmailAddress@example.com");
        assertEquals("0bc83cb571cd1c50ba6f3e8a78ef1346", gravatarHash);
    }
}