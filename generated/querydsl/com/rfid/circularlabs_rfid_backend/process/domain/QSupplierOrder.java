package com.rfid.circularlabs_rfid_backend.process.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QSupplierOrder is a Querydsl query type for SupplierOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSupplierOrder extends EntityPathBase<SupplierOrder> {

    private static final long serialVersionUID = -894627911L;

    public static final QSupplierOrder supplierOrder = new QSupplierOrder("supplierOrder");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath classificationCode = createString("classificationCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deliveryAt = createDateTime("deliveryAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public final NumberPath<Integer> orderMount = createNumber("orderMount", Integer.class);

    public final StringPath productCode = createString("productCode");

    public final StringPath statementnumber = createString("statementnumber");

    public QSupplierOrder(String variable) {
        super(SupplierOrder.class, forVariable(variable));
    }

    public QSupplierOrder(Path<? extends SupplierOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSupplierOrder(PathMetadata metadata) {
        super(SupplierOrder.class, metadata);
    }

}

