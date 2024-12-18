package com.rfid.circularlabs_rfid_backend.process.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QDiscardHistory is a Querydsl query type for DiscardHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDiscardHistory extends EntityPathBase<DiscardHistory> {

    private static final long serialVersionUID = 1985367583L;

    public static final QDiscardHistory discardHistory = new QDiscardHistory("discardHistory");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath clientCode = createString("clientCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> discardAt = createDateTime("discardAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> discardHistoryId = createNumber("discardHistoryId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath productCode = createString("productCode");

    public final StringPath productSerialCode = createString("productSerialCode");

    public final StringPath reason = createString("reason");

    public final StringPath rfidChipCode = createString("rfidChipCode");

    public final StringPath supplierCode = createString("supplierCode");

    public QDiscardHistory(String variable) {
        super(DiscardHistory.class, forVariable(variable));
    }

    public QDiscardHistory(Path<? extends DiscardHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDiscardHistory(PathMetadata metadata) {
        super(DiscardHistory.class, metadata);
    }

}

