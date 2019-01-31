package sonia.scm.legacy;

import lombok.Getter;
import sonia.scm.repository.Person;

@Getter
public class LegacyAuthorDto {
    private final String mail;
    private final String name;

    LegacyAuthorDto(Person author) {
        this.mail = author.getMail();
        this.name = author.getName();
    }
}
