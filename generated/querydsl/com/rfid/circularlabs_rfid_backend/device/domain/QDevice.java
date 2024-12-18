package com.rfid.circularlabs_rfid_backend.device.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QDevice is a Querydsl query type for Device
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDevice extends EntityPathBase<Device> {

    private static final long serialVersionUID = 801362890L;

    public static final QDevice device = new QDevice("device");

    public final com.rfid.circularlabs_rfid_backend.share.QTimeStamped _super = new com.rfid.circularlabs_rfid_backend.share.QTimeStamped(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath deviceCode = createString("deviceCode");

    public final NumberPath<Long> deviceId = createNumber("deviceId", Long.class);

    public final StringPath deviceType = createString("deviceType");

    public final StringPath email = createString("email");

    public final StringPath manager = createString("manager");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath phone = createString("phone");

    public final StringPath supplierCode = createString("supplierCode");

    public QDevice(String variable) {
        super(Device.class, forVariable(variable));
    }

    public QDevice(Path<? extends Device> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDevice(PathMetadata metadata) {
        super(Device.class, metadata);
    }

}

