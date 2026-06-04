package com.example.filemanager;

import com.example.filemanager.Bean.FileBean;
import com.google.gson.Gson;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("main")
public class mainApi {

    /**
     * 安全路径解析：将用户输入的 path 解析为安全的绝对路径，防止路径穿越
     */
    private File safePath(String path) {
        if (path == null) path = "";
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            // ignore
        }
        // 移除开头的 / 或 \
        if (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1);
        }
        File dir = new File(ResPath.DownloadPath);
        File target = new File(dir, path);
        // 防止路径穿越：确保解析后的路径仍在根目录内
        try {
            if (!target.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
                return dir;
            }
        } catch (IOException e) {
            return dir;
        }
        return target;
    }

    /**
     * 单文件上传（支持指定目录路径）
     */
    @RequestMapping(value = "fileUpload", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String fileUpload(@RequestParam("fileName") MultipartFile file,
                             @RequestParam(value = "path", required = false, defaultValue = "") String path) {
        if (file.isEmpty()) {
            return "文件为空";
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return "文件名无效";
        }
        // 防止文件名中的路径穿越
        fileName = new File(fileName).getName();
        File dir = safePath(path);
        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest);
            return "上传成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "上传失败: " + e.getMessage();
        }
    }

    /**
     * 多文件上传
     */
    @RequestMapping(value = "multifileUpload", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String multifileUpload(HttpServletRequest request,
                                  @RequestParam(value = "path", required = false, defaultValue = "") String path) {
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("fileName");
        if (files == null || files.isEmpty()) {
            return "没有选择文件";
        }
        File dir = safePath(path);
        int successCount = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String fileName = file.getOriginalFilename();
            if (fileName == null) continue;
            fileName = new File(fileName).getName();
            try {
                file.transferTo(new File(dir, fileName));
                successCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "成功上传 " + successCount + " 个文件";
    }

    /**
     * 文件下载（路径参数）
     */
    @RequestMapping(value = "/download/{name}", method = {RequestMethod.GET, RequestMethod.POST})
    public void getDownload(@PathVariable String name,
                            HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException {
        Util.download(request, response, name);
    }

    /**
     * 文件下载（查询参数）
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET, RequestMethod.POST})
    public void Download(@RequestParam("name") String name,
                         HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException {
        if (name != null && !name.isEmpty()) {
            if (name.charAt(0) == '/' || name.charAt(0) == '\\') {
                name = name.substring(1);
            }
            Util.download(request, response, name);
        }
    }

    /**
     * 文件下载（通配符路径）
     */
    @GetMapping("/files/**/{filename:.+}")
    public void downloadFile(@PathVariable String filename, HttpServletRequest request, HttpServletResponse response) {
        String pathWithinApplication = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String realPath = pathWithinApplication.replaceFirst("/main/files", "");
        if (!realPath.isEmpty()) {
            Util.download(request, response, realPath);
        }
    }

    /**
     * 获取文件列表（JSON），支持 lastModified
     */
    @RequestMapping(value = "/getAllFile", method = {RequestMethod.GET, RequestMethod.POST})
    public String getAllFile(@RequestParam(value = "path", required = false) String path) {
        List<FileBean> files = new ArrayList<>();
        File file = safePath(path);

        File[] tempList = file.listFiles();
        if (tempList == null) {
            return new Gson().toJson(files);
        }

        for (File f : tempList) {
            String name = f.getName();
            files.add(new FileBean(name, f.length(), f.isDirectory(), f.lastModified()));
        }
        return new Gson().toJson(files);
    }

    /**
     * 创建文件夹
     */
    @RequestMapping(value = "/mkdir", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String mkdir(@RequestParam(value = "path", required = false, defaultValue = "") String path,
                        @RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            return "文件夹名不能为空";
        }
        name = new File(name).getName(); // 安全处理
        File dir = safePath(path);
        File newDir = new File(dir, name);
        if (newDir.exists()) {
            return "文件夹已存在";
        }
        if (newDir.mkdirs()) {
            return "创建成功";
        }
        return "创建失败";
    }

    /**
     * 删除文件或文件夹（递归删除）
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String delete(@RequestParam("path") String path) {
        File file = safePath(path);
        if (!file.exists()) {
            return "文件不存在";
        }
        if (deleteRecursive(file)) {
            return "删除成功";
        }
        return "删除失败";
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    /**
     * 重命名文件或文件夹
     */
    @RequestMapping(value = "/rename", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String rename(@RequestParam("path") String path,
                         @RequestParam("newName") String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return "新名称不能为空";
        }
        newName = new File(newName).getName(); // 安全处理
        File file = safePath(path);
        if (!file.exists()) {
            return "文件不存在";
        }
        File newFile = new File(file.getParent(), newName);
        if (newFile.exists()) {
            return "同名文件已存在";
        }
        if (file.renameTo(newFile)) {
            return "重命名成功";
        }
        return "重命名失败";
    }

    /**
     * 移动文件或文件夹
     */
    @RequestMapping(value = "/move", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String move(@RequestParam("path") String path,
                       @RequestParam("targetDir") String targetDir) {
        File file = safePath(path);
        if (!file.exists()) {
            return "文件不存在";
        }
        File dest = safePath(targetDir);
        if (!dest.exists() || !dest.isDirectory()) {
            return "目标目录不存在";
        }
        File newFile = new File(dest, file.getName());
        if (newFile.exists()) {
            return "目标位置已存在同名文件";
        }
        if (file.renameTo(newFile)) {
            return "移动成功";
        }
        return "移动失败";
    }

    /**
     * 写入文本（指定路径）
     */
    @RequestMapping(value = "/writeText", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String WriteText(@RequestParam("txt") String txt,
                            @RequestParam(value = "path", required = false, defaultValue = "test.txt") String path) {
        File file = safePath(path);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(txt.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            return "写入成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "写入失败: " + e.getMessage();
        }
    }

    /**
     * 读取文本（指定路径）
     */
    @RequestMapping(value = "/readText", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String readText(@RequestParam(value = "path", required = false, defaultValue = "test.txt") String path) {
        File file = safePath(path);
        if (!file.isFile() || !file.exists()) {
            return "文件不存在";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "读取失败: " + e.getMessage();
        }
    }

    /**
     * 获取文件信息（单个文件详情）
     */
    @RequestMapping(value = "/fileInfo", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String fileInfo(@RequestParam("path") String path) {
        File file = safePath(path);
        if (!file.exists()) {
            return "{}";
        }
        FileBean bean = new FileBean(file.getName(), file.length(), file.isDirectory(), file.lastModified());
        return new Gson().toJson(bean);
    }
}
