package com.rfid.circularlabs_rfid_backend.product;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSupplyProduct is a Querydsl query type for SupplyProduct
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSupplyProduct extends EntityPathBase<SupplyProduct> {

    private static final long serialVersionUID = -398342177L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSupplyProduct supplyProduct = new QSupplyProduct("supplyProduct");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath classificationCode = createString("classificationCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> maximumOrderMount = createNumber("maximumOrderMount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final QProduct product;

    public final NumberPath<Integer> rentalPrice = createNumber("rentalPrice", Integer.class);

    public final NumberPath<Long> supplyProductId = createNumber("supplyProductId", Long.class);

    public QSupplyProduct(String variable) {
        this(SupplyProduct.class, forVariable(variable), INITS);
    }

    public QSupplyProduct(Path<? extends SupplyProduct> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSupplyProduct(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSupplyProduct(PathMetadata metadata, PathInits inits) {
        this(SupplyProduct.class, metadata, inits);
    }

    public QSupplyProduct(Class<? extends SupplyProduct> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

