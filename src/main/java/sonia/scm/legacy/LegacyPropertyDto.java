package sonia.scm.legacy;

import lombok.Getter;

@Getter
public class LegacyPropertyDto {
    private final String key;
    private final String value;

    LegacyPropertyDto(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
