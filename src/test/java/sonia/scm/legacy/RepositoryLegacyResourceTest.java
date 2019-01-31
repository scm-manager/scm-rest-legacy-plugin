package sonia.scm.legacy;

import com.github.sdorra.shiro.ShiroRule;
import com.github.sdorra.shiro.SubjectAware;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryDAO;
import sonia.scm.repository.api.BranchesCommandBuilder;
import sonia.scm.repository.api.CatCommandBuilder;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SubjectAware(username = "admin",
        password = "secret",
        configuration = "classpath:sonia/scm/legacy/shiro.ini")
public class RepositoryLegacyResourceTest {

    private static final Repository REPOSITORY_1 = new Repository("x", "git", "space", "X");
    private static final Repository REPOSITORY_2 = new Repository("y", "git", "blue", "origin");

    @Rule
    public ShiroRule shiroRule = new ShiroRule();

    private Dispatcher dispatcher;

    @Mock
    private RepositoryDAO repositoryDAO;
    @Mock
    private RepositoryServiceFactory serviceFactory;

    @InjectMocks
    private RepositoryLegacyResource resource;

    private final MockHttpResponse response = new MockHttpResponse();
    private RepositoryService repositoryService1;
    private RepositoryService repositoryService2;

    @Before
    public void init() {
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(resource);

        repositoryService1 = createServiceFor(REPOSITORY_1);
        when(serviceFactory.create("x")).thenReturn(repositoryService1);
        repositoryService2 = createServiceFor(REPOSITORY_2);
        when(serviceFactory.create("y")).thenReturn(repositoryService2);
        when(repositoryDAO.getAll()).thenReturn(asList(REPOSITORY_1, REPOSITORY_2));
    }

    private RepositoryService createServiceFor(Repository repository) {
        RepositoryService service = mock(RepositoryService.class);
        when(service.getRepository()).thenReturn(repository);
        return service;
    }

    @Test
    @SubjectAware(username = "admin")
    public void shouldReadAllRepositoriesForAdmin() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("space/X")
                .contains("blue/origin");
    }

    @Test
    @SubjectAware(username = "trillian")
    public void shouldReadOnlyRepositoriesWithPermission() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("space/X")
                .doesNotContain("blue/origin");
    }

    @Test
    @SubjectAware(username = "admin")
    public void shouldReadWithJsonAppendix() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories.json");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("space/X")
                .contains("blue/origin");
    }

    @Test
    public void shouldReadSingleRepository() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("space/X");
    }

    @Test
    public void shouldReadSingleRepositoryWithJsonAppendix() throws URISyntaxException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x.json");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("space/X");
    }

    @Test
    @SubjectAware(username = "trillian")
    public void shouldReadContent() throws URISyntaxException, IOException {
        ArgumentCaptor<String> revisionCaptor = forClass(String.class);

        CatCommandBuilder catCommandBuilder = mock(CatCommandBuilder.class, RETURNS_SELF);

        when(repositoryService1.getCatCommand()).thenReturn(catCommandBuilder);
        when(catCommandBuilder.setRevision(revisionCaptor.capture())).thenReturn(catCommandBuilder);

        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x/content?path=README&revision=123");
        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(revisionCaptor.getValue()).isEqualTo("123");
        verify(catCommandBuilder).retriveContent(any(), eq("README"));
    }

    @Test(expected = Exception.class)
    @SubjectAware(username = "trillian")
    public void shouldNotReadContentWithoutPermission() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/y/content?path=README");
        dispatcher.invoke(request, response);
    }

    @Test
    public void shouldReadBranches() throws URISyntaxException, IOException {
        BranchesCommandBuilder branchesCommandBuilder = mock(BranchesCommandBuilder.class, RETURNS_SELF);

        when(repositoryService1.getBranchesCommand()).thenReturn(branchesCommandBuilder);
        when(branchesCommandBuilder.getBranches()).thenReturn(new Branches(new Branch("branch", "master")));

        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x/branches");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("\"name\":\"branch\"")
                .contains("\"revision\":\"master\"");
    }

    @Test
    public void shouldReadBranchesWithJsonAppendix() throws URISyntaxException, IOException {
        BranchesCommandBuilder branchesCommandBuilder = mock(BranchesCommandBuilder.class, RETURNS_SELF);

        when(repositoryService1.getBranchesCommand()).thenReturn(branchesCommandBuilder);
        when(branchesCommandBuilder.getBranches()).thenReturn(new Branches(new Branch("branch", "master")));

        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x/branches.json");
        dispatcher.invoke(request, response);

        assertThat(response.getContentAsString())
                .contains("\"name\":\"branch\"")
                .contains("\"revision\":\"master\"");
    }

    @Test
    public void shouldReadChangesets() throws URISyntaxException, IOException {
        ArgumentCaptor<Integer> limitCaptor = forClass(Integer.class);

        LogCommandBuilder logCommandBuilder = mock(LogCommandBuilder.class, RETURNS_SELF);

        when(repositoryService1.getLogCommand()).thenReturn(logCommandBuilder);
        when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, asList(new Changeset("123", 1L, new Person("Trillian", "trillian@x.de")))));

        when(logCommandBuilder.setPagingLimit(limitCaptor.capture())).thenReturn(logCommandBuilder);

        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x/changesets?limit=1");
        dispatcher.invoke(request, response);

        System.out.println(response.getContentAsString());

        assertThat(response.getContentAsString())
                .contains("gravatar-hash");
        assertThat(limitCaptor.getValue()).isEqualTo(1);
    }

    @Test
    public void shouldReadChangesetsWithJsonAppendix() throws URISyntaxException, IOException {
        ArgumentCaptor<Integer> limitCaptor = forClass(Integer.class);

        LogCommandBuilder logCommandBuilder = mock(LogCommandBuilder.class, RETURNS_SELF);

        when(repositoryService1.getLogCommand()).thenReturn(logCommandBuilder);
        when(logCommandBuilder.getChangesets()).thenReturn(new ChangesetPagingResult(1, asList(new Changeset("123", 1L, new Person("Trillian", "trillian@x.de")))));

        when(logCommandBuilder.setPagingLimit(limitCaptor.capture())).thenReturn(logCommandBuilder);

        MockHttpRequest request = MockHttpRequest.get("/rest/repositories/x/changesets.json?limit=1");
        dispatcher.invoke(request, response);

        System.out.println(response.getContentAsString());

        assertThat(response.getContentAsString())
                .contains("gravatar-hash");
        assertThat(limitCaptor.getValue()).isEqualTo(1);
    }
}