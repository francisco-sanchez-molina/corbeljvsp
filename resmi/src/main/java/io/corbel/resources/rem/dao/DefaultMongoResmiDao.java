package io.corbel.resources.rem.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.corbel.lib.mongo.JsonObjectMongoWriteConverter;
import io.corbel.lib.mongo.utils.GsonUtil;
import io.corbel.lib.queries.mongo.builder.CriteriaBuilder;
import io.corbel.lib.queries.request.*;
import io.corbel.resources.rem.dao.builder.MongoAggregationBuilder;
import io.corbel.resources.rem.model.GenericDocument;
import io.corbel.resources.rem.model.RelationDocument;
import io.corbel.resources.rem.model.ResourceUri;
import io.corbel.resources.rem.resmi.exception.InvalidApiParamException;
import io.corbel.resources.rem.utils.JsonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;
import org.springframework.expression.spel.SpelParseException;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


/**
 * @author Alberto J. Rubio
 *
 */
public class DefaultMongoResmiDao implements MongoResmiDao {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMongoResmiDao.class);

    private static final String ID = "id";
    private static final String _ID = "_id";

    private static final String RELATION_CONCATENATION = ".";
    private static final String DOMAIN_CONCATENATION = "__";
    private static final String EMPTY_STRING = "";
    private static final String EXPIRE_AT = "_expireAt";
    private static final String CREATED_AT = "_createdAt";
    private static final String COUNT = "count";
    private static final String AVERAGE = "average";
    private static final String SUM = "sum";
    public static final String $INC = "$inc";

    private static final int MONGO_DOUBLE_TYPE = 1;
    private static final int MONGO_INT_TYPE = 16;
    private static final int MONGO_LONG_TYPE = 18;


    private final MongoOperations mongoOperations;
    private final JsonObjectMongoWriteConverter jsonObjectMongoWriteConverter;
    private final NamespaceNormalizer namespaceNormalizer;
    private final ResmiOrder resmiOrder;
    private final AggregationResultsFactory<JsonElement> aggregationResultsFactory;
    private final boolean allowDiskUse;

    public DefaultMongoResmiDao(MongoOperations mongoOperations, JsonObjectMongoWriteConverter jsonObjectMongoWriteConverter,
                                NamespaceNormalizer namespaceNormalizer, ResmiOrder resmiOrder, AggregationResultsFactory<JsonElement> aggregationResultsFactory,
                                boolean allowDiskUse) {
        this.mongoOperations = mongoOperations;
        this.jsonObjectMongoWriteConverter = jsonObjectMongoWriteConverter;
        this.namespaceNormalizer = namespaceNormalizer;
        this.resmiOrder = resmiOrder;
        this.aggregationResultsFactory = aggregationResultsFactory;
        this.allowDiskUse = allowDiskUse;
    }

    @Override
    public boolean existsResources(ResourceUri uri) {
        return mongoOperations.exists(Query.query(Criteria.where(_ID).is(uri.getTypeId())), getMongoCollectionName(uri));
    }

    @Override
    public <T> T findResource(ResourceUri uri, Class<T> entityClass) {
        return mongoOperations.findById(uri.getTypeId(), entityClass, getMongoCollectionName(uri));
    }

    @Override
    public <T> List<T> findCollection(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException {
        Query query = getCollectionQuery(resourceQueries, textSearch, pagination, sort);
        LOG.debug("findCollection Query executed : " + query.getQueryObject().toString());
        return mongoOperations.find(query, entityClass, getMongoCollectionName(uri));
    }

    @Override
    public <T> List<T> findRelation(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException {
        Query query = getRelationQuery(uri, resourceQueries, textSearch, pagination, sort);
        LOG.debug("findRelation Query executed : " + query.getQueryObject().toString());
        return mongoOperations.find(query, entityClass, getMongoCollectionName(uri));
    }

    private Query getCollectionQuery(Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort) throws InvalidApiParamException {
        MongoResmiQueryBuilder mongoResmiQueryBuilder = new MongoResmiQueryBuilder();
        try {
            mongoResmiQueryBuilder.query(resourceQueries.orElse(null)).pagination(pagination.orElse(null)).sort(sort.orElse(null));
        } catch (PatternSyntaxException pse) {
            throw new InvalidApiParamException(pse.getMessage());
        }
        if (textSearch.isPresent() && StringUtils.isNoneEmpty(textSearch.get()))
        {
            mongoResmiQueryBuilder.textSearch(textSearch.get());
        }
        return mongoResmiQueryBuilder.build();
    }

    private Query getRelationQuery(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort) throws InvalidApiParamException {
        MongoResmiQueryBuilder mongoResmiQueryBuilder = new MongoResmiQueryBuilder();

        if (uri.getRelationId() != null) {
            mongoResmiQueryBuilder.relationDestinationId(uri.getRelationId());
        }

        try {
            mongoResmiQueryBuilder.relationSubjectId(uri).query(resourceQueries.orElse(null)).pagination(pagination.orElse(null))
                    .sort(sort.orElse(null));
        } catch (PatternSyntaxException pse) {
            throw new InvalidApiParamException(pse.getMessage());
        }


        if (textSearch.isPresent() && StringUtils.isNoneEmpty(textSearch.get())) {
            mongoResmiQueryBuilder.textSearch(textSearch.get());
        }

        Query query = mongoResmiQueryBuilder.build();
        query.fields().exclude(_ID);
        return query;
    }

    @Override
    public <T> Stream<T> findCollectionAsStream(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException {
        Query query = getCollectionQuery(resourceQueries, textSearch, pagination, sort);
        CloseableIterator<T> iterator = mongoOperations.stream(query, entityClass, getMongoCollectionName(uri));
        return StreamUtils.createStreamFromIterator(iterator);
    }

    @Override
    public <T> Stream<T> findRelationAsStream(ResourceUri uri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass) throws InvalidApiParamException {
        Query query = getRelationQuery(uri, resourceQueries, textSearch, pagination, sort);
        CloseableIterator<T> iterator = mongoOperations.stream(query, entityClass, getMongoCollectionName(uri));
        return StreamUtils.createStreamFromIterator(iterator);
    }

    @Override
    public <T> List<T> aggregate(ResourceUri resourceUri, Optional<List<ResourceQuery>> resourceQueries, Optional<String> textSearch, Optional<Pagination> pagination, Optional<Sort> sort, Class<T> entityClass, AggregationOperation... operations) {
        MongoAggregationBuilder builder = new MongoAggregationBuilder();
        builder.match(resourceUri, resourceQueries, textSearch);
        Arrays.asList(operations).forEach(builder::withOperation);

        if (sort.isPresent()) {
            builder.sort(sort.get().getDirection().toString(),  sort.get().getField());
        }

        if (pagination.isPresent()) {
            builder.pagination(pagination.get());
        }

        Aggregation aggregation = builder.build();

        if(allowDiskUse) {
            aggregation = withAllowDiskUseOption(aggregation);
        }
        return mongoOperations.aggregate(aggregation, getMongoCollectionName(resourceUri), entityClass).getMappedResults();
    }

    private List<DBObject> aggregate(ResourceUri resourceUri, Optional<List<ResourceQuery>> resourceQueries,  AggregationOperation... operations) {
        return aggregate(resourceUri, resourceQueries, Optional.empty(), Optional.empty(), Optional.empty(), DBObject.class, operations);
    }


    private Aggregation withAllowDiskUseOption(Aggregation aggregation) {
        return aggregation.withOptions(new AggregationOptions.Builder().allowDiskUse(true).build());
    }

    @Override
    public <T> List<T> findAll(ResourceUri uri, Class<T> entityClass) {
        return mongoOperations.findAll(entityClass, getMongoCollectionName(uri));
    }

    @Override
    public void updateCollection(ResourceUri uri, JsonObject entity, List<ResourceQuery> resourceQueries) {
        updateMulti(getMongoCollectionName(uri), entity, Optional.of(resourceQueries));
    }

    @Override
    public void updateResource(ResourceUri uri, JsonObject entity) {
        findAndModify(getMongoCollectionName(uri), Optional.of(uri.getTypeId()), entity, true, Optional.empty());
    }

    @Override
    public boolean conditionalUpdateResource(ResourceUri uri, JsonObject entity, List<ResourceQuery> resourceQueries) {
        JsonObject saved = findAndModify(getMongoCollectionName(uri), Optional.of(uri.getTypeId()), entity, false, Optional.of(resourceQueries));
        return saved != null;
    }

    private void updateMulti(String collection, JsonObject entity, Optional<List<ResourceQuery>> resourceQueries) {
        Update update = updateFromJsonObject(entity, Optional.empty());
        Query query = getQueryFromResourceQuery(resourceQueries, Optional.empty());
        mongoOperations.updateMulti(query, update, JsonObject.class, collection);
    }

    private Query getQueryFromResourceQuery(Optional<List<ResourceQuery>> resourceQueries, Optional<String> id) {
        MongoResmiQueryBuilder builder = id.map(identifier -> new MongoResmiQueryBuilder().id(identifier)).orElse(new MongoResmiQueryBuilder());

        if (resourceQueries.isPresent()) {
            builder.query(resourceQueries.get());
        }

        return builder.build();
    }

    private JsonObject findAndModify(String collection, Optional<String> id, JsonObject entity, boolean upsert, Optional<List<ResourceQuery>> resourceQueries) {
        Update update = updateFromJsonObject(entity, id);
        Query query = (id.isPresent()) ? getQueryFromResourceQuery(resourceQueries, id) : Query.query(Criteria.where(_ID).exists(false));

        return mongoOperations.findAndModify(query, update, FindAndModifyOptions.options().upsert(upsert).returnNew(true), JsonObject.class, collection);
    }

    @Override
    public void saveResource(ResourceUri uri, Object entity) {
        mongoOperations.save(entity, getMongoCollectionName(uri));
    }

    @SuppressWarnings("unchecked")
    private Update updateFromJsonObject(JsonObject entity, Optional<String> id) {
        Update update = new Update();

        if (id.isPresent()) {
            entity.remove(ID);
            if (entity.entrySet().isEmpty()) {
                update.set(_ID, id);
            }
        }

        if (entity.has(CREATED_AT)) {
            JsonPrimitive createdAt = entity.get(CREATED_AT).getAsJsonPrimitive();
            entity.remove(CREATED_AT);
            update.setOnInsert(CREATED_AT, GsonUtil.getPrimitive(createdAt));
        }

        if (entity.has($INC) && entity.get($INC).isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : entity.getAsJsonObject($INC).entrySet()) {
                update.inc(entry.getKey(), entry.getValue().getAsLong());
            }
            entity.remove($INC);
        }

        jsonObjectMongoWriteConverter.convert(entity).toMap().forEach((key, value) -> update.set((String) key, value));
        entity.entrySet().stream().filter(entry -> entry.getValue().isJsonNull()).forEach(entry -> update.unset(entry.getKey()));

        return update;
    }

    @Override
    public void upsertRelation(ResourceUri uri, JsonObject entity) throws NotFoundException {
        if (!existsResources(new ResourceUri(uri.getDomain(), uri.getType(), uri.getTypeId()))) {
            throw new NotFoundException("The resource does not exist");
        }

        JsonObject relationJson = JsonRelation.create(uri.getTypeId(), uri.getRelationId(), entity);
        JsonObject storedRelation = findModifyOrCreateRelation(uri, relationJson);

        if (!storedRelation.has("_order")) {
            JsonObject order = new JsonObject();
            resmiOrder.addNextOrderInRelation(uri, order);
            findAndModify(getMongoCollectionName(uri), Optional.ofNullable(storedRelation.get("id").getAsString()), order, false,
                    Optional.empty());
        }
    }

    @Override
    public boolean conditionalUpdateRelation(ResourceUri uri, JsonObject entity, List<ResourceQuery> resourceQueries) {
        JsonObject saved = findModifyRelation(uri, entity, resourceQueries);
        return saved != null;
    }

    private JsonObject findModifyOrCreateRelation(ResourceUri uri, JsonObject entity) {
        if (uri.getRelationId() != null) {
            Criteria criteria = Criteria.where(JsonRelation._SRC_ID).is(uri.getTypeId()).and(JsonRelation._DST_ID).is(uri.getRelationId());
            Update update = updateFromJsonObject(entity, Optional.<String>empty());
            update.set(JsonRelation._SRC_ID, uri.getTypeId());
            update.set(JsonRelation._DST_ID, uri.getRelationId());
            return mongoOperations.findAndModify(new Query(criteria), update, FindAndModifyOptions.options().upsert(true).returnNew(true),
                    JsonObject.class, getMongoCollectionName(uri));
        } else {
            mongoOperations.save(entity, getMongoCollectionName(uri));
            return entity;
        }
    }
    private JsonObject findModifyRelation(ResourceUri uri, JsonObject entity,  List<ResourceQuery>
            resourceQueries) {
        Query query = new MongoResmiQueryBuilder().query(resourceQueries).build();
        Criteria criteria = Criteria.where(JsonRelation._SRC_ID).is(uri.getTypeId()).and(JsonRelation._DST_ID).is(uri.getRelationId());
        query.addCriteria(criteria);
        Update update = updateFromJsonObject(entity, Optional.<String>empty());
        update.set(JsonRelation._SRC_ID, uri.getTypeId());
        update.set(JsonRelation._DST_ID, uri.getRelationId());
        return mongoOperations.findAndModify(query, update, FindAndModifyOptions.options().upsert(false).returnNew
                        (true),
                JsonObject.class, getMongoCollectionName(uri));
    }

    @Override
    public void ensureExpireIndex(ResourceUri uri) {
        mongoOperations.indexOps(getMongoCollectionName(uri)).ensureIndex(new Index().on(EXPIRE_AT, Direction.ASC).expire(0));
    }

    @Override
    public void ensureIndex(ResourceUri uri, IndexDefinition indexDefinition) {
        mongoOperations.indexOps(getMongoCollectionName(uri)).ensureIndex(indexDefinition);
    }

    @Override
    public void dropIndex(ResourceUri uri, String indexId) {
        mongoOperations.indexOps(getMongoCollectionName(uri)).dropIndex(indexId);
    }


    @Override
    public JsonObject deleteResource(ResourceUri uri) {
        Criteria criteria = Criteria.where(_ID).is(uri.getTypeId());
        return findAndRemove(uri, criteria);
    }

    @Override
    public List<GenericDocument> deleteCollection(ResourceUri uri, Optional<List<ResourceQuery>> queries) {
        List<ResourceQuery> resourceQueries = queries.orElse(Collections.<ResourceQuery>emptyList());
        Criteria criteria = CriteriaBuilder.buildFromResourceQueries(resourceQueries);
        return findAllAndRemove(uri, criteria, GenericDocument.class);
    }

    @Override
    public List<GenericDocument> deleteRelation(ResourceUri uri, Optional<List<ResourceQuery>> queries) {
        List<ResourceQuery> resourceQueries = queries.orElse(Collections.<ResourceQuery>emptyList());
        Criteria criteria = CriteriaBuilder.buildFromResourceQueries(resourceQueries);
        if (!uri.isTypeWildcard()) {
            criteria = criteria.and(JsonRelation._SRC_ID).is(uri.getTypeId());
        }
        if (uri.getRelationId() != null) {
            criteria = criteria.and(JsonRelation._DST_ID).is(uri.getRelationId());
        }

        return findAllAndRemove(uri, criteria, RelationDocument.class).stream().map(document -> {
            return new GenericDocument().setId(document.getDstId());
        }).collect(Collectors.<GenericDocument>toList());
    }

    private <T extends GenericDocument> List<T> findAllAndRemove(ResourceUri resourceUri, Criteria criteria, Class<T> clazz) {
        return mongoOperations.findAllAndRemove(new Query(criteria), clazz, getMongoCollectionName(resourceUri));
    }

    private JsonObject findAndRemove(ResourceUri resourceUri, Criteria criteria) {
        return mongoOperations.findAndRemove(new Query(criteria), JsonObject.class, getMongoCollectionName(resourceUri));
    }


    @Override
    public void moveRelation(ResourceUri uri, RelationMoveOperation relationMoveOperation) {
        resmiOrder.moveRelation(uri, relationMoveOperation);
    }


    @Override
    public JsonElement count(ResourceUri resourceUri, List<ResourceQuery> resourceQueries) {
        Query query = new MongoResmiQueryBuilder().relationSubjectId(resourceUri).query(resourceQueries).build();
        if (resourceUri.isRelation()) {
            query.fields().exclude(_ID).exclude(JsonRelation._SRC_ID);
        }
        LOG.debug("Query executed : " + query.getQueryObject().toString());
        return aggregationResultsFactory.countResult(mongoOperations.count(query, getMongoCollectionName(resourceUri)));
    }

    @Override
    public JsonElement average(ResourceUri resourceUri, List<ResourceQuery> resourceQueries, String field) {
        List<DBObject> results =  aggregate(resourceUri, Optional.ofNullable(resourceQueries), group().avg(field).as(AVERAGE).count().as(COUNT));
        return results.isEmpty() || fieldNotExists(resourceUri, field, results, AVERAGE) ? aggregationResultsFactory.averageResult(Optional.empty()) :
                aggregationResultsFactory.averageResult(getAggregationResultValue(AVERAGE, results, resourceUri, field));
    }

    @Override
        public JsonElement sum(ResourceUri resourceUri, List<ResourceQuery> resourceQueries, String field) {
        List<DBObject> results = aggregate(resourceUri, Optional.ofNullable(resourceQueries), group().sum(field).as(SUM).count().as(COUNT));
        return aggregationResultsFactory.sumResult(getAggregationResultValue(SUM, results, resourceUri, field));
    }


    private Optional<Double> getAggregationResultValue(String operator, List<DBObject> results, ResourceUri resourceUri, String field) {
        if(!results.isEmpty()) {
            DBObject result = results.get(0);
            if(isValidAggregationResult(result, resourceUri, field, operator)) {
                return Optional.ofNullable((Number) result.get(operator)).map(Number::doubleValue);
            }
        }
        return Optional.empty();
    }

    private boolean isValidAggregationResult(DBObject result, ResourceUri resourceUri, String field, String operator) {
        Object aggregation = result.get(operator);
        int count = (int) result.get(COUNT);

        return !(count == 0 || ((aggregation.equals(0) || aggregation.equals(0.0)) && !collectionHasAtLeastOneEntryWithNumericInField(resourceUri, field)));
    }

    protected boolean fieldNotExists(ResourceUri resourceUri, String field, List<DBObject> results, String type) {
        Query query = Query.query(Criteria.where(field).exists(true));

        return ((results.get(0).get(type).equals(0) || results.get(0).get(type).equals(0.0)) && mongoOperations.count(query, getMongoCollectionName(resourceUri)) == 0);
    }

    protected boolean collectionHasAtLeastOneEntryWithNumericInField(ResourceUri resourceUri, String field) {
        Query query = Query.query(Criteria.where(field).type(MONGO_DOUBLE_TYPE).orOperator(Criteria.where(field).type(MONGO_INT_TYPE)
                .orOperator(Criteria.where(field).type(MONGO_LONG_TYPE))));

        return mongoOperations.count(query, getMongoCollectionName(resourceUri)) != 0;
    }

    @Override
    public JsonElement max(ResourceUri resourceUri, List<ResourceQuery> resourceQueries, String field) {
        List<DBObject> results = aggregate(resourceUri, Optional.ofNullable(resourceQueries), group().max(field).as("max"));
        return aggregationResultsFactory.maxResult(results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0).get("max")));
    }

    @Override
    public JsonElement min(ResourceUri resourceUri, List<ResourceQuery> resourceQueries, String field) {
        List<DBObject> results = aggregate(resourceUri, Optional.ofNullable(resourceQueries), group().min(field).as("min"));
        return aggregationResultsFactory.minResult(results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.get(0).get("min")));
    }

    @Override
    public JsonArray combine(ResourceUri resourceUri, Optional<List<ResourceQuery>> resourceQueries, Optional<Pagination> pagination,
                             Optional<Sort> sort, String field, String expression) throws InvalidApiParamException {

        MongoAggregationBuilder builder = new MongoAggregationBuilder();
        builder.match(resourceUri, resourceQueries);

        builder.projection(field, expression);

        if (sort.isPresent()) {
            builder.sort(sort.get().getDirection().toString(), sort.get().getField());
        }

        if (pagination.isPresent()) {
            builder.pagination(pagination.get());
        }

        Aggregation aggregation = builder.build();
        if(allowDiskUse){
            aggregation = withAllowDiskUseOption(aggregation);
        }

        List<JsonObject> results;
        try {
            results = mongoOperations.aggregate(aggregation, getMongoCollectionName(resourceUri), JsonObject.class)
                    .getMappedResults();
        } catch (SpelParseException spe) {
            throw new InvalidApiParamException(spe.getMessage());
        }

        return JsonUtils.convertToArray(results.stream().map(result -> {
            JsonObject document = result.get(MongoAggregationBuilder.DOCUMENT).getAsJsonObject();
            document.add(field, result.get(field));
            return document;
        }).collect(Collectors.toList()));
    }

    @Override
    public JsonElement histogram(ResourceUri resourceUri, List<ResourceQuery> resourceQueries, Optional<Pagination> pagination,
                                 Optional<Sort> sortParam, String field) {
        AggregationOperation[] aggregations = {
                group(Fields.from(Fields.field(field, field))).push("$_id").as("ids"),
                new ExposingFieldsCustomAggregationOperation(new BasicDBObject("$project", new BasicDBObject(COUNT, new BasicDBObject(
                        "$size", "$ids")))) {
                    @Override
                    public ExposedFields getFields() {
                        return ExposedFields.synthetic(Fields.fields(COUNT));
                    }
                }};

        if (sortParam.isPresent()) {
            Direction direction = Direction.ASC;
            if (sortParam.get().getDirection() == Sort.Direction.DESC) {
                direction = Direction.DESC;
            }
            aggregations = ArrayUtils.add(aggregations, sort(direction, sortParam.get().getField()));
        }

        if (pagination.isPresent()) {
            aggregations = ArrayUtils.addAll(aggregations, skip(pagination.get().getPage() * pagination.get().getPageSize()),
                    limit(pagination.get().getPageSize()));
        }

        List<HistogramEntry> results = aggregate(resourceUri, Optional.ofNullable(resourceQueries), aggregations).stream()
                .map(result -> toHistogramEntry(result, field)).collect(Collectors.toList());
        return aggregationResultsFactory.histogramResult(results.toArray(new HistogramEntry[results.size()]));
    }

    private HistogramEntry toHistogramEntry(DBObject result, String... fields) {
        long count = ((Number) result.get(COUNT)).longValue();
        Map<String, Object> values;
        if (fields.length == 1) {
            values = Collections.singletonMap(fields[0], result.get("_id"));
        } else {
            values = new HashMap<>(fields.length);
            result.keySet().stream().filter(f -> !COUNT.equals(f)).forEach(f -> values.put(f, result.get(f)));
        }

        return new HistogramEntry(count, values);
    }


    private String getMongoCollectionName(ResourceUri resourceUri) {
        return namespaceNormalizer.normalize(resourceUri.getDomain()) + DOMAIN_CONCATENATION +
                Optional.ofNullable(namespaceNormalizer.normalize(resourceUri.getType()))
                        .map(type -> type + Optional.ofNullable(resourceUri.getRelation())
                                .map(relation -> RELATION_CONCATENATION + namespaceNormalizer.normalize(relation)).orElse(EMPTY_STRING))
                        .orElse(EMPTY_STRING);
    }

    private class CustomAggregationOperation implements AggregationOperation {
        private final DBObject operation;

        public CustomAggregationOperation(DBObject operation) {
            this.operation = operation;
        }

        @Override
        public DBObject toDBObject(AggregationOperationContext context) {
            return context.getMappedObject(operation);
        }
    }

    private class ExposingFieldsCustomAggregationOperation extends CustomAggregationOperation implements FieldsExposingAggregationOperation {

        public ExposingFieldsCustomAggregationOperation(DBObject operation) {
            super(operation);
        }

        @Override
        public ExposedFields getFields() {
            return null;
        }
    }
}
