package com.rfid.circularlabs_rfid_backend.product;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductDetailHistory is a Querydsl query type for ProductDetailHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductDetailHistory extends EntityPathBase<ProductDetailHistory> {

    private static final long serialVersionUID = 885778069L;

    public static final QProductDetailHistory productDetailHistory = new QProductDetailHistory("productDetailHistory");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath clientCode = createString("clientCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> cycle = createNumber("cycle", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> latestReadingAt = createDateTime("latestReadingAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath productCode = createString("productCode");

    public final NumberPath<Long> productDetailHistoryId = createNumber("productDetailHistoryId", Long.class);

    public final StringPath productSerialCode = createString("productSerialCode");

    public final StringPath rfidChipCode = createString("rfidChipCode");

    public final StringPath status = createString("status");

    public final StringPath supplierCode = createString("supplierCode");

    public QProductDetailHistory(String variable) {
        super(ProductDetailHistory.class, forVariable(variable));
    }

    public QProductDetailHistory(Path<? extends ProductDetailHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductDetailHistory(PathMetadata metadata) {
        super(ProductDetailHistory.class, metadata);
    }

}

