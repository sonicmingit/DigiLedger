
---

# ✅ 一、Bing Image Search API Key 申请方法（推荐，国内可直接用）

微软的 Bing 搜索属于 **Microsoft Azure Cognitive Services** 的一部分。
你需要开通 Azure 帐号 → 创建一个资源组 → 创建 “Bing Search v7” 资源 → 获取 Key。

---

## ✔ 步骤 1：注册 Azure（可用国内手机号）

进入微软 Azure 官网：
[https://azure.microsoft.com](https://azure.microsoft.com)

注册即可，支持国内手机号码。

若你用 GitHub 登录也可以。

---

## ✔ 步骤 2：进入 Azure Portal 控制台

[https://portal.azure.com](https://portal.azure.com)

登录后进入 Azure Portal。

---

## ✔ 步骤 3：创建资源组（Resource Group）

* 左侧菜单 → “Resource groups”
* 点击 “Create”
* 随便起个名字：如 `digi-web-ai`
* Region 建议东南亚（Singapore）访问最快

---

## ✔ 步骤 4：创建 Bing Image Search 资源

在顶部搜索框搜索：

```
Bing Search v7
```

进入后：

点击 **Create**

选择：

* Pricing tier：`F0`（免费）每天最多 100 次
* 或 `S1`（付费版）价格不贵

点 Create。

---

## ✔ 步骤 5：获取 API Key

在你创建的 Bing Search 资源里：

* 左侧 **Keys and Endpoint**
* 会看到：

```
KEY 1: xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
KEY 2: xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
Endpoint: https://api.bing.microsoft.com/v7.0
```

把 KEY 写入你的后端配置：

例如：

```yaml
external:
  bing:
    api-key: xxxxx
    endpoint: https://api.bing.microsoft.com/v7.0/images/search
```

**这个 Bing Key 就可以直接用于你的“智能找图设封面”功能。**

---

# 🎯 二、Google Image Search（Custom Search）申请方式（可选扩展）

Google 需要 Custom Search Engine（CSE）+ API Key。
此方案搜索结果最精准（尤其是数码类），但国内后端必须走代理访问。

---

# ✔ 步骤 1：开通 Google Cloud Console

访问：
[https://console.cloud.google.com/](https://console.cloud.google.com/)

使用 Google 帐号登录。

---

# ✔ 步骤 2：创建一个新项目（Project）

点左上角下拉 → New Project
命名：`digi-cover-search`即可

---

# ✔ 步骤 3：启用 Custom Search API

进入：

菜单 → APIs & Services → Library
搜索：

```
Custom Search API
```

点击 Enable。

---

# ✔ 步骤 4：创建 API Key

菜单 → APIs & Services → Credentials
点击 “Create Credentials”
选择 “API Key”

会得到类似：

```
AIzaSyBxxxxxx
```

这是 Google API Key。

---

# ✔ 步骤 5：创建 Image Search 专用的 CSE（Custom Search Engine）

访问：
[https://cse.google.com/cse/all](https://cse.google.com/cse/all)

点击 “Add”

在 “Sites to search” 填入：

```
Search the entire web
```

然后在设置中启用：

* **Image Search: ON**
* Safe Search 可关闭
* API Mode ON

你会得到一个 CSE ID：

```
cx = 1234567890abcdef:abcde12345
```

---

# ✔ Google 图片搜索 API 最终调用格式：

```
https://www.googleapis.com/customsearch/v1?q=iphone15&searchType=image&cx=CSE_ID&key=API_KEY
```

---

# 🎯 三、你项目应该怎么选择？

结合你现在的项目情况（私有服务 + 国内服务器 + 物品封面图智能搜索）：

---

## ✔ 最推荐方案

### **第一优先：Bing Image Search API（国内服务器可直连，不需要代理）**

* 国内可访问（不需要 VPN）
* 质量足够用于产品图封面
* 成本最低：每天 100 次免费额度

👉 **最适合你的“智能找图设封面”功能**

---

## ✔ 第二优先（增强版）

### **解析购买链接 → 直接获取商品主图（淘宝/京东）**

* 如果你存了购买链接，那这个封面图是最精准的
* 不需要调用搜索引擎
* 建议做成优先级最高的解析策略
  （我已帮你写到 codex 提示词方案中了）

---

## ✔ 第三优先（可选扩展）

### **Google Image Search（需要代理，但效果最好）**

* 搜索结果质量高
* 特别适合复杂物品（工具、小设备、生活用品）
* 在你未来需要增强封面匹配时可扩展

---

# 🎯 四、如果你愿意，我可以继续帮助：

我可以帮你：

### ✔ 更新到变更记录（V0.3.10）

### ✔ 输出专门给 Codex 的“配置 Bing/Google Key + 后端封装搜索服务”任务提示词

### ✔ 写 Bing / Google 搜索客户端（Java 版或 Node 版）

### ✔ 补全智能找图弹窗的前端 UI 原型图

你随时说要继续哪一步。
