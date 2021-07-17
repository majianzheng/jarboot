package com.mz.jarboot.base;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.security.PermissionInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.mz.jarboot.constant.AuthConst.REQUEST_PATH_SEPARATOR;

@Component
public class PermissionsCache {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsCache.class);
    
    private final ConcurrentHashMap<RequestMappingInfo, PermissionInfo> methods = new ConcurrentHashMap<>();
    
    private final ConcurrentHashMap<String, List<RequestMappingInfo>> urlLookup = new ConcurrentHashMap<>();

    private final ArrayList<PermissionInfo> permissionInfos = new ArrayList<>();

    public PermissionInfo getMethod(HttpServletRequest request) {
        String path = getPath(request);
        String httpMethod = request.getMethod();
        String urlKey = httpMethod + REQUEST_PATH_SEPARATOR + path;
        List<RequestMappingInfo> requestMappingInfos = urlLookup.get(urlKey);
        if (CollectionUtils.isEmpty(requestMappingInfos)) {
            return null;
        }
        List<RequestMappingInfo> matchedInfo = findMatchedInfo(requestMappingInfos, request);
        if (CollectionUtils.isEmpty(matchedInfo)) {
            return null;
        }
        RequestMappingInfo bestMatch = matchedInfo.get(0);
        if (matchedInfo.size() > 1) {
            RequestMappingInfo.RequestMappingInfoComparator comparator = new RequestMappingInfo.RequestMappingInfoComparator();
            matchedInfo.sort(comparator);
            bestMatch = matchedInfo.get(0);
            RequestMappingInfo secondBestMatch = matchedInfo.get(1);
            if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                throw new IllegalStateException(
                        "Ambiguous methods mapped for '" + request.getRequestURI() + "': {" + bestMatch + ", "
                                + secondBestMatch + "}");
            }
        }
        return methods.get(bestMatch);
    }

    public List<PermissionInfo> getPermissionInfos() {
        return permissionInfos;
    }
    
    private String getPath(HttpServletRequest request) {
        try {
            return new URI(request.getRequestURI()).getPath();
        } catch (URISyntaxException e) {
            logger.error("parse request to path error", e);
            throw new MzException(404, "Invalid URI");
        }
    }
    
    private List<RequestMappingInfo> findMatchedInfo(List<RequestMappingInfo> requestMappingInfos,
            HttpServletRequest request) {
        ArrayList<RequestMappingInfo> matchedInfo = new ArrayList<>();
        for (RequestMappingInfo requestMappingInfo : requestMappingInfos) {
            ParamRequestCondition matchingCondition = requestMappingInfo.getParamRequestCondition()
                    .getMatchingCondition(request);
            if (matchingCondition != null) {
                matchedInfo.add(requestMappingInfo);
            }
        }
        return matchedInfo;
    }
    
    /**
     * find target method from class list.
     *
     * @param classesList class list
     */
    public void initClassMethod(Set<Class<?>> classesList) {
        for (Class<?> clazz : classesList) {
            initClassMethod(clazz);
        }
    }
    
    /**
     * find target method from target class.
     *
     * @param clazz {@link Class}
     */
    private void initClassMethod(Class<?> clazz) {
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        for (String classPath : requestMapping.value()) {
            for (Method method : clazz.getMethods()) {
                doInitClassMethod(classPath, method);
            }
        }
    }

    private void doInitClassMethod(String classPath, Method method) {
        RequestMapping requestMapping;
        Permission permission = method.getAnnotation(Permission.class);
        if (null == permission) {
            return;
        }
        if (!method.isAnnotationPresent(RequestMapping.class)) {
            parseSubAnnotations(method, classPath);
            return;
        }
        requestMapping = method.getAnnotation(RequestMapping.class);
        RequestMethod[] requestMethods = requestMapping.method();
        if (requestMethods.length == 0) {
            requestMethods = new RequestMethod[1];
            requestMethods[0] = RequestMethod.GET;
        }
        for (String methodPath : requestMapping.value()) {
            String urlKey = requestMethods[0].name() + REQUEST_PATH_SEPARATOR + classPath + methodPath;
            addUrlAndMethodRelation(urlKey, requestMapping.params(), method);
        }
    }

    private void parseSubAnnotations(Method method, String classPath) {
        
        final GetMapping getMapping = method.getAnnotation(GetMapping.class);
        final PostMapping postMapping = method.getAnnotation(PostMapping.class);
        final PutMapping putMapping = method.getAnnotation(PutMapping.class);
        final DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        final PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        
        if (getMapping != null) {
            put(RequestMethod.GET, classPath, getMapping.value(), getMapping.params(), method);
        }
        
        if (postMapping != null) {
            put(RequestMethod.POST, classPath, postMapping.value(), postMapping.params(), method);
        }
        
        if (putMapping != null) {
            put(RequestMethod.PUT, classPath, putMapping.value(), putMapping.params(), method);
        }
        
        if (deleteMapping != null) {
            put(RequestMethod.DELETE, classPath, deleteMapping.value(), deleteMapping.params(), method);
        }
        
        if (patchMapping != null) {
            put(RequestMethod.PATCH, classPath, patchMapping.value(), patchMapping.params(), method);
        }
        
    }
    
    private void put(RequestMethod requestMethod, String classPath, String[] requestPaths, String[] requestParams,
            Method method) {
        if (ArrayUtils.isEmpty(requestPaths)) {
            String urlKey = requestMethod.name() + REQUEST_PATH_SEPARATOR + classPath;
            addUrlAndMethodRelation(urlKey, requestParams, method);
            return;
        }
        for (String requestPath : requestPaths) {
            String urlKey = requestMethod.name() + REQUEST_PATH_SEPARATOR + classPath + requestPath;
            addUrlAndMethodRelation(urlKey, requestParams, method);
        }
    }
    
    private void addUrlAndMethodRelation(String urlKey, String[] requestParam, Method method) {
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo();
        requestMappingInfo.setPathRequestCondition(new PathRequestCondition(urlKey));
        requestMappingInfo.setParamRequestCondition(new ParamRequestCondition(requestParam));
        List<RequestMappingInfo> requestMappingInfos = urlLookup.get(urlKey);
        if (requestMappingInfos == null) {
            urlLookup.putIfAbsent(urlKey, new ArrayList<>());
            requestMappingInfos = urlLookup.get(urlKey);
            String urlKeyBackup = urlKey + "/";
            urlLookup.putIfAbsent(urlKeyBackup, requestMappingInfos);
        }
        requestMappingInfos.add(requestMappingInfo);
        Permission permission = method.getAnnotation(Permission.class);
        PermissionInfo info = new PermissionInfo();
        info.setResource(urlKey);
        String name = StringUtils.isEmpty(permission.value()) ? method.getName() : permission.value();
        info.setName(name);
        info.setRole(permission.role());
        methods.put(requestMappingInfo, info);
        permissionInfos.add(info);
    }
}
