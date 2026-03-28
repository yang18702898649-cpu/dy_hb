# GitHub Actions 自动签名配置

本配置让 GitHub Actions 自动签名 APK，无需手动操作。

---

## 🔐 配置步骤

### 1. 生成本地签名密钥

在本地电脑执行：

```bash
# 生成密钥库（只需做一次，妥善保管）
keytool -genkey -v \
  -keystore my-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias my-alias

# 按提示输入：
# - 密钥库密码（记住这个密码）
# - 密钥密码（可以和上面相同）
# - 姓名、组织等信息（随便填）
```

### 2. 转换密钥为 Base64

```bash
# macOS / Linux
base64 -i my-release-key.jks -o keystore-base64.txt

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Out-File -Encoding ASCII keystore-base64.txt
```

### 3. 添加到 GitHub Secrets

1. 打开 GitHub 仓库页面
2. 点击 **Settings** → **Secrets and variables** → **Actions**
3. 点击 **New repository secret**
4. 添加以下 Secrets：

| Secret 名称 | 值 | 说明 |
|------------|-----|------|
| `KEYSTORE_BASE64` | 复制 `keystore-base64.txt` 的内容 | 密钥库文件的 Base64 编码 |
| `KEYSTORE_PASSWORD` | 你的密钥库密码 | 创建密钥时设置的密码 |
| `KEY_ALIAS` | `my-alias` | 密钥别名（创建时指定的） |
| `KEY_PASSWORD` | 你的密钥密码 | 密钥密码（通常和密钥库密码相同） |

---

## ✅ 验证配置

配置完成后，推送代码到 main 分支：

```bash
git add .
git commit -m "配置自动签名"
git push origin main
```

然后查看 GitHub Actions：
1. 进入仓库 → **Actions** 标签
2. 点击最新的 workflow 运行
3. 等待构建完成
4. 下载 Release APK 测试安装

---

## 🔒 安全说明

- **密钥文件** (`my-release-key.jks`) 和 **密码** 永远不要提交到 Git 仓库
- GitHub Secrets 是加密存储的，只有 GitHub Actions 可以访问
- 每次构建后，密钥文件会自动删除
- 建议备份 `my-release-key.jks` 文件到安全的地方

---

## 📱 安装签名后的 APK

签名后的 APK 可以直接安装：

1. 从 GitHub Actions 或 Releases 下载 `app-release.apk`
2. 在 Android 手机上打开 APK 文件
3. 允许安装未知来源应用
4. 正常安装使用

---

## 🆘 常见问题

### Q: 没有配置 Secrets 会怎样？
A: GitHub Actions 会构建未签名的 APK，仍然可以下载 Debug 版本使用。

### Q: 忘记密码怎么办？
A: 无法找回，需要重新生成密钥。已发布的应用无法更新，必须重新发布。

### Q: 密钥泄露了怎么办？
A: 立即在 GitHub 中删除 Secrets，重新生成密钥，重新配置。

---

## 📚 参考

- [Android 签名文档](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
