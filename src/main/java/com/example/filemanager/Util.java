package com.example.filemanager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;

public class Util {
    /**
     * 文件下载，断点续传
     * 参考地址： https://leaveslm.github.io/2018/07/31/2018-2018-07-31-%E6%96%AD%E7%82%B9%E7%BB%AD%E4%BC%A0%E4%B8%8B%E8%BD%BD%E6%96%87%E4%BB%B6-%E5%A4%9A%E5%AA%92%E4%BD%93%E5%9C%A8%E7%BA%BF%E6%92%AD%E6%94%BE/
     *
     * @param request
     * @param response
     * @param fileName
     * @return
     */
    public static boolean download(HttpServletRequest request, HttpServletResponse response, String fileName) {
        //文件目录
        Calendar calendar = Calendar.getInstance();
        File serverDir = new File(ResPath.DownloadPath);
        File file = new File(serverDir + File.separator + fileName);

        //下载开始位置
        long startByte = 0;
        //下载结束位置
        long endByte = file.length() - 1;

        //获取下载范围
        String range = request.getHeader("range");
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=") + 1).trim();
            String rangeArray[] = range.split("-");
            if (rangeArray.length == 1) {
                //Example: bytes=1024-
                if (range.endsWith("-")) {
                    startByte = Long.parseLong(rangeArray[0]);
                } else { //Example: bytes=-1024
                    endByte = Long.parseLong(rangeArray[0]);
                }
            }
            //Example: bytes=2048-4096
            else if (rangeArray.length == 2) {
                startByte = Long.parseLong(rangeArray[0]);
                endByte = Long.parseLong(rangeArray[1]);
            }
        }

        long contentLength = endByte - startByte + 1;
        String contentType = request.getServletContext().getMimeType(fileName);

        //HTTP 响应头设置
        //断点续传，HTTP 状态码必须为 206，否则不设置，如果非断点续传设置 206 状态码，则浏览器无法下载
        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setContentType(contentType);
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Accept-Ranges", "bytes");
        //Content-Range: 下载开始位置-下载结束位置/文件大小
        response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());
        //Content-disposition: inline; filename=xxx.xxx 表示浏览器内嵌显示该文件
        //Content-disposition: attachment; filename=xxx.xxx 表示浏览器下载该文件
        response.setHeader("Content-Disposition", "inline;filename="+fileName.replaceAll("/","") );

        //传输文件流
        BufferedOutputStream outputStream = null;
        RandomAccessFile randomAccessFile = null;
        //已传送数据大小
        long transmittedLength = 0;
        try {
            //以只读模式设置文件指针偏移量
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(startByte);

            outputStream = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[4096];
            int len;
            while (transmittedLength < contentLength && (len = randomAccessFile.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
                transmittedLength += len;
            }

            outputStream.flush();
            response.flushBuffer();

            return true;

        } catch (IOException e) {

        } finally {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
