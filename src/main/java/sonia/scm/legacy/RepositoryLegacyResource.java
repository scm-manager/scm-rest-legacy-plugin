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
