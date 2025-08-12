package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.dao.OpenApiTokenDao;
import io.github.majianzheng.jarboot.entity.OpenApiToken;
import io.github.majianzheng.jarboot.security.JwtTokenManager;
import io.github.majianzheng.jarboot.service.OpenApiService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Open API token管理
 * @author majianzheng
 */
@Service
public class OpenApiServiceImpl implements OpenApiService {
    @Resource
    private OpenApiTokenDao openApiTokenDao;
    @Resource
    private JwtTokenManager jwtTokenManager;

    @Override
    public String createOpenApiToken(String username, Long expireTimestamp) {
        Date expireDate = null;
        if (null != expireTimestamp && expireTimestamp > 0) {
            expireDate = new Date(expireTimestamp);
        }
        String token = jwtTokenManager.createOpenApiToken(username, expireDate);
        OpenApiToken openApiToken = new OpenApiToken();
        openApiToken.setUsername(username);
        openApiToken.setToken(token);
        openApiToken.setExpireTime(expireTimestamp);
        openApiTokenDao.save(openApiToken);
        return token;
    }

    @Override
    public void deleteOpenApiToken(Long id) {
        openApiTokenDao.deleteById(id);
    }

    @Override
    public PagedList<OpenApiToken> getTokens(String username, int pageNo, int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize);
        ExampleMatcher match = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.STARTING);
        if (StringUtils.isNotEmpty(username)) {
            match = match.withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        Page<OpenApiToken> all;
        if (StringUtils.isNotEmpty(username)) {
            OpenApiToken query = new OpenApiToken();
            query.setUsername(username);
            all = openApiTokenDao.findAll(Example.of(query, match), page);
        } else {
            all = openApiTokenDao.findAll(page);
        }
        List<OpenApiToken> result = all.getContent();
        return new PagedList<>(result, all.getTotalElements());
    }
}
