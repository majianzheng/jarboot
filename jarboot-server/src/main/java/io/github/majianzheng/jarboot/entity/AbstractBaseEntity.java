package io.github.majianzheng.jarboot.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * @author majianzheng
 */
@MappedSuperclass
public abstract class AbstractBaseEntity implements Serializable {
    protected Long id;

    /**
     * 获取主键id
     * @return id
     * 前端js能处理的长度低于Java，防止精度丢失
     */
    @Id
    @GenericGenerator(name="snowFlakeIdGenerator", strategy="io.github.majianzheng.jarboot.idgenerator.SnowFlakeIdGenerator")
    @GeneratedValue(generator="snowFlakeIdGenerator")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Long getId() {
        return id;
    }

    /**
     * 设置主键id
     * @param id 主键id
     */
    public void setId(Long id) {
        this.id = id;
    }
}
