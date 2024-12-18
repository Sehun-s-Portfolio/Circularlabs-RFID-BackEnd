package com.rfid.circularlabs_rfid_backend.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -1722948270L;

    public static final QMember member = new QMember("member1");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    public final StringPath address = createString("address");

    public final StringPath bankAccountName = createString("bankAccountName");

    public final StringPath bankAccountNumber = createString("bankAccountNumber");

    public final StringPath bankName = createString("bankName");

    public final StringPath businessNumber = createString("businessNumber");

    public final StringPath categoryBusiness = createString("categoryBusiness");

    public final StringPath classificationCode = createString("classificationCode");

    public final StringPath clientCompany = createString("clientCompany");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final StringPath etc = createString("etc");

    public final NumberPath<Integer> grade = createNumber("grade", Integer.class);

    public final StringPath lastOrders = createString("lastOrders");

    public final StringPath loginId = createString("loginId");

    public final StringPath manager = createString("manager");

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath motherCode = createString("motherCode");

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final StringPath position = createString("position");

    public final StringPath represent = createString("represent");

    public final StringPath stateOfBusiness = createString("stateOfBusiness");

    public final StringPath withDrawal = createString("withDrawal");

    public final DateTimePath<java.time.LocalDateTime> withDrawalDate = createDateTime("withDrawalDate", java.time.LocalDateTime.class);

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

