package sonia.scm.legacy;

import com.github.sdorra.ssp.PermissionActionCheck;
import lombok.Getter;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryDAO;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.repository.api.ScmProtocol;
import sonia.scm.util.Util;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Path("rest")
public class RepositoryLegacyResource {

    private final RepositoryDAO repositoryDAO;
    private final RepositoryServiceFactory serviceFactory;

    @Inject
    public RepositoryLegacyResource(RepositoryDAO repositoryDAO, RepositoryServiceFactory serviceFactory) {
        this.repositoryDAO = repositoryDAO;
        this.serviceFactory = serviceFactory;
    }

    @Path("/repositories.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        final PermissionActionCheck<Repository> check = RepositoryPermissions.read();

        Collection<Repository> repositories = Util.createSubCollection(repositoryDAO.getAll(), comparing(Repository::getId),
                (collection, item) -> {
                    if (check.isPermitted(item)) {
                        collection.add(item.clone());
                    }
                }, 0, Integer.MAX_VALUE);

        return Response.ok(repositories.stream().map(LegacyRepositoryDto::new).collect(toList())).build();
    }

    @Path("/repositories/{id}.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id) {
        RepositoryPermissions.read(id).check();

        Repository repository = repositoryDAO.get(id);
        if (repository == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(new LegacyRepositoryDto(repository)).build();
        }
    }

    @Path("/repositories/{id}/content")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") String id, @QueryParam("path") String path, @QueryParam("revision") String revision) {
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

    @Path("/repositories/{id}/branches.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBranches(@PathParam("id") String id) throws IOException {
        RepositoryPermissions.read(id).check();

        try (RepositoryService repositoryService = serviceFactory.create(id)) {
            Branches branches = repositoryService.getBranchesCommand().getBranches();
            return Response.ok(new LegacyBranchCollectionDto(branches)).build();
        }
    }

    @Path("/repositories/{id}/changesets.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChangesets(@PathParam("id") String id, @QueryParam("limit") int limit) throws IOException {
        RepositoryPermissions.read(id).check();

        try (RepositoryService repositoryService = serviceFactory.create(id)) {
            ChangesetPagingResult changesets = repositoryService.getLogCommand().setPagingStart(0).setPagingLimit(limit).getChangesets();
            return Response.ok(new LegacyChangesetCollectionDto(changesets)).build();
        }
    }

    @Getter
    public class LegacyRepositoryDto {
        private final String contact;
        private final Long creationDate;
        private final String description;
        private final String id;
        private final Long lasModified;
        private final String name;
        private final boolean archived;
        private final String type;
        @XmlElement(name = "public")
        private final boolean isPublic;
        private final String url;

        LegacyRepositoryDto(Repository repository) {
            this.contact = repository.getContact();
            this.creationDate = repository.getCreationDate();
            this.description = repository.getDescription();
            this.id = repository.getId();
            this.lasModified = repository.getLastModified();
            this.name = String.format("%s/%s", repository.getNamespace(), repository.getName());
            this.archived = repository.isArchived();
            this.type = repository.getType();
            this.isPublic = repository.isPublicReadable();
            try (RepositoryService repositoryService = serviceFactory.create(repository.getId())) {
                if (RepositoryPermissions.pull(repository).isPermitted()) {
                    Optional<String> httpProtocolUrl = repositoryService.getSupportedProtocols()
                            .filter(p -> "http".equals(p.getType()))
                            .map(ScmProtocol::getUrl)
                            .findFirst();
                    this.url = httpProtocolUrl.orElse(null);
                } else {
                    this.url = null;
                }
            }
        }
    }

    @Getter
    public static class LegacyBranchCollectionDto {
        private final List<LegacyBranchDto> branch;

        LegacyBranchCollectionDto(Branches branches) {
            this.branch = branches.getBranches().stream().map(LegacyBranchDto::new).collect(toList());
        }
    }

    @Getter
    public static class LegacyBranchDto {
        private final String name;
        private final String revision;

        LegacyBranchDto(Branch branch) {
            this.name = branch.getName();
            this.revision = branch.getRevision();
        }
    }

    @Getter
    public static class LegacyChangesetCollectionDto {
        private final List<LegacyChangesetDto> changesets;

        LegacyChangesetCollectionDto(ChangesetPagingResult changesets) {
            this.changesets = changesets.getChangesets().stream().map(LegacyChangesetDto::new).collect(toList());
        }
    }

    @Getter
    public static class LegacyChangesetDto {
        private final LegacyAuthorDto author;
        private final List<LegacyPropertyDto> properties;
        private final String description;
        private final Long date;

        LegacyChangesetDto(Changeset changeset) {
            this.author = new LegacyAuthorDto(changeset.getAuthor());
            this.properties = Collections.singletonList(new LegacyPropertyDto("gravatar-hash", GravatarMD5Util.md5Hex(author.mail)));
            this.description = changeset.getDescription();
            this.date = changeset.getDate();
        }
    }

    @Getter
    public static class LegacyPropertyDto {
        private final String key;
        private final String value;

        LegacyPropertyDto(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @Getter
    public static class LegacyAuthorDto {
        private final String mail;
        private final String name;

        LegacyAuthorDto(Person author) {
            this.mail = author.getMail();
            this.name = author.getName();
        }
    }
}
