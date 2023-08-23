package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VersionUtils;
import io.github.majianzheng.jarboot.common.utils.ZipUtils;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cloud
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.CLOUD_CONTEXT)
@Controller
public class CloudController {
    @Value("${docker:false}")
    private boolean isInDocker;

    /**
     * 获取版本
     * @return 版本
     */
    @GetMapping(value="/version")
    @ResponseBody
    public String getVersion() {
        return VersionUtils.version;
    }

    /**
     * 检测是否在docker
     * @return
     */
    @GetMapping(value="/checkInDocker")
    @ResponseBody
    public Boolean checkInDocker() {
        return isInDocker;
    }

    /**
     * 拉取服务目录
     * @param name 服务名
     * @param response Servlet response
     * @throws IOException IO 异常
     */
    @GetMapping(value="/pull/server")
    public void pullServerDirectory(@RequestParam String name, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(name)) {
            throw new JarbootException(ResultCodeConst.EMPTY_PARAM, "导出失败，服务名为空！");
        }
        File dir = FileUtils.getFile(SettingUtils.getServicePath(name));
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
    public ResponseVo<String> pushServerDirectory(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        //临时目录，用于操作ZIP文件
        String filename = file.getOriginalFilename();
        String name = StringUtils.stripEnd(filename, ".zip");
        final File tempDir = CacheDirHelper.getTempDir(name);
        if (tempDir.exists()) {
            //文件正在处理中
            return HttpResponseUtils.error(ResultCodeConst.ALREADY_EXIST, "文件" + filename + "正在处理中...");
        }
        try {
            FileUtils.forceMkdir(tempDir);
            final File zipFie = FileUtils.getFile(tempDir, filename);
            //保持本地临时文件
            file.transferTo(zipFie);
            TaskUtils.getTaskExecutor().execute(() -> doPushServer(tempDir, zipFie));
        } catch (Exception e) {
            throw new JarbootRunException(e.getMessage(), e);
        }
        return HttpResponseUtils.success();
    }

    private void doPushServer(File tempDir, File zipFie) {
        final String id = tempDir.getName();
        //开始正式导入
        try {
            final File out = FileUtils.getFile(tempDir, "out");
            if (!out.exists()) {
                FileUtils.forceMkdir(out);
            }
            MessageUtils.globalLoading(id, "上传成功，开始解压缩...");
            //解压ZIP压缩文件
            ZipUtils.unZip(zipFie, out);
            File[] dirs = out.listFiles();
            //必须保证解压后仅有一个文件夹
            if (null == dirs || 1 != dirs.length || !dirs[0].isDirectory()) {
                MessageUtils.info("压缩文件中应当仅有一个文件夹！");
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
                String sid = SettingUtils.createSid(dest.getAbsolutePath());
                if (AgentManager.getInstance().isOnline(sid)) {
                    MessageUtils.info(name + " 正在运行，请先停止再导入！");
                    return;
                }
                MessageUtils.globalLoading(id, name + " 已存在，正在清除原目录...");
                //先删除
                FileUtils.deleteDirectory(dest);
            }
            //移动到工作空间目录
            MessageUtils.globalLoading(id, name + " 解压完成，开始拷贝...");
            FileUtils.copyDirectory(dir, dest);
            MessageUtils.globalLoading(id, name + " 推送完成！");
            //通知前端刷新列表
            if (isExist) {
                MessageUtils.info(name + " 更新成功！");
            } else {
                MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
                MessageUtils.info("推送成功，新增服务 " + name);
            }
        } catch (Exception e) {
            MessageUtils.error("推送失败！" + e.getMessage());
        } finally {
            //最终清理临时目录
            try {
                FileUtils.forceDelete(tempDir);
            } catch (Exception e) {
                //ignore
            }
            MessageUtils.globalLoading(id, StringUtils.EMPTY);
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
            throw new JarbootException(404, "文件不存在！" + fileName);
        }
        setDownloadRespHeader(response, target.getName());
        //创建缓冲输入流
        try (FileInputStream fis = new FileInputStream(target);
             OutputStream outputStream = response.getOutputStream()){
            IOUtils.copy(fis, outputStream);
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
