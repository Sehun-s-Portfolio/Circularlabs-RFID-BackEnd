package com.rfid.circularlabs_rfid_backend.product.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClientProduct is a Querydsl query type for ClientProduct
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClientProduct extends EntityPathBase<ClientProduct> {

    private static final long serialVersionUID = 2107259643L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClientProduct clientProduct = new QClientProduct("clientProduct");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath ClientClassficationCode = createString("ClientClassficationCode");

    public final NumberPath<Long> clientProductId = createNumber("clientProductId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final QSupplyProduct supplyProduct;

    public QClientProduct(String variable) {
        this(ClientProduct.class, forVariable(variable), INITS);
    }

    public QClientProduct(Path<? extends ClientProduct> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClientProduct(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClientProduct(PathMetadata metadata, PathInits inits) {
        this(ClientProduct.class, metadata, inits);
    }

    public QClientProduct(Class<? extends ClientProduct> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.supplyProduct = inits.isInitialized("supplyProduct") ? new QSupplyProduct(forProperty("supplyProduct"), inits.get("supplyProduct")) : null;
    }

}

