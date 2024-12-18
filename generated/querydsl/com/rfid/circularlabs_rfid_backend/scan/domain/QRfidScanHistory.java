package com.rfid.circularlabs_rfid_backend.scan.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRfidScanHistory is a Querydsl query type for RfidScanHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRfidScanHistory extends EntityPathBase<RfidScanHistory> {

    private static final long serialVersionUID = 1667928589L;

    public static final QRfidScanHistory rfidScanHistory = new QRfidScanHistory("rfidScanHistory");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath clientCode = createString("clientCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath deviceCode = createString("deviceCode");

    public final NumberPath<Integer> flowRemainQuantity = createNumber("flowRemainQuantity", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> latestReadingAt = createDateTime("latestReadingAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final NumberPath<Integer> noReturnQuantity = createNumber("noReturnQuantity", Integer.class);

    public final StringPath productCode = createString("productCode");

    public final ListPath<com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory, com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory> productDetailHistories = this.<com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory, com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory>createList("productDetailHistories", com.rfid.circularlabs_rfid_backend.product.domain.ProductDetailHistory.class, com.rfid.circularlabs_rfid_backend.product.domain.QProductDetailHistory.class, PathInits.DIRECT2);

    public final StringPath rfidChipCode = createString("rfidChipCode");

    public final NumberPath<Long> rfidScanhistoryId = createNumber("rfidScanhistoryId", Long.class);

    public final StringPath status = createString("status");

    public final NumberPath<Integer> statusCount = createNumber("statusCount", Integer.class);

    public final StringPath supplierCode = createString("supplierCode");

    public final NumberPath<Integer> totalRemainQuantity = createNumber("totalRemainQuantity", Integer.class);

    public QRfidScanHistory(String variable) {
        super(RfidScanHistory.class, forVariable(variable));
    }

    public QRfidScanHistory(Path<? extends RfidScanHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRfidScanHistory(PathMetadata metadata) {
        super(RfidScanHistory.class, metadata);
    }

}

