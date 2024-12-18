package com.rfid.circularlabs_rfid_backend.process.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QRecall is a Querydsl query type for Recall
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecall extends EntityPathBase<Recall> {

    private static final long serialVersionUID = 11237402L;

    public static final QRecall recall = new QRecall("recall");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath classificationCode = createString("classificationCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final DateTimePath<java.time.LocalDateTime> possibleRecallAt = createDateTime("possibleRecallAt", java.time.LocalDateTime.class);

    public final StringPath productCode = createString("productCode");

    public final NumberPath<Long> recallId = createNumber("recallId", Long.class);

    public final NumberPath<Integer> recallMount = createNumber("recallMount", Integer.class);

    public QRecall(String variable) {
        super(Recall.class, forVariable(variable));
    }

    public QRecall(Path<? extends Recall> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRecall(PathMetadata metadata) {
        super(Recall.class, metadata);
    }

}

