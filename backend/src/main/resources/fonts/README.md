# 字体下载说明

## 方式一: 使用阿里云镜像下载 (推荐)

```bash
# 从阿里云镜像下载 Noto Sans SC 字体
curl -L -o src/main/resources/fonts/NotoSansSC-Regular.ttf \
  "https://fonts.aliyun.com/notosanssc/v35/k3kJo84MPvpLmixcA63oeALZTYKL2S24UEg-2_c9IY2u.woff2"
```

## 方式二: 使用 GitHub 下载 (可能较慢)

```bash
# 下载完整的 TTF 字体文件
curl -L -o src/main/resources/fonts/NotoSansSC-Regular.ttf \
  "https://github.com/googlefonts/noto-fonts/raw/main/hinted/ttf/NotoSansSC/NotoSansSC-Regular.ttf"
```

## 方式三: 使用国内 CDN

```bash
# 使用 BootCDN 或其他国内 CDN
curl -L -o src/main/resources/fonts/NotoSansSC-Regular.ttf \
  "https://cdn.bootcdn.net/ajax/libs/font-noto-sans-sc/5.0.2/NotoSansSC-Regular.ttf"
```

## 手动下载

如果上述方式都无法下载,请手动下载字体文件:

1. 访问 [Google Fonts - Noto Sans SC](https://fonts.google.com/notosanssc)
2. 点击 "Download family" 下载字体包
3. 解压后找到 `NotoSansSC-Regular.ttf`
4. 将文件复制到 `src/main/resources/fonts/` 目录

## 验证下载

下载完成后,运行以下命令验证:

```bash
# 查看字体文件大小 (应该大于 1MB)
ls -lh src/main/resources/fonts/NotoSansSC-Regular.ttf

# 检查文件格式 (应该是 TrueType 或 WOFF2)
file src/main/resources/fonts/NotoSansSC-Regular.ttf
```
