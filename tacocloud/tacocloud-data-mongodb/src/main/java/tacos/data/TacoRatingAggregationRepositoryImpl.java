package tacos.data;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.TacoRating;

public class TacoRatingAggregationRepositoryImpl implements TacoRatingAggregationRepository {
  private final ReactiveMongoTemplate template;

  public TacoRatingAggregationRepositoryImpl(ReactiveMongoTemplate template) {
    this.template = template;
  }

  @Override
  public Mono<TacoRating> upsert(String userId, String tacoId, int score) {
    return save(userId, tacoId, score, true);
  }

  @Override
  public Mono<TacoRating> update(String userId, String tacoId, int score) {
    return save(userId, tacoId, score, false);
  }

  @Override
  public Mono<TacoRatingAggregate> summarize(String tacoId) {
    List<AggregationOperation> operations = stages(tacoId, 0);
    operations.add(stage(new Document("$limit", 1)));
    return aggregate(operations).next();
  }

  @Override
  public Flux<TacoRatingAggregate> top(int minimumVotes, int limit) {
    List<AggregationOperation> operations = stages(null, minimumVotes);
    operations.add(stage(new Document("$limit", limit)));
    return aggregate(operations);
  }

  private Mono<TacoRating> save(String userId, String tacoId, int score, boolean upsert) {
    Date now = new Date();
    Query query = Query.query(where("userId").is(userId).and("tacoId").is(tacoId));
    Update update = new Update().set("score", score).set("updatedAt", now)
        .setOnInsert("userId", userId).setOnInsert("tacoId", tacoId).setOnInsert("createdAt", now);
    return template.findAndModify(query, update,
        FindAndModifyOptions.options().upsert(upsert).returnNew(true), TacoRating.class);
  }

  private List<AggregationOperation> stages(String tacoId, int minimumVotes) {
    List<AggregationOperation> operations = new ArrayList<>();
    if (tacoId != null) operations.add(stage(new Document("$match", new Document("tacoId", tacoId))));
    operations.add(stage(new Document("$group", new Document("_id",
        new Document("tacoId", "$tacoId").append("score", "$score"))
        .append("votes", new Document("$sum", 1)))));
    operations.add(stage(new Document("$group", new Document("_id", "$_id.tacoId")
        .append("count", new Document("$sum", "$votes"))
        .append("weighted", new Document("$sum", new Document("$multiply",
            List.of("$_id.score", "$votes"))))
        .append("distribution", new Document("$push",
            new Document("score", "$_id.score").append("count", "$votes"))))));
    operations.add(stage(new Document("$match", new Document("count",
        new Document("$gte", minimumVotes)))));
    operations.add(stage(new Document("$addFields", new Document("average",
        new Document("$divide", List.of("$weighted", "$count"))))));
    operations.add(stage(new Document("$lookup", new Document("from", "taco")
        .append("localField", "_id").append("foreignField", "_id").append("as", "taco"))));
    operations.add(stage(new Document("$unwind", "$taco")));
    operations.add(stage(new Document("$match", new Document("taco.published",
        new Document("$ne", false)))));
    operations.add(stage(new Document("$sort", new Document("average", -1)
        .append("count", -1).append("_id", 1))));
    operations.add(stage(new Document("$project", new Document("_id", 0)
        .append("tacoId", "$_id").append("taco", 1).append("average", 1)
        .append("count", 1).append("distribution", 1))));
    return operations;
  }

  private Flux<TacoRatingAggregate> aggregate(List<AggregationOperation> operations) {
    return template.aggregate(Aggregation.newAggregation(operations), TacoRating.class,
        TacoRatingAggregate.class);
  }

  private AggregationOperation stage(Document document) {
    return context -> document;
  }
}
