package com.rfid.circularlabs_rfid_backend.inquiry;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOrderInquiry is a Querydsl query type for OrderInquiry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderInquiry extends EntityPathBase<OrderInquiry> {

    private static final long serialVersionUID = -1843639806L;

    public static final QOrderInquiry orderInquiry = new QOrderInquiry("orderInquiry");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath classificationCode = createString("classificationCode");

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final NumberPath<Long> orderInquiryId = createNumber("orderInquiryId", Long.class);

    public final StringPath title = createString("title");

    public QOrderInquiry(String variable) {
        super(OrderInquiry.class, forVariable(variable));
    }

    public QOrderInquiry(Path<? extends OrderInquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrderInquiry(PathMetadata metadata) {
        super(OrderInquiry.class, metadata);
    }

}

