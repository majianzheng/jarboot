package com.mz.jarboot.service;

import com.mz.jarboot.dto.ServerRunningDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServerMgrService {

    /**
     * 获取服务列表
     * @return 服务列表
     */
    List<ServerRunningDTO> getServerList();

    /**
     * 一键重启，杀死所有服务进程，根据依赖重启
     */
    void oneClickRestart();

    /**
     * 一键启动，根据依赖重启
     */
    void oneClickStart();

    /**
     * 一键停止，杀死所有服务进程
     */
    void oneClickStop();

    /**
     * 启动服务
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    void startServer(List<String> p);

    /**
     * 停止服务
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    void stopServer(List<String> p);

    /**
     * 重启服务
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    void restartServer(List<String> p);

    /**
     * 更新或新增服务的文件
     * @param file 上传的jar或zip文件
     * @param server 服务名
     */
    void uploadJarFiles(MultipartFile file, String server);
}
