package sonia.scm.legacy;

import lombok.Getter;
import sonia.scm.repository.ChangesetPagingResult;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter
public class LegacyChangesetCollectionDto {
    private final List<LegacyChangesetDto> changesets;

    LegacyChangesetCollectionDto(ChangesetPagingResult changesets) {
        this.changesets = changesets.getChangesets().stream().map(LegacyChangesetDto::new).collect(toList());
    }
}
