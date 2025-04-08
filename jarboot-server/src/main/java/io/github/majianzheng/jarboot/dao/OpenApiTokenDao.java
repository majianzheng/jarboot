package io.github.majianzheng.jarboot.dao;

import io.github.majianzheng.jarboot.entity.OpenApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 *
 * @author majianzheng
 */
@Repository
public interface OpenApiTokenDao extends JpaRepository<OpenApiToken, Long> {

}
