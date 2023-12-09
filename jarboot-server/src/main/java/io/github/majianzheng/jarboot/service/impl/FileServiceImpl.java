package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.pojo.FileNode;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.dao.FileUploadProgressDao;
import io.github.majianzheng.jarboot.service.FileService;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author mazheng
 */
@Service
public class FileServiceImpl implements FileService {
    @Resource
    private FileUploadProgressDao fileUploadProgressDao;

    @Override
    public List<FileNode> getWorkspaceFiles(String baseDir, boolean withRoot) {
        check(baseDir);
        File base = FileUtils.getFile(SettingUtils.getWorkspace(), baseDir);
        FileNode fileNode = getFileNode(base);
        fileNode.setParent(null);
        if (withRoot) {
            return Collections.singletonList(fileNode);
        }
        return fileNode.getChildren();
    }

    @Override
    public String getContent(String file) {
        check(file);
        try {
            return FileUtils.readFileToString(FileUtils.getFile(SettingUtils.getWorkspace(), file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String file) {
        check(file);
        try {
            FileUtils.forceDelete(FileUtils.getFile(SettingUtils.getWorkspace(), file));
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    @Override
    public void uploadFile(String file, InputStream is) {
        check(file);
        try (OutputStream os = FileUtils.openOutputStream(FileUtils.getFile(SettingUtils.getWorkspace(), file))) {
            IOUtils.copy(is, os);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    @Override
    public void download(String file, OutputStream os) {
        check(file);
        try (InputStream is = FileUtils.openInputStream(FileUtils.getFile(SettingUtils.getWorkspace(), file))) {
            IOUtils.copy(is, os);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    @Override
    public String writeFile(String file, String content) {
        check(file);
        File file1 = FileUtils.getFile(SettingUtils.getWorkspace(), file);
        try {
            FileUtils.writeStringToFile(file1, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        return genNodeKey(file1);
    }

    @Override
    public String newFile(String file, String content) {
        File file1 = FileUtils.getFile(SettingUtils.getWorkspace(), file);
        if (file1.exists()) {
            throw new JarbootException(file + "已存在！");
        }
        this.writeFile(file, content);
        return genNodeKey(file1);
    }

    @Override
    public String addDirectory(String file) {
        check(file);
        File file1 = FileUtils.getFile(SettingUtils.getWorkspace(), file);
        if (file1.exists()) {
            throw new JarbootException(file + "已存在！");
        }
        try {
            FileUtils.forceMkdir(file1);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        return genNodeKey(file1);
    }

    private static FileNode getFileNode(File file) {
        FileNode fileNode = convertFileToNode(file);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null == files || files.length == 0) {
                fileNode.setLeaf(true);
                fileNode.setChildren(new ArrayList<>());
                return fileNode;
            }
            fileNode.setLeaf(false);
            fileNode.setChildren(new ArrayList<>(files.length));
            for (File child : files) {
                if (child.getName().startsWith(".")) {
                    continue;
                }
                fileNode.getChildren().add(convertFileToNode(child));
            }
        }
        return fileNode;
    }

    private static FileNode convertFileToNode(File file) {
        FileNode fileNode = new FileNode();
        fileNode.setName(file.getName());
        fileNode.setKey(genNodeKey(file));
        fileNode.setParent(file.getParentFile().getName());
        fileNode.setModifyTime(file.lastModified());
        if (file.isDirectory()) {
            fileNode.setDirectory(true);
            String[] files = file.list();
            fileNode.setLeaf(null == files || files.length == 0);
        } else {
            fileNode.setDirectory(false);
            fileNode.setSize(file.length());
            fileNode.setLeaf(true);
        }
        return fileNode;
    }

    private static String genNodeKey(File file) {
        return String.valueOf(file.getAbsolutePath().hashCode());
    }

    private void check(String path) {
        if (null == path) {
            throw new JarbootException("目录为null");
        }
        final String rel = "..";
        if (path.contains(rel)) {
            throw new JarbootException("禁用相对目录");
        }
    }

    @PostConstruct
    public void init() {
        fileUploadProgressDao.deleteFinished();
    }
}
