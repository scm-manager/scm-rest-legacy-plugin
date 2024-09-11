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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;

import javax.xml.bind.annotation.XmlElement;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class LegacyRepositoryDto {
    private final String contact;
    private final Long creationDate;
    private final String description;
    private final String id;
    private final Long lasModified;
    private final String name;
    private final String type;
    @XmlElement(name = "public")
    private final boolean isPublic;
    private final String url;

    static LegacyRepositoryDto from(RepositoryServiceFactory serviceFactory, String repositoryId) {
        try (RepositoryService repositoryService = serviceFactory.create(repositoryId)) {
            return from(repositoryService);
        }
    }

    private static LegacyRepositoryDto from(RepositoryService repositoryService) {
        Repository repository = repositoryService.getRepository();
        String url;
        if (RepositoryPermissions.pull(repository).isPermitted()) {
            Optional<String> httpProtocolUrl = repositoryService.getSupportedProtocols()
                    .filter(p -> "http".equals(p.getType()))
                    .map(ScmProtocol::getUrl)
                    .findFirst();
            url = httpProtocolUrl.orElse(null);
        } else {
            url = null;
        }
        return new LegacyRepositoryDto(repository.getContact(),
                repository.getCreationDate(),
                repository.getDescription(),
                repository.getId(),
                repository.getLastModified(),
                String.format("%s/%s", repository.getNamespace(), repository.getName()),
                repository.getType(),
                false, // there is no public flag anymore
                url);
    }
}
