package com.rfid.circularlabs_rfid_backend.product.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductDetailHistory is a Querydsl query type for ProductDetailHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductDetailHistory extends EntityPathBase<ProductDetailHistory> {

    private static final long serialVersionUID = 2059854365L;

    private static final PathInits INITS = PathInits.DIRECT2;

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

    public final com.rfid.circularlabs_rfid_backend.scan.domain.QRfidScanHistory rfidScanHistory;

    public final StringPath status = createString("status");

    public final StringPath supplierCode = createString("supplierCode");

    public QProductDetailHistory(String variable) {
        this(ProductDetailHistory.class, forVariable(variable), INITS);
    }

    public QProductDetailHistory(Path<? extends ProductDetailHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductDetailHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductDetailHistory(PathMetadata metadata, PathInits inits) {
        this(ProductDetailHistory.class, metadata, inits);
    }

    public QProductDetailHistory(Class<? extends ProductDetailHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.rfidScanHistory = inits.isInitialized("rfidScanHistory") ? new com.rfid.circularlabs_rfid_backend.scan.domain.QRfidScanHistory(forProperty("rfidScanHistory")) : null;
    }

}

