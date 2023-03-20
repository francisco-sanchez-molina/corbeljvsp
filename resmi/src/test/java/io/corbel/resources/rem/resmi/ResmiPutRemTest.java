package io.corbel.resources.rem.resmi;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import io.corbel.resources.rem.request.CollectionParameters;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.corbel.lib.queries.request.ResourceQuery;
import io.corbel.resources.rem.dao.NotFoundException;
import io.corbel.resources.rem.model.ResourceUri;
import io.corbel.resources.rem.request.RequestParameters;
import io.corbel.resources.rem.request.ResourceId;
import io.corbel.resources.rem.request.ResourceParameters;
import io.corbel.resources.rem.resmi.exception.StartsWithUnderscoreException;

public class ResmiPutRemTest extends ResmiRemTest {

    private static final ResourceId OTHER_ID = new ResourceId("otherId");
    private static final String TEST_URI = "testUri/123";
    private static final String TEST_RELATION = "relation";
    private static final String TEST_INVALID_URI = "testUri/asdf/asdf";
    private AbstractResmiRem putRem;

    @Override
    @Before
    public void setup() {
        super.setup();
        putRem = new ResmiPutRem(resmiServiceMock);
    }

    private RequestParameters<CollectionParameters> getCollectionParametersMockWithCondition(Optional<List<ResourceQuery>> conditions) {
        CollectionParameters collectionParametersMock = mock(CollectionParameters.class);
        @SuppressWarnings("unchecked")
        RequestParameters<CollectionParameters> requestParametersMock = mock(RequestParameters.class);
        when(requestParametersMock.getOptionalApiParameters()).thenReturn(Optional.of(collectionParametersMock));
        when(collectionParametersMock.getConditions()).thenReturn(conditions);
        return requestParametersMock;

    }

    private RequestParameters<ResourceParameters> getResourceParametersMockWithCondition(Optional<List<ResourceQuery>> conditions) {
        ResourceParameters resourceParametersMock = mock(ResourceParameters.class);
        @SuppressWarnings("unchecked")
        RequestParameters<ResourceParameters> requestParametersMock = mock(RequestParameters.class);
        when(requestParametersMock.getOptionalApiParameters()).thenReturn(Optional.of(resourceParametersMock));
        when(resourceParametersMock.getConditions()).thenReturn(conditions);
        when(requestParametersMock.getRequestedDomain()).thenReturn(DOMAIN);
        return requestParametersMock;

    }

    @Test
    public void updateCollectionTest() {
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));

        RequestParameters<CollectionParameters> requestParametersMock = getCollectionParametersMockWithCondition(Optional.empty());
        Response response = putRem.collection(TEST_TYPE, requestParametersMock, null, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);

    }

    @Test
    public void updateCollectionTestWithCondition() throws StartsWithUnderscoreException {
        ResourceUri resourceUri = new ResourceUri(DOMAIN, TEST_TYPE);
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));
        @SuppressWarnings("unchecked")
        List<ResourceQuery> resourceQueryListMock = mock(List.class);
        RequestParameters<CollectionParameters> requestParametersMock = getCollectionParametersMockWithCondition(
                Optional.of(resourceQueryListMock));

        when(resmiServiceMock.updateCollection(resourceUri, json, resourceQueryListMock)).thenReturn(json);
        Response response = putRem.collection(TEST_TYPE, requestParametersMock, null, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void updateCollectionTestWithFailCondition() throws StartsWithUnderscoreException {
        ResourceUri resourceUri = new ResourceUri(DOMAIN, TEST_TYPE);
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));
        @SuppressWarnings("unchecked")
        List<ResourceQuery> resourceQueryListMock = mock(List.class);
        RequestParameters<CollectionParameters> requestParametersMock = getCollectionParametersMockWithCondition(
                Optional.of(resourceQueryListMock));

        when(resmiServiceMock.updateCollection(resourceUri, json, resourceQueryListMock)).thenReturn(null);
        Response response = putRem.collection(TEST_TYPE, requestParametersMock, null, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void updateResourceTest() {
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));

        RequestParameters<ResourceParameters> requestParametersMock = getResourceParametersMockWithCondition(Optional.empty());
        Response response = putRem.resource(TEST_TYPE, TEST_ID, requestParametersMock, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);

        response = putRem.resource(TEST_TYPE, OTHER_ID, requestParametersMock, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void updateResourceTestWithCondition() throws StartsWithUnderscoreException {
        ResourceUri resourceUri = new ResourceUri(DOMAIN, TEST_TYPE, ID);

        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));
        @SuppressWarnings("unchecked")
        List<ResourceQuery> resourceQueryListMock = mock(List.class);
        RequestParameters<ResourceParameters> requestParametersMock = getResourceParametersMockWithCondition(
                Optional.of(resourceQueryListMock));

        when(resmiServiceMock.conditionalUpdateResource(resourceUri, json, resourceQueryListMock)).thenReturn(json);
        Response response = putRem.resource(TEST_TYPE, TEST_ID, requestParametersMock, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    public void updateResourceTestWithFailCondition() throws StartsWithUnderscoreException {
        ResourceUri resourceUri = new ResourceUri(DOMAIN, TEST_TYPE, ID);
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));
        @SuppressWarnings("unchecked")
        List<ResourceQuery> resourceQueryListMock = mock(List.class);
        RequestParameters<ResourceParameters> requestParametersMock = getResourceParametersMockWithCondition(
                Optional.of(resourceQueryListMock));

        when(resmiServiceMock.conditionalUpdateResource(resourceUri, json, resourceQueryListMock)).thenReturn(null);
        Response response = putRem.resource(TEST_TYPE, TEST_ID, requestParametersMock, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(412);
    }

    @Test
    public void updateResourceWithUnderscoreInAttributeNameTest() throws StartsWithUnderscoreException {
        JsonObject json = new JsonObject();
        json.add("a", new JsonPrimitive("1"));
        json.add("_b", new JsonPrimitive("2"));

        doThrow(new StartsWithUnderscoreException("_b")).when(resmiServiceMock).updateResource(any(), eq(json));

        RequestParameters<ResourceParameters> requestParametersMock = getResourceParametersMockWithCondition(Optional.empty());

        Response response = putRem.resource(TEST_TYPE, TEST_ID, requestParametersMock, Optional.of(json));
        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    public void updateMissingTest() {
        Response response = putRem.resource(TEST_TYPE, TEST_ID, getParametersWithEmptyUri(), Optional.empty());
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    public void testPutRelation() {
        @SuppressWarnings("unchecked")
        Response response = putRem.relation(TEST_TYPE, TEST_ID, TEST_RELATION, getParameters(TEST_URI), Optional.empty());
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    public void testPutRelationWithUnderscoreInAttributeNameTest() throws NotFoundException, StartsWithUnderscoreException {
        JsonObject json = new JsonObject();
        json.add("_b", new JsonPrimitive("2"));

        doThrow(new StartsWithUnderscoreException("_b")).when(resmiServiceMock).upsertRelation(any(), eq(json));

        @SuppressWarnings("unchecked")
        Response response = putRem.relation(TEST_TYPE, TEST_ID, TEST_RELATION, getParameters(TEST_URI), Optional.ofNullable(json));
        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    public void testPutRelationWithData() {
        @SuppressWarnings("unchecked")
        Response response = putRem.relation(TEST_TYPE, TEST_ID, TEST_RELATION, getParameters(TEST_URI), Optional.of(getTestRelationData()));
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    public void testInvalidUriPutRelation() {
        @SuppressWarnings("unchecked")
        Response response = putRem.relation(TEST_TYPE, TEST_ID, TEST_RELATION, getParameters(TEST_INVALID_URI), Optional.empty());
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    public void testNullUriPutRelation() {
        @SuppressWarnings("unchecked")
        Response response = putRem.relation(TEST_TYPE, TEST_ID, TEST_RELATION, getParametersWithEmptyUri(), Optional.of(getTestResource()));
        assertThat(response.getStatus()).isEqualTo(400);
    }

}
