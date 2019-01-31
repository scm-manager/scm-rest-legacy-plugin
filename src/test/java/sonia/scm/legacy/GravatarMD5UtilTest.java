package sonia.scm.legacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GravatarMD5UtilTest {

    @Test
    void shouldCreateCorrectHash() {
        String gravatarHash = GravatarMD5Util.md5Hex("MyEmailAddress@example.com ");
        assertEquals("f9879d71855b5ff21e4963273a886bfc", gravatarHash);
    }
}