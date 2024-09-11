/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

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