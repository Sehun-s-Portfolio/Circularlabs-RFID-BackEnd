package com.rfid.circularlabs_rfid_backend.process;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QClientOrder is a Querydsl query type for ClientOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClientOrder extends EntityPathBase<ClientOrder> {

    private static final long serialVersionUID = 1720757858L;

    public static final QClientOrder clientOrder = new QClientOrder("clientOrder");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath classificationCode = createString("classificationCode");

    public final NumberPath<Long> clientOrderId = createNumber("clientOrderId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deliveryAt = createDateTime("deliveryAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath modifiedYN = createString("modifiedYN");

    public final StringPath motherCode = createString("motherCode");

    public final NumberPath<Integer> orderMount = createNumber("orderMount", Integer.class);

    public final StringPath productCode = createString("productCode");

    public QClientOrder(String variable) {
        super(ClientOrder.class, forVariable(variable));
    }

    public QClientOrder(Path<? extends ClientOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QClientOrder(PathMetadata metadata) {
        super(ClientOrder.class, metadata);
    }

}

