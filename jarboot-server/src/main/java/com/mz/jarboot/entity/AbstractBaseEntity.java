package com.mz.jarboot.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractBaseEntity implements Serializable {
    protected Long id;

    @Id
    @GenericGenerator(name="snowFlakeIdGenerator", strategy="com.mz.jarboot.idgenerator.SnowFlakeIdGenerator")
    @GeneratedValue(generator="snowFlakeIdGenerator")
    @JsonFormat(shape = JsonFormat.Shape.STRING)  //前端js能处理的长度低于Java，防止精度丢失
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
