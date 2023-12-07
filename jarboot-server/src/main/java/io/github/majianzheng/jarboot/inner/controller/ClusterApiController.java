package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.cluster.ClusterEventMessage;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.FileService;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 集群API服务
 * 仅限集群内部IP访问，保证集群内部使用统一的secret key
 * 普通界面登录使用的token禁用访问
 * @author mazheng
 */
@RequestMapping(value = CommonConst.CLUSTER_API_CONTEXT)
@RestController
@PreAuthorize("hasRole('CLUSTER')")
public class ClusterApiController {
    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ServerRuntimeService serverRuntimeService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private FileService fileService;

    @GetMapping("/group")
    @ResponseBody
    public ServiceInstance getServiceGroup() {
        return taskRunCache.getServiceGroup(SettingUtils.getCurrentUserDir());
    }

    @GetMapping("/jvmGroup")
    @ResponseBody
    public JvmProcess getJvmGroup() {
        return serviceManager.getJvmGroup();
    }

    @GetMapping("/serviceSetting")
    @ResponseBody
    public ServiceSetting getServiceSetting(String serviceName) {
        return settingService.getServiceSetting(serviceName);
    }

    @PostMapping("/serviceSetting")
    @ResponseBody
    public ResponseSimple saveServiceSetting(@RequestBody ServiceSetting setting) {
        settingService.submitServiceSetting(setting);
        return HttpResponseUtils.success();
    }

    @DeleteMapping("/service")
    @ResponseBody
    public ResponseSimple deleteService(String serviceName) {
        serviceManager.deleteService(serviceName);
        return HttpResponseUtils.success();
    }

    @GetMapping("/attach")
    @ResponseBody
    public ResponseSimple attach(String pid) {
        serviceManager.attach(pid);
        return HttpResponseUtils.success();
    }

    @PostMapping("/handleMessage/{host}")
    @ResponseBody
    public ResponseSimple handleMessage(@RequestBody ClusterEventMessage eventMessage, @PathVariable("host") String host) {
        ClusterClient client = ClusterClientManager.getInstance().getClient(host);
        if (null == client) {
            return HttpResponseUtils.error("集群客户端不存在" + host);
        }
        client.handleMessage(eventMessage);
        return HttpResponseUtils.success();
    }

    @PostMapping("file")
    @ResponseBody
    public ResponseSimple upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("path") String path) throws IOException {
        try (InputStream is = file.getInputStream()) {
            fileService.uploadFile(path + File.separator + file.getOriginalFilename(), is);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 下载文件
     * @param path 文件
     * @param response
     * @throws IOException
     */
    @PostMapping("file/download")
    public void download(
            @RequestParam("path") String path,
            HttpServletResponse response) throws IOException {
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
        try (OutputStream os = response.getOutputStream()) {
            fileService.download(path, os);
        }
    }

    /**
     * 获取文件列表
     * @param baseDir 文件目录
     * @param withRoot 是否包含baseDir
     * @return 文件列表
     */
    @PostMapping("file/list")
    public List<FileNode> getFiles(String baseDir, boolean withRoot) {
        return fileService.getWorkspaceFiles(baseDir, withRoot);
    }

    /**
     * 获取文件内容
     * @param file 文件相对于工作目录的路径
     * @return 文件内容
     */
    @PostMapping("file/content")
    public ResponseVo<String> getContent(@RequestParam("path") String file) {
        return HttpResponseUtils.success(fileService.getContent(file));
    }

    /**
     * 删除文件
     * @param path 文件相对于工作目录的路径
     * @return
     */
    @PostMapping("file/delete")
    public ResponseVo<String> deleteFile(@RequestParam("path") String path) {
        fileService.deleteFile(path);
        return HttpResponseUtils.success();
    }

    /**
     * 写文件
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return
     */
    @PostMapping("file/write")
    public ResponseVo<String> writeFile(
            @RequestParam("path") String path,
            @RequestParam("content") String content) {
        return HttpResponseUtils.success(fileService.writeFile(path, content));
    }

    /**
     * 创建文本文件
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return
     */
    @PostMapping("file/create")
    public ResponseVo<String> newFile(@RequestParam("path") String path, @RequestParam("content") String content) {
        return HttpResponseUtils.success(fileService.newFile(path, content));
    }

    /**
     * 创建文件夹
     * @param file 文件相对于工作目录的路径
     * @return
     */
    @PostMapping("directory")
    public ResponseVo<String> addDirectory(@RequestParam("path") String file) {
        return HttpResponseUtils.success(fileService.addDirectory(file));
    }

    /**
     * 导出服务
     * @param name 服务名
     * @param response Servlet response
     * @throws IOException IO 异常
     */
    @GetMapping(value="/exportService")
    public void exportService(@RequestParam String name, HttpServletResponse response) throws IOException {
        CommonUtils.setDownloadHeader(response, name + ".zip");
        try (OutputStream os = response.getOutputStream()) {
            serverRuntimeService.exportService(name, os);
        }
    }

    /**
     * 导入服务
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping("/importService")
    @ResponseBody
    public ResponseVo<String> importService(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            serverRuntimeService.importService(file.getOriginalFilename(), is);
        } catch (Exception e) {
            return HttpResponseUtils.error(e.getMessage());
        }
        return HttpResponseUtils.success();
    }

    /**
     * 从服务器下载文件
     * @param file base64编码的文件全路径名
     * @param response Servlet response
     */
    @GetMapping(value="/download/{file}")
    public void downloadAnyFile(@PathVariable("file") String file, HttpServletResponse response) throws IOException {
        CommonUtils.setDownloadHeader(response, null);
        try (OutputStream os = response.getOutputStream()) {
            serverRuntimeService.downloadAnyFile(file, os);
        }
    }
}
