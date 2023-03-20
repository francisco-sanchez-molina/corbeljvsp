package io.corbel.resources.rem.resmi;

import com.google.gson.JsonObject;
import io.corbel.lib.ws.api.error.ErrorResponseFactory;
import io.corbel.resources.rem.BaseRem;
import io.corbel.resources.rem.model.ResourceUri;
import io.corbel.resources.rem.request.*;
import io.corbel.resources.rem.service.ResmiService;

import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author Alberto J. Rubio
 */
public class ResmiDropIndexRem extends BaseRem<JsonObject> {

    private final ResmiService resmiService;

    public ResmiDropIndexRem(ResmiService resmiService) {
        this.resmiService = resmiService;
    }

    @Override
    public Response resource(String type, ResourceId id, RequestParameters<ResourceParameters> parameters, Optional<JsonObject> entity) {
        ResourceUri resourceUri = new ResourceUri(parameters.getRequestedDomain(), type);
        return dropIndex(resourceUri, id.getId());
    }

    @Override
    public Response relation(String type, ResourceId id, String relation, RequestParameters<RelationParameters> parameters, Optional<JsonObject> entity) {
        Optional<RelationParameters> apiParameters = parameters.getOptionalApiParameters();
        Optional<String> predicateResource = apiParameters.flatMap(RelationParameters::getPredicateResource);
        ResourceUri uri = new ResourceUri(parameters.getRequestedDomain(), type, id.getId(), relation, predicateResource.orElse(null));
        if (!id.isWildcard() && uri.getRelationId() != null) {
            return dropIndex(uri, uri.getRelationId());
        } else {
            return ErrorResponseFactory.getInstance().methodNotAllowed();
        }
    }

    private Response dropIndex(ResourceUri resourceUri, String indexId) {
        resmiService.dropIndex(resourceUri, indexId);
        return Response.noContent().build();
    }

    @Override
    public Class<JsonObject> getType() {
        return JsonObject.class;
    }
}
