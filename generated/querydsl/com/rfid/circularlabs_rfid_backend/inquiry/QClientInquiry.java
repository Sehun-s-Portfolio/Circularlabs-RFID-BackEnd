package com.rfid.circularlabs_rfid_backend.inquiry;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QClientInquiry is a Querydsl query type for ClientInquiry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClientInquiry extends EntityPathBase<ClientInquiry> {

    private static final long serialVersionUID = -1262555853L;

    public static final QClientInquiry clientInquiry = new QClientInquiry("clientInquiry");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final DateTimePath<java.time.LocalDateTime> answerAt = createDateTime("answerAt", java.time.LocalDateTime.class);

    public final StringPath answerContent = createString("answerContent");

    public final StringPath answerStatus = createString("answerStatus");

    public final StringPath classificationCode = createString("classificationCode");

    public final NumberPath<Long> clientInquiryId = createNumber("clientInquiryId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath motherCode = createString("motherCode");

    public final StringPath title = createString("title");

    public QClientInquiry(String variable) {
        super(ClientInquiry.class, forVariable(variable));
    }

    public QClientInquiry(Path<? extends ClientInquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QClientInquiry(PathMetadata metadata) {
        super(ClientInquiry.class, metadata);
    }

}

