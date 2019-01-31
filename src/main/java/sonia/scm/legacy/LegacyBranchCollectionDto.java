package sonia.scm.legacy;

import lombok.Getter;
import sonia.scm.repository.Branches;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter
public class LegacyBranchCollectionDto {
    private final List<LegacyBranchDto> branch;

    LegacyBranchCollectionDto(Branches branches) {
        this.branch = branches.getBranches().stream().map(LegacyBranchDto::new).collect(toList());
    }
}
