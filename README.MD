# Spring Boot 文件管理器

一个基于 Spring Boot 的 Web 文件管理器，提供文件浏览、上传、下载、预览、编辑等功能。

Android App - [fileManager-android](https://github.com/ming123aaa/fileManager-android)


## 功能特性

### 文件浏览
- 支持网格视图和列表视图
- 文件/文件夹图标自动识别
- 面包屑导航，支持快速跳转
- 文件搜索过滤
- 按名称、大小、修改时间排序

### 文件操作（需要登录管理页面）
- 上传文件（支持拖拽上传、多文件上传）
- 新建文件夹
- 重命名文件/文件夹
- 删除文件/文件夹（支持递归删除）
- 移动文件/文件夹
- 在线编辑文本文件

### 文件预览
- 图片预览（jpg, png, gif, webp 等）
- 视频播放（mp4, avi, mkv 等）
- 音频播放（mp3, wav, flac 等）
- 文本/代码预览
- PDF 预览

### 其他功能
- 复制下载链接
- 在新标签页打开
- 移动端响应式适配
- 路径参数支持，方便分享

## 页面说明

| 页面 | 地址 | 功能 |
|------|------|------|
| 文件浏览（只读） | `http://localhost:8080/index.html` | 只能浏览、预览、下载文件 |
| 文件管理（可编辑） | `http://localhost:8080/file.html` | 可上传、删除、重命名、移动文件 |

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 构建项目

**方式一：使用 Maven 命令行**
```bash
mvn clean package -DskipTests
```

**方式二：使用 IDEA**
1. 打开 IDEA，找到 Maven 插件
2. 打开 Maven 后找到 `Lifecycle` -> `package` 运行
3. 运行结束后在 `target` 文件夹下可以看到 `FileManager-1.0.0.jar`

### 运行项目

```bash
# 默认配置运行
java -Dfile.encoding=utf-8 -jar target/FileManager-1.0.0.jar

# 方式一：使用 --path 参数指定存储路径
java -Dfile.encoding=utf-8 -jar target/FileManager-1.0.0.jar --path=/your/files/path

# 方式二：使用 --storage 参数指定存储路径
java -Dfile.encoding=utf-8 -jar target/FileManager-1.0.0.jar --storage=D:\my-files

# 方式三：直接传入路径（兼容旧版）
java -Dfile.encoding=utf-8 -jar target/FileManager-1.0.0.jar /path/to/files

# 指定端口和存储路径
java -Dfile.encoding=utf-8 -jar target/FileManager-1.0.0.jar --server.port=9090 --path=/your/files/path
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| `--path` 或 `--storage` | 文件存储目录 | `--path=D:\files` |
| `--server.port` | 服务端口 | `--server.port=9090` |

**默认路径：**
- Windows: `./file`
- Linux/Mac: `./file`

## API 接口

### 文件操作

| 接口 | 方法 | 说明 |
|------|------|------|
| `/main/getAllFile` | GET | 获取文件列表 |
| `/main/fileUpload` | POST | 上传单个文件 |
| `/main/multifileUpload` | POST | 上传多个文件 |
| `/main/download/{name}` | GET | 下载文件 |
| `/main/files/**` | GET | 通配符方式下载文件 |
| `/main/readText` | GET | 读取文本文件 |
| `/main/writeText` | POST | 写入文本文件 |
| `/main/mkdir` | POST | 创建文件夹 |
| `/main/delete` | POST | 删除文件/文件夹 |
| `/main/rename` | POST | 重命名文件/文件夹 |
| `/main/move` | POST | 移动文件/文件夹 |
| `/main/fileInfo` | GET | 获取文件详情 |

### 接口示例

**获取文件列表**
```
GET /main/getAllFile?path=subfolder
```

**上传文件**
```
POST /main/fileUpload
Content-Type: multipart/form-data

fileName: (文件)
path: subfolder (可选)
```

**创建文件夹**
```
POST /main/mkdir
Content-Type: application/x-www-form-urlencoded

path=parent_folder&name=new_folder
```

## 项目结构

```
file-manager_springboot/
├── src/main/
│   ├── java/com/example/filemanager/
│   │   ├── FileManagerApplication.java    # 启动类
│   │   ├── mainApi.java                   # 文件操作 API
│   │   ├── ResPath.java                   # 路径配置
│   │   ├── Util.java                      # 工具类
│   │   └── Bean/
│   │       └── FileBean.java              # 文件信息实体
│   └── resources/
│       ├── application.properties         # 配置文件
│       └── static/                        # 前端静态资源
│           ├── index.html                 # 只读浏览页面
│           ├── file.html                  # 可编辑管理页面
│           ├── css/
│           │   └── style.css              # 样式文件
│           └── js/
│               └── app.js                 # JavaScript 逻辑
└── README.MD
```

## 配置说明

配置文件位于 `src/main/resources/application.properties`

```properties
# 服务端口
server.port=8080

# 文件编码
server.tomcat.uri-encoding=utf-8

# 最大上传文件大小 (1024000MB)
spring.servlet.multipart.max-file-size=1024000MB
spring.servlet.multipart.max-request-size=1024000MB

# Session 超时时间
server.servlet.session.timeout=30m
```

## 安全特性

- 路径穿越防护：防止通过 `../` 访问系统文件
- 文件名安全处理：防止恶意文件名注入
- 递归删除保护：删除文件夹时安全处理子目录

## 浏览器兼容

- Chrome / Edge 90+
- Firefox 88+
- Safari 14+
- 移动端浏览器

## 常见问题

### Q: 端口被占用怎么办？
A: 修改 `application.properties` 中的 `server.port`，或启动时指定：
```bash
java -jar FileManager-1.0.0.jar --server.port=9090
```

### Q: 如何修改默认存储路径？
A: 启动时在命令行参数中指定路径：
```bash
java -jar FileManager-1.0.0.jar /your/custom/path
```

### Q: 上传大文件失败？
A: 检查 `application.properties` 中的上传大小限制配置。

## License

MIT