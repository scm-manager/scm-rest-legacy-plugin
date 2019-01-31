package sonia.scm.legacy;

import lombok.Getter;
import sonia.scm.repository.Changeset;

import java.util.Collections;
import java.util.List;

@Getter
public class LegacyChangesetDto {
    private final LegacyAuthorDto author;
    private final List<LegacyPropertyDto> properties;
    private final String description;
    private final Long date;

    LegacyChangesetDto(Changeset changeset) {
        this.author = new LegacyAuthorDto(changeset.getAuthor());
        this.properties = Collections.singletonList(new LegacyPropertyDto("gravatar-hash", GravatarMD5Util.md5Hex(author.getMail())));
        this.description = changeset.getDescription();
        this.date = changeset.getDate();
    }
}
