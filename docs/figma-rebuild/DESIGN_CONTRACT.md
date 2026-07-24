# DigiLedger Figma 重构设计契约

## 1. 设计源

- Figma 文件：`https://www.figma.com/design/0l4BbT07ZRmYxgNjL89pkz`
- H5 原型根节点：`12:2`
- PC 原型根节点：`16:2`
- 共享组件页：`4:3`
- 设计实现必须以 Figma 节点属性和截图为准，旧前端只作为业务流程参考。

## 2. 目录边界

- 新 PC：`frontend-figma/`
- 新 H5 / Android：`uniapp-figma/`
- 后端扩展：`backend/`
- 新部署资料：`deploy/v2/`（后端 + `frontend-figma`）
- 禁止修改旧前端：`frontend/`、`uniapp/digiLeder/`

## 3. 设计变量

| Token | 值 |
| --- | --- |
| `--dl-text` | `#171915` |
| `--dl-text-secondary` | `#4e534b` |
| `--dl-muted` | `#7a8075` |
| `--dl-black` | `#0f100e` |
| `--dl-lime` | `#b7ff3c` |
| `--dl-white` / `--dl-card` | `#ffffff` |
| `--dl-accent-soft` | `#e8ffc0` |
| `--dl-bg-alt` | `#eef0eb` |
| `--dl-bg` | `#f5f6f3` |
| `--dl-success` | `#29c477` |
| `--dl-warning` | `#f5a623` |
| 小/中/大/胶囊圆角 | `12px / 20px / 28px / 999px` |

- 字体：`Noto Sans SC`，系统字体作为回退。
- 图标：使用 Figma 返回的真实 SVG/PNG 资源或仓库内同源图标路径，不用 Emoji、ASCII 或文本符号代替。
- 所有可点击按钮必须具备 hover/active/disabled/loading 状态；表单必须具备校验与错误提示。

## 4. PC 页面

目标画板 1440 × 900，固定 224px 侧栏，内容区域允许在 1280px 以上自适应。

| 页面 | Figma 节点 | 路由 |
| --- | --- | --- |
| 总览 | `16:5` | `/` |
| 物品中心 | `16:79` | `/assets` |
| 物品详情 | `17:2` | `/assets/:id` |
| 心愿单 | `17:55` | `/wishlist` |
| 升级路线 | `18:2` | `/upgrade-routes` |
| 系统设置 | `18:58` | `/settings` |

既有新增/编辑、购买记录、出售、上传、心愿转物品等流程可使用同风格抽屉或弹窗承载，但不得破坏主画板结构。

## 5. H5 / Android 页面

目标画板 393 × 852，支持安全区，主操作触控高度不小于 48px，底部导航固定为物品、心愿、统计、设置。

| 页面 | Figma 节点 | 建议页面路径 |
| --- | --- | --- |
| 物品首页 | `12:5` | `pages/assets/home` |
| 搜索筛选 | `12:51` | `pages/assets/search` |
| 新增编辑 | `13:2` | `pages/assets/editor` |
| 物品详情 | `13:34` | `pages/assets/detail` |
| 心愿单 | `14:2` | `pages/wishlist/index` |
| 数据统计 | `14:46` | `pages/statistics/index` |
| 设置与分类 | `14:88` | `pages/settings/index` |

必须同时支持 H5 和 `app-android` 编译，避免只在浏览器 DOM 中可用的 API。

## 6. 视觉验收

1. 使用与 Figma 相同的画板尺寸截取实现截图。
2. 检查布局、间距、字号、字重、颜色、圆角、阴影、图标、状态与滚动区域。
3. 视觉差异修正后再次截图，不能只以构建通过作为完成依据。
4. PC 与 H5 的空态、加载态、错误态必须使用同一设计语言。
