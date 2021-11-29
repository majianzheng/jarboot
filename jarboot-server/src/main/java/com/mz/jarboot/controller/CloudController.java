package com.mz.jarboot.controller;

import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.*;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.event.WsEventEnum;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/cloud")
@Controller
public class CloudController {
    /**
     * 拉取服务目录
     * @param name 服务名
     * @param response Servlet response
     * @throws IOException IO 异常
     */
    @GetMapping(value="/pull/server")
    public void pullServerDirectory(String name, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(name)) {
            throw new JarbootException(ResultCodeConst.EMPTY_PARAM, "导出失败，服务名为空！");
        }
        File dir = FileUtils.getFile(SettingUtils.getServerPath(name));
        if (!dir.exists()) {
            response.sendError(404, "服务不存在！" + name);
            return;
        }
        setDownloadRespHeader(response, name + ".zip");
        //创建缓冲输入流
        try (OutputStream outputStream = response.getOutputStream()){
            ZipUtils.toZip(dir, outputStream, true);
            outputStream.flush();
        }
    }

    /**
     * 上传服务文件
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping("/push/server")
    @ResponseBody
    public ResponseSimple pushServerDirectory(@RequestParam("file") MultipartFile file) {
        //临时目录，用于操作ZIP文件
        final File tempDir = CacheDirHelper.getTempDir(UUID.randomUUID().toString());
        try {
            FileUtils.forceMkdir(tempDir);
            String filename = file.getOriginalFilename();
            final File zipFie = FileUtils.getFile(tempDir, filename);
            //保持本地临时文件
            file.transferTo(zipFie);
            TaskUtils.getTaskExecutor().execute(() -> doPushServer(tempDir, zipFie));
        } catch (Exception e) {
            throw new JarbootRunException(e.getMessage(), e);
        }
        return new ResponseSimple();
    }

    private void doPushServer(File tempDir, File zipFie) {
        //开始正式导入
        try {
            final File out = FileUtils.getFile(tempDir, "out");
            if (!out.exists()) {
                FileUtils.forceMkdir(out);
            }
            WebSocketManager.getInstance().notice("上传成功，开始解压缩...", NoticeEnum.INFO);
            //解压ZIP压缩文件
            ZipUtils.unZip(zipFie, out);
            File[] dirs = out.listFiles();
            //必须保证解压后仅有一个文件夹
            if (null == dirs || 1 != dirs.length || !dirs[0].isDirectory()) {
                WebSocketManager.getInstance().notice("压缩文件中应当仅有一个文件夹！", NoticeEnum.INFO);
                return;
            }
            //解压后的文件夹
            File dir = dirs[0];
            //文件夹的名字
            final String name = dir.getName();
            //将要移动到的工作空间目录
            File dest = FileUtils.getFile(SettingUtils.getWorkspace(), name);
            boolean isExist = dest.exists();
            if (isExist) {
                String sid = SettingUtils.createSid(dest.getPath());
                if (AgentManager.getInstance().isOnline(sid)) {
                    WebSocketManager.getInstance().notice(name + " 正在运行，请先停止再导入！", NoticeEnum.INFO);
                    return;
                }
                WebSocketManager.getInstance().notice(name + " 已存在，将覆盖原服务目录。", NoticeEnum.INFO);
                //先删除
                FileUtils.deleteDirectory(dest);
            }
            //移动到工作空间目录
            FileUtils.copyDirectory(dir, dest);
            //通知前端刷新列表
            if (isExist) {
                WebSocketManager.getInstance().notice(name + " 更新成功！", NoticeEnum.INFO);
            } else {
                WebSocketManager.getInstance().publishGlobalEvent(StringUtils.SPACE,
                        StringUtils.EMPTY, WsEventEnum.WORKSPACE_CHANGE);
                WebSocketManager.getInstance().notice("推送成功，新增服务 " + name, NoticeEnum.INFO);
            }
        } catch (Exception e) {
            WebSocketManager.getInstance().notice("推送失败！" + e.getMessage(), NoticeEnum.ERROR);
        } finally {
            //最终清理临时目录
            try {
                FileUtils.forceDelete(tempDir);
            } catch (Exception e) {
                //ignore
            }
        }
    }

    /**
     * 从服务器下载文件
     * @param file base64编码的文件全路径名
     * @param response Servlet response
     */
    @GetMapping(value="/download/{file}")
    public void download(@PathVariable("file") String file, HttpServletResponse response) throws IOException {
        //待下载文件名
        String fileName = new String(Base64.getDecoder().decode(file));
        File target = FileUtils.getFile(fileName);
        if (!target.exists() || !target.isFile()) {
            response.sendError(404, "文件不存在！" + fileName);
            return;
        }
        setDownloadRespHeader(response, target.getName());
        byte[] buff = new byte[2048];
        //创建缓冲输入流
        try (FileInputStream fis = new FileInputStream(target);
             OutputStream outputStream = response.getOutputStream();
             BufferedInputStream bis = new BufferedInputStream(fis)){
            int len = -1;
            while (-1 != (len = bis.read(buff))) {
                outputStream.write(buff, 0, len);
            }
            outputStream.flush();
        }
    }

    private void setDownloadRespHeader(HttpServletResponse response, String filename) {
        try {
            //支持中文、空格
            filename = new String(filename.getBytes("gb2312"), StandardCharsets.ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            //ignore
        }
        final String contentDisposition = String.format("attachment; filename=\"%s\"", filename);
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", contentDisposition);
    }
}
