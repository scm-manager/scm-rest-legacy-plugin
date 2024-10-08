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

import sonia.scm.repository.Branches;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryDAO;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;

import static java.util.stream.Collectors.toList;

@Path("rest")
public class RepositoryLegacyResource {

    private final RepositoryDAO repositoryDAO;
    private final RepositoryServiceFactory serviceFactory;

    @Inject
    public RepositoryLegacyResource(RepositoryDAO repositoryDAO, RepositoryServiceFactory serviceFactory) {
        this.repositoryDAO = repositoryDAO;
        this.serviceFactory = serviceFactory;
    }

    @Path("/repositories{format: (\\.json)?}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        return Response.ok(
                repositoryDAO.getAll()
                        .stream()
                        .filter(RepositoryPermissions.read()::isPermitted)
                        .map(Repository::getId)
                        .map(id -> LegacyRepositoryDto.from(serviceFactory, id))
                        .collect(toList()))
                .build();
    }

    @Path("/repositories/{id: [\\w]+}{format: (\\.json)?}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id) {
        return Response.ok(LegacyRepositoryDto.from(serviceFactory, id)).build();
    }

    @Path("/repositories/{id}/content")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response content(@PathParam("id") String id, @QueryParam("path") String path, @QueryParam("revision") String revision) {
        RepositoryPermissions.read(id).check();

        return Response.ok(createStreamingOutput(id, revision, path)).build();
    }

    private StreamingOutput createStreamingOutput(String repositoryId, String revision, String path) {
        return os -> {
            try (RepositoryService repositoryService = serviceFactory.create(repositoryId)) {
                repositoryService.getCatCommand().setRevision(revision).retriveContent(os, path);
                os.close();
            }
        };
    }

    @Path(value = "/repositories/{id}/branches{format: (\\.json)?}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBranches(@PathParam("id") String id) throws IOException {
        RepositoryPermissions.read(id).check();

        try (RepositoryService repositoryService = serviceFactory.create(id)) {
            Branches branches = repositoryService.getBranchesCommand().getBranches();
            return Response.ok(new LegacyBranchCollectionDto(branches)).build();
        }
    }

    @Path("/repositories/{id}/changesets{format: (\\.json)?}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChangesets(@PathParam("id") String id, @QueryParam("limit") int limit) throws IOException {
        RepositoryPermissions.read(id).check();

        try (RepositoryService repositoryService = serviceFactory.create(id)) {
            ChangesetPagingResult changesets = repositoryService.getLogCommand().setPagingStart(0).setPagingLimit(limit).getChangesets();
            return Response.ok(new LegacyChangesetCollectionDto(changesets)).build();
        }
    }
}
