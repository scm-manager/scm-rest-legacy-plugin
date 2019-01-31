package sonia.scm.legacy;

import lombok.Getter;
import sonia.scm.repository.Branch;

@Getter
public class LegacyBranchDto {
    private final String name;
    private final String revision;

    LegacyBranchDto(Branch branch) {
        this.name = branch.getName();
        this.revision = branch.getRevision();
    }
}
