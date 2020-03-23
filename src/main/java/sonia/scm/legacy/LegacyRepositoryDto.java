/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
