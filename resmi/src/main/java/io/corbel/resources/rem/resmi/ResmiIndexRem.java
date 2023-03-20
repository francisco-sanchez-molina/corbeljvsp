package io.corbel.resources.rem.resmi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.corbel.lib.mongo.index.MongoIndex;
import io.corbel.lib.mongo.index.MongoTextSearchIndex;
import io.corbel.lib.ws.api.error.ErrorResponseFactory;
import io.corbel.resources.rem.BaseRem;
import io.corbel.resources.rem.model.ResourceUri;
import io.corbel.resources.rem.request.CollectionParameters;
import io.corbel.resources.rem.request.RelationParameters;
import io.corbel.resources.rem.request.RequestParameters;
import io.corbel.resources.rem.request.ResourceId;
import io.corbel.resources.rem.service.ResmiService;
import org.springframework.data.mongodb.core.index.IndexDefinition;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

/**
 * @author Alberto J. Rubio
 */
public class ResmiIndexRem extends BaseRem<JsonObject> {

    private static final String TEXT_SEARCH_INDEX = "text";
    private final ResmiService resmiService;

    public ResmiIndexRem(ResmiService resmiService) {
        this.resmiService = resmiService;
    }

    @Override
    public Response collection(String type, RequestParameters<CollectionParameters> parameters, URI uri, Optional<JsonObject> entity) {
        ResourceUri resourceUri = new ResourceUri(parameters.getRequestedDomain(), type);
        return createIndex(resourceUri, entity);
    }

    @Override
    public Response relation(String type, ResourceId id, String relation, RequestParameters<RelationParameters> parameters, Optional<JsonObject> entity) {
        ResourceUri resourceUri = new ResourceUri(parameters.getRequestedDomain(), type, id.getId(), relation);
        return createIndex(resourceUri, entity);
    }

    private Response createIndex(ResourceUri resourceUri, Optional<JsonObject> entity) {
        return entity.map(object -> {
            IndexDefinition indexDefinition = null;
            JsonArray fields = object.getAsJsonArray("fields");
            if (object.has("type") && TEXT_SEARCH_INDEX.equals(object.get("type").getAsString())) {
                MongoTextSearchIndex mongoTextSearchIndex = new MongoTextSearchIndex();
                fields.forEach(field -> mongoTextSearchIndex.on(field.getAsString()));
                if (object.has("name")) {
                    String name = object.get("name").getAsString();
                    if(!name.isEmpty()) {
                        mongoTextSearchIndex.named(name);
                    }
                }
                indexDefinition = mongoTextSearchIndex.getIndexDefinition();
            } else {
                MongoIndex mongoIndex = new MongoIndex();
                fields.forEach(field -> mongoIndex.on(field.getAsString()));
                if (object.has("name")) {
                    String name = object.get("name").getAsString();
                    if(!name.isEmpty()) {
                        mongoIndex.named(name);
                    }
                }
                if (object.has("unique") && object.get("unique").getAsBoolean()) {
                    mongoIndex.unique();
                }
                if (object.has("sparse") && object.get("sparse").getAsBoolean()) {
                    mongoIndex.sparse();
                }
                indexDefinition = mongoIndex.getIndexDefinition();
            }
            resmiService.ensureIndex(resourceUri, indexDefinition);
            return Response.ok().build();
        }).orElse(ErrorResponseFactory.getInstance().badRequest());
    }


    @Override
    public Class<JsonObject> getType() {
        return JsonObject.class;
    }
}
