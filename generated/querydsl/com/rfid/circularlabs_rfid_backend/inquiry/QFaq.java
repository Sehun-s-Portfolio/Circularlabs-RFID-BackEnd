package com.rfid.circularlabs_rfid_backend.inquiry;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QFaq is a Querydsl query type for Faq
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFaq extends EntityPathBase<Faq> {

    private static final long serialVersionUID = 1935179149L;

    public static final QFaq faq = new QFaq("faq");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath answer = createString("answer");

    public final StringPath classificationCode = createString("classificationCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> faqId = createNumber("faqId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath question = createString("question");

    public QFaq(String variable) {
        super(Faq.class, forVariable(variable));
    }

    public QFaq(Path<? extends Faq> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFaq(PathMetadata metadata) {
        super(Faq.class, metadata);
    }

}

