package com.rfid.circularlabs_rfid_backend.product;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductDetail is a Querydsl query type for ProductDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductDetail extends EntityPathBase<ProductDetail> {

    private static final long serialVersionUID = 1740065023L;

    public static final QProductDetail productDetail = new QProductDetail("productDetail");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath clientCode = createString("clientCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> cycle = createNumber("cycle", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> latestReadingAt = createDateTime("latestReadingAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath productCode = createString("productCode");

    public final NumberPath<Long> productDetailId = createNumber("productDetailId", Long.class);

    public final StringPath productSerialCode = createString("productSerialCode");

    public final StringPath rfidChipCode = createString("rfidChipCode");

    public final StringPath status = createString("status");

    public final StringPath supplierCode = createString("supplierCode");

    public QProductDetail(String variable) {
        super(ProductDetail.class, forVariable(variable));
    }

    public QProductDetail(Path<? extends ProductDetail> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductDetail(PathMetadata metadata) {
        super(ProductDetail.class, metadata);
    }

}

