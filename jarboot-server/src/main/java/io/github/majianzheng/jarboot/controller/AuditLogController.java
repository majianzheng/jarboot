package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.common.annotation.PrivilegeCheck;
import io.github.majianzheng.jarboot.common.pojo.PagedList;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.dao.AuditLogDao;
import io.github.majianzheng.jarboot.entity.AuditLog;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 操作日志
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/auditLog")
@RestController
@PrivilegeCheck(value = "AUDIT_LOG")
public class AuditLogController {
    @Resource
    private AuditLogDao auditLogDao;


    /**
     * 查询操作日志
     * @param username 用户
     * @param operation 操作
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 操作日志
     */
    @GetMapping
    public ResponseVo<PagedList<AuditLog>> getLogs(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "operation", required = false) String operation,
            @RequestParam(name = "page") int pageNo,
            @RequestParam(name = "limit") int pageSize) {
        PageRequest page = PageRequest.of(pageNo, pageSize).withSort(Sort.Direction.DESC, "createTime");
        ExampleMatcher match = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.STARTING);
        if (StringUtils.isNotEmpty(operation)) {
            match = match.withMatcher("operation", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        if (StringUtils.isNotEmpty(username)) {
            match = match.withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains());
        }
        Page<AuditLog> all;
        if (StringUtils.isNotEmpty(operation) || StringUtils.isNotEmpty(username)) {
            AuditLog query = new AuditLog();
            query.setUsername(username);
            query.setOperation(operation);
            all = auditLogDao.findAll(Example.of(query, match), page);
        } else {
            all = auditLogDao.findAll(page);
        }
        return HttpResponseUtils.success(new PagedList<>(all.getContent(), all.getTotalElements()));
    }
}
