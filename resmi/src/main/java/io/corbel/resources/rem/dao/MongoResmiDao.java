package io.corbel.resources.rem.dao;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.corbel.lib.queries.request.Pagination;
import io.corbel.lib.queries.request.ResourceQuery;
import io.corbel.lib.queries.request.Sort;
import io.corbel.resources.rem.dao.builder.MongoAggregationBuilder;
import io.corbel.resources.rem.model.ResourceUri;
import io.corbel.resources.rem.resmi.exception.InvalidApiParamException;
import io.corbel.resources.rem.utils.JsonUtils;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.index.IndexDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.corbel.resources.rem.dao.builder.MongoAggregationBuilder.REFERENCE;

/**
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public interface MongoResmiDao extends ResmiDao {

    default JsonObject findResource(ResourceUri uri) {
        return findResource(uri, JsonObject.class);
    }

    default JsonArray findCollection(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort) throws InvalidApiParamException {
        return JsonUtils.convertToArray(findCollection(uri, resourceQueries, textSearch, pagination, sort, JsonObject.class));
    }


    default JsonElement findRelation(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort) throws InvalidApiParamException {
        JsonArray result = ResmiDaoHelper.renameIds(JsonUtils.convertToArray(findRelation(uri, resourceQueries, textSearch, pagination, sort, JsonObject.class)), uri.isTypeWildcard());

        if (uri.getRelationId() != null) {
            if (result.size() == 1) {
                return result.get(0);
            } else if (result.size() == 0) {
                return null;
            }
        }

        return result;
    }

    default JsonArray findCollectionWithGroup(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, List<String> groups, boolean first) throws InvalidApiParamException {
        if (sort.isPresent() && first) {
            sort = getSortWithFirst(sort);
        }
        GroupOperation group = getGroupOperationWithFirst(groups, first);
        List<JsonObject> result = aggregate(uri, resourceQueries, textSearch, pagination, sort, JsonObject.class, group);
        return JsonUtils.convertToArray(first ? extractDocuments(result) : result);
    }

    default JsonArray findRelationWithGroup(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, List<String> groups, boolean first) throws InvalidApiParamException {
        if (sort.isPresent() && first) {
            sort = getSortWithFirst(sort);
        }
        GroupOperation group = getGroupOperationWithFirst(groups, first);
        List<JsonObject> result = aggregate(uri, resourceQueries, textSearch, pagination, sort, JsonObject.class, group);
        return ResmiDaoHelper.renameIds(JsonUtils.convertToArray(first ? extractDocuments(result) : result), uri.isTypeWildcard());
    }

    default GroupOperation getGroupOperationWithFirst(List<String> groups, boolean first) {
        GroupOperation group = Aggregation.group(groups.toArray(new String[groups.size()]));
        if (first) {
            group = group.first(Aggregation.ROOT).as(REFERENCE);
        }
        return group;
    }

    default Optional<Sort> getSortWithFirst(Optional<Sort> sort) {
        return Optional.of(new Sort(sort.get().getDirection().name(), "first."+sort.get().getField()));
    }


    <T> T findResource(ResourceUri uri, Class<T> entityClass);

    <T> List<T> findCollection(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException;

    <T> List<T> findRelation(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException;

    <T> Stream<T> findCollectionAsStream(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException;

    <T> Stream<T> findRelationAsStream(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException;

    <T> List<T> aggregate(ResourceUri resourceUri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass, AggregationOperation... operations) throws InvalidApiParamException;

    <T> List<T> findAll(ResourceUri uri, Class<T> entityClass);

    void ensureExpireIndex(ResourceUri uri);

    void ensureIndex(ResourceUri uri, IndexDefinition indexDefinition);

    void dropIndex(ResourceUri uri, String indexId);

    static List<JsonObject> extractDocuments(List<JsonObject> results) {
        return results.stream().map(result -> result.get(REFERENCE).getAsJsonObject()).collect(Collectors.toList());
    }

}
