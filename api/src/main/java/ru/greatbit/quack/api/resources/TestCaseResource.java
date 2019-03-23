package ru.greatbit.quack.api.resources;

import io.swagger.annotations.ApiParam;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import ru.greatbit.quack.beans.*;
import ru.greatbit.quack.services.BaseService;
import ru.greatbit.quack.services.TestCaseService;
import ru.greatbit.quack.services.errors.EntityValidationException;
import ru.greatbit.whoru.auth.Session;
import ru.greatbit.whoru.jaxrs.Authenticable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static javax.ws.rs.core.Response.serverError;

@Authenticable
@Path("/{projectId}/testcase")
public class TestCaseResource extends BaseCrudResource<TestCase> {

    @Autowired
    private TestCaseService service;

    @Override
    protected BaseService<TestCase> getService() {
        return service;
    }

    @Override
    protected Filter initFilter(HttpServletRequest hsr) {
        TestcaseFilter filter = new TestcaseFilter(super.initFilter(hsr));
        if(hsr.getParameterValues("groups") != null){
            filter.getGroups().addAll(Arrays.asList(hsr.getParameterValues("groups")));
        }
        if (filter.getFields().containsKey("groups")){
            filter.getFields().remove("groups");
        }
        
        return filter;
    }

    @GET
    @Path("/tree")
    public TestCaseTree findFilteredTree(@ApiParam(value = "Project Id", required = true) @PathParam("projectId") String projectId) {
        return service.findFilteredTree(getUserSession(), projectId, (TestcaseFilter) initFilter(request));
    }

    @POST
    @Path("/attachment/{testcaseId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TestCase upload(@FormDataParam("file") InputStream uploadedInputStream,
                              @FormDataParam("file") FormDataContentDisposition fileDetail,
                              @FormDataParam("size") long size,
                              @PathParam("projectId") String projectId,
                              @PathParam("testcaseId") String testcaseId) throws IOException {
        if (fileDetail == null) {
            throw new EntityValidationException();
        }
        return service.uploadAttachment(getUserSession(), projectId, testcaseId,
                uploadedInputStream, fileDetail.getFileName(), fileDetail.getSize());
    }

    @GET
    @Path("/attachment/{testcaseId}/{attachmentId}")
    public Response downloadAttachment(
            @PathParam("projectId") String projectId,
            @PathParam("testcaseId") final String testcaseId,
            @PathParam("attachmentId") final String attachmentId) {
        Attachment attachment = service.getAttachment(getUserSession(), projectId, testcaseId, attachmentId);
        try {
            return Response
                    .ok(service.getAttachmentStream(attachment), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition", format("attachment; filename = %s", attachment.getTitle()))
                    .build();
        } catch (IOException ioexp) {
            return serverError().build();
        }
    }

    @DELETE
    @Path("/attachment/{testcaseId}/{attachmentId}")
    public TestCase deleteAttachment(
            @PathParam("projectId") String projectId,
            @PathParam("testcaseId") final String testcaseId,
            @PathParam("attachmentId") final String attachmentId) throws IOException {
        return service.deleteAttachment(getUserSession(), projectId, testcaseId, attachmentId);
    }

    @POST
    @Path("/issue/{testcaseId}")
    public TestCase createIssue(@PathParam("projectId") String projectId,
                                @PathParam("testcaseId") final String testcaseId,
                                @RequestBody Issue issue) {
        return service.createIssue(request, getUserSession(), projectId, testcaseId, issue);
    }

    @POST
    @Path("/issue/{testcaseId}/link-id/{issueId}")
    public TestCase linkIssueById(@PathParam("projectId") String projectId,
                                  @PathParam("testcaseId") final String testcaseId,
                                  @PathParam("issueId") final String issueId) {
        return service.linkIssueById(request, getUserSession(), projectId, testcaseId, issueId);
    }

    @POST
    @Path("/issue/{testcaseId}/link-url/{url}")
    public TestCase linkIssueByUrl(@PathParam("projectId") String projectId,
                                   @PathParam("testcaseId") final String testcaseId,
                                   @PathParam("url") final String url) {
        return service.linkIssueByUrl(request, getUserSession(), projectId, testcaseId, url);
    }

    @DELETE
    @Path("/issue/{testcaseId}/{issueId}")
    public TestCase unlinkIssue(@PathParam("projectId") String projectId,
                                @PathParam("testcaseId") final String testcaseId,
                                @PathParam("issueId") final String issueId) {
        return service.unlinkIssue(request, getUserSession(), projectId, testcaseId, issueId);
    }

    @GET
    @Path("/issue/suggest")
    public List<Issue> suggestIssue(@PathParam("projectId") String projectId,
                                    @QueryParam("text") String text) {
        return service.suggestIssue(request, getUserSession(), projectId, text);
    }
}
