package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.entity.OpenApiToken;

/**
 * Open API token管理
 * @author majianzheng
 */
public interface OpenApiService {

    /**
     * 创建Open API token
     * @param username 用户名
     * @param expireDate 过期时间
     * @return token
     */
    String createOpenApiToken(String username, Long expireDate);

    /**
     * 删除Open API token
     * @param id token id
     */
    void deleteOpenApiToken(Long id);

    /**
     * 获取Open API token列表
     * @param username 用户名
     * @param pageNo 开始页
     * @param pageSize 页大小
     * @return token列表
     */
    PagedList<OpenApiToken> getTokens(String username, int pageNo, int pageSize);
}
