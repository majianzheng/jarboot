package io.github.majianzheng.jarboot.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Open API token
 * @author mazheng
 */
@EqualsAndHashCode(callSuper = true)
@Table(name = OpenApiToken.TABLE_NAME)
@Entity
@Data
public class OpenApiToken extends AbstractBaseEntity {
    public static final String TABLE_NAME = "open_api_token";
    private String username;
    private String token;
    private Long expireTime;
}
