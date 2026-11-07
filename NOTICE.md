# 第三方组件与致谢 / Third-Party Notices

本文件说明 AIGC's Equal Earth Generator（下称"本模组"）的设计来源、第三方组件及其许可证。
本模组本身以 **MIT License** 发布，完整文本见 [`LICENSE`](LICENSE)。

## 0. AIGC 生成声明

本项目的源代码、数据包、语言文件与文档使用 **DeepSeek AI** 辅助生成，并经维护者人工整理、
编译与测试。AI 生成内容可能存在错误、遗漏或与特定模组版本不兼容的情况；本项目按 MIT
License 的 “AS IS” 条款提供，不附带任何担保，使用前请备份存档并在测试环境中验证。
完整的免责声明与注意事项见仓库 README 的「AIGC 声明与免责」章节。

## 1. 设计灵感：TFC Eratosthenes

本模组在**设计思路**上受到 **TFC Eratosthenes** 的启发，包括但不限于：有限球形世界、
180° 经线环绕、极地区域处理、以地图投影约束大陆生成、Distant Horizons 曲率建议等。

| 项目 | 信息 |
| --- | --- |
| 名称 | TFC Eratosthenes（模组 ID：`tfc_eratosthenes`） |
| 作者 | Trygve55 |
| 许可证 | **GNU General Public License v3.0 or later（GPL-3.0-or-later）** |
| 源码 | <https://github.com/trygve55/tfc_eratosthenes> |
| Modrinth | <https://modrinth.com/mod/tfc-eratosthenes> |
| CurseForge | <https://www.curseforge.com/minecraft/mc-mods/tfc-eratosthenes> |

**代码独立性声明**：本模组是按上述公开设计思路独立实现的，**不包含、不复制、不链接**
TFC Eratosthenes 的源代码或素材。因此本模组可以按 MIT License 发布。TFC Eratosthenes
的代码、名称与素材版权归 Trygve55 所有，并继续遵循 GPL-3.0-or-later；任何对
TFC Eratosthenes 本身的修改或再分发都必须遵守其 GPL 条款。

## 2. Equal Earth 投影

- 算法来源：Šavrič, B., Patterson, M. P., & Jenny, B. (2018).
  *The Equal Earth map projection*. International Journal of Geographical Information Science.
- 参考实现：[d3-geo `equalEarth`](https://github.com/d3/d3-geo/blob/main/src/projection/equalEarth.js)
  （ISC License）。
- 本模组仅使用论文公开的公式与常数，并自行实现查找表与牛顿反解，未复制 d3-geo 的源代码。

## 3. NeoForge MDK

构建模板（`build.gradle`、`gradle.properties`、`src/main/templates` 等派生文件）来自
[github.com/NeoForged/MDK](https://github.com/NeoForged/MDK)，以 MIT License 提供，
完整文本见 [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt)。

## 4. TerraFirmaCraft（可选兼容目标）

TerraFirmaCraft（TFC）仅作为可选的编译期兼容目标（`compileOnly`），**不随本模组分发**。
TFC 的代码与素材版权归其作者所有，使用或分发 TFC 时请遵守其自身许可证。
