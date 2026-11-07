# AIGC`s Equal Earth Generator

> 仓库：<https://github.com/trensjur/AIGC-Equal-Earth-Generator>
>
> 当前版本：**0.9.6** —— Phase 0–6 全部实现，新增本初子午线/地理信息查询与天空/环境配置。

> [!WARNING]
> **AIGC 声明**：本模组的代码、资源与文档使用 **DeepSeek AI** 辅助生成，并经人工整理、编译与测试。
> AI 生成内容可能存在错误或遗漏；使用前请阅读下方的「AIGC 声明与免责」，并备份存档。

NeoForge 1.21.1 的**有限球形世界**模组：把世界实现为"一张有限的 Equal Earth 地图 + 可环绕的接缝 + 投影形状的大陆掩码"。

设计文档见 [`fromgithub/docs/球形世界模组设计方案.md`](fromgithub/docs/球形世界模组设计方案.md)。

## 它做了什么

| 能力 | 实现方式 |
| --- | --- |
| 大陆分布符合 Equal Earth 投影 | `EqualEarthProjection`（`z → θ → 纬度` 查找表，零三角函数热路径） |
| 地图外是海，不是虚空 | 密度函数包装（`MaskedDensity`）+ 完全外部区块的"平坦海洋"捷径 |
| 东西方向可环绕 | 越过接缝 `x = ±halfWidth(z)` 后按周期 `2 * seamX` 环绕，保留动量与载具乘客 |
| 南北有极线 | 极线缓冲带强制海洋，越界推回 |
| 不破坏原版/数据包世界生成 | 只做**掩码**，不重映射噪声采样坐标；Terralith、TerraBlender 与自定义密度函数自动兼容 |
| 结构/特征不跨接缝 | `createStructures` / `applyBiomeDecoration` 在缓冲带内取消 |
| 形状跟存档走 | `SavedData`（`e2generator_world_shape.dat`），配置只影响新世界 |
| 客户端可选 | 登录时同步形状，客户端只用于 HUD；未装模组也能进服 |
| TFC 兼容（可选） | 独立 `e2generator.tfc.mixins.json` + `WorldgenAdapter`；未安装 TFC 时自动跳过 |
| 数据包形状预设 | `data/<namespace>/e2generator/shape/*.json`，可用 `/sphere preset` 查看 |
| 其它模组可接入 | 公开事件（创建/挂载/接缝/极线）+ `WorldgenAdapter` SPI |

## 配置（`config/e2generator-server.toml`）

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用球形世界 |
| `dimensions` | `["minecraft:overworld"]` | 生效维度 |
| `shapePreset` | `""` | 新建世界使用的预设 id；留空使用下面的单独配置 |
| `boundaryMode` | `OUTLINE` | `RECTANGLE`（直边）/ `OUTLINE`（Equal Earth 轮廓） |
| `halfMeridian` | `8000` | 赤道到极线的方块距离 H |
| `widthToHeightRatio` | `2.0` | 赤道半宽 / H；2 = 完整世界，1 = 半世界 |
| `equatorZ` | `0` | 赤道所在 z |
| `seamBuffer` | `384` | 接缝海洋带宽度 |
| `polarBuffer` | `192` | 极线海洋带宽度 |
| `softEdgeWidth` | `96` | 海陆软过渡宽度 |
| `structureMargin` | `256` | 结构与边界的安全余量 |
| `meridianGrace` | `32` | 越过接缝多少格后触发包裹 |
| `oceanFloorY` | `24` | 地图外海床高度 |
| `fallbackBiome` | `minecraft:deep_ocean` | 地图外兜底群系 |
| `wrapEntities` | `true` | 实体（玩家/载具/投掷物/掉落物）接缝环绕 |
| `announceCrossing` | `false` | 穿越接缝时显示提示 |
| `polarPushback` | `true` | 极线推回 |
| `tfcCompatibility` | `true` | 安装 TFC 时套用形状掩码 |
| `tfcGridBlockSize` | `128` | TFC region 网格一格对应的方块数（1.21.1 为 128） |
| `densityMask` | `true` | 是否包装原版密度函数（c2me 冲突时的退化开关） |
| `chunkMaskCache` | `true` | 每区块预计算 16×16 掩码（性能优化） |
| `distantHorizonsCompat` | `true` | 挂载形状时输出 DH 曲率推荐值 |
| `dhReferenceHalfMeridian` | `20000` | DH 推荐值的参考半子午线 |
| `dhReferenceCurveRatio` | `160` | 参考半子午线对应的 DH 曲率比例 |

默认地图尺寸：赤道周长 `2 * H * ratio = 32000` 格，南北跨度 `2 * H = 16000` 格。

## 世界生成控制器（创建世界 → 更多选项 → 世界生成控制器）

创建世界界面的 **更多（More）** 标签页底部新增了入口。控制器提供：

| 控件 | 说明 |
| --- | --- |
| 种子 | 可输入任意种子；**留空时每次刷新（手动点“刷新”或参数变化）都会自动填入随机种子** |
| 星球半径 | 直接输入数字，以**区块**为单位（64 ~ 64000）。它决定一个立体球体的大小，球面再通过 Equal Earth 投影铺到平面上 |
| 允许极地生成大陆 | 关闭（默认）时极线附近保留海洋缓冲带；开启后大陆可以一直延伸到极线 |
| 允许大陆跨边界生成 | 关闭（默认）时接缝附近保留海洋缓冲带；开启后大陆可以贴到接缝 |

所有控件（种子、半径输入框、两个开关、刷新/应用/取消按钮、标尺区域）都支持**鼠标悬停显示详细提示**；半径输入框会把超出范围的值夹取到 64 ~ 64000，并在标签右侧以红色提示。

“星球大小标尺”会实时显示换算结果：左侧是 Equal Earth 叶形轮廓 + 区块标尺，右侧是星球半径、世界尺寸（格 / 区块）、赤道周长，并注明**地图面积 = 球面面积**（等面积投影）。

换算方式：

$$
4\pi R^2 \;=\; A_{\text{unit}} \cdot H^2
\;\Longrightarrow\;
H = R\sqrt{\frac{4\pi}{A_{\text{unit}}}}
$$

其中 $R$ 是星球半径（方块），$A_{\text{unit}}$ 是单位投影（$H=1$）的叶形面积，由 LUT 数值积分得到；世界宽 $=2H\cdot ratio$，高 $=2H$。

点“应用”后：

- 种子写入创建世界界面（世界页的种子框会同步显示）；
- 半径与两个开关被记录下来，**下一个新建的世界**会使用它们（`WorldShapeData` 在首次创建时取用一次后清空）；
- 已有存档不受影响。

## 形状预设（数据包 / 内置，Phase 6）

新建世界可以使用**形状预设**，而不必逐个调整配置项。预设来源：

- **内置**：`e2generator:earth`、`e2generator:half_world`、`e2generator:compact`、`e2generator:large`、`e2generator:rectangle`；
- **数据包**：`data/<namespace>/e2generator/shape/<path>.json`，可覆盖同名内置预设。

在 `e2generator-server.toml` 中设置 `shapePreset = "yourpack:my_shape"`，下一个新建的世界会使用它；已有存档不受影响。命令 `/sphere preset list` 与 `/sphere preset info <id>` 可查看当前可用预设。

示例（仓库内另附 `example_custom.json`）：

```json
{
  "description": "800 区块半径的整合包世界",
  "planetRadiusChunks": 800,
  "widthToHeightRatio": 2.0,
  "boundaryMode": "OUTLINE",
  "equatorZ": 0,
  "seamBuffer": 512,
  "polarBuffer": 256,
  "softEdgeWidth": 128,
  "structureMargin": 320,
  "meridianGrace": 48,
  "oceanFloorY": 16,
  "fallbackBiome": "minecraft:deep_ocean"
}
```

- `planetRadiusChunks` 与 `halfMeridian` 二选一（优先前者）；前者按等面积关系自动换算 `H`。
- 非法/未知字段会记录日志并跳过该文件；`/reload` 会重新扫描数据包。
- 预设只影响**新建世界**；已有存档始终以 `SavedData` 中的形状为准。

## TFC 兼容（Phase 4，可选）

TFC 使用自己的 `RegionGenerator`，不走 `NoiseBasedChunkGenerator`。本模组提供独立的
`e2generator.tfc.mixins.json`，由 `TfcMixinPlugin` 在 **TFC 未安装时自动跳过**：

- `RegionGenerator#continentFactor` 返回世界形状的掩码；
- `Region.Point#setLand` / `setIsland` / `setMountain` / `setCoastalMountain` / `setVolcanic` / `setBarrierIsland` 在地图外取消；
- TFC 生成器通过 `WorldgenAdapters` 的 `TfcWorldgenAdapter` 接收形状（按类名识别，核心 jar 不依赖 TFC）；
- 基础 `ChunkGeneratorMixin` 的结构/特征过滤同样作用于 TFC 生成器；
- 实体接缝环绕、极线推回、HUD 与命令与生成器无关，自动可用。

配置：`tfcCompatibility`（默认 `true`）、`tfcGridBlockSize`（默认 `128`，对应 TFC 1.21.1 的 `Units.GRID_BITS = 7`）。

## 优化模组兼容（Phase 5）

- **c2me**：不注入 `ChunkMap` / `ServerChunkCache` / `ChunkHolder` / 存储层；形状不可变、每生成器一份；`fillFromNoise` 期间的 16×16 掩码缓存使用 `ThreadLocal` 并在结束时显式清理。若遇到极端冲突，可把 `densityMask` 设为 `false`（退化为平坦海洋 + 群系/结构/特征过滤，仍可玩）。
- **Distant Horizons**：接缝和极线外是平坦海洋，DH 远景看到的是海面而不是虚空；本模组**不引用任何 DH 类**。挂载形状时会按世界半径输出推荐值：

$$
\text{EarthCurveRatio} \approx 160 \times \frac{20000}{H}
$$

也可以在代码里用 `SphericalWorldApi.getRecommendedDistantHorizonsCurveRatio(level)` 查询。
- LOD 镜像插件（把对侧 LOD 平移）不在 0.9.6 范围内；需要时应基于 DH 公开 API 做版本锁定的独立模块。

## 天空与环境（0.9.6）

客户端新增了一组可调的日月 / 天空视觉效果，并内置了一个可开关的资源包：

- **自动日月倾斜**：按玩家当前纬度自动计算日月轨道倾斜角；非球形维度用
  `z / 30000 * 90°` 估算。也可以关闭自动模式，使用固定角度。
- **日月贴图缩放**：太阳、月亮、其它天体分别有独立的缩放倍数。原版与 Ad Astra
  的自定义天空渲染都支持（Ad Astra 需安装并可关闭兼容开关）。
- **本地预设库**：8 个槽位，保存在 `config/e2generator-sky-presets.json`；在
  NeoForge 配置页底部可以打开预设库或一键重置天空设置。
- **内置资源包 `gameassets`**：首次启动默认启用，可在“资源包”界面关闭。包含
  原版太阳、Ad Astra / Ad Astra Extra 天空与行星材质，素材改编自 **Ad Astra 3D**
  （Yellowcake_3D，MIT；见 `NOTICE.md` 与 `gameassets/LICENSE.txt`）。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `skyTiltEnabled` | `true` | 是否启用日月轨道额外倾斜 |
| `skyTiltAuto` | `true` | 自动按玩家位置计算；`false` 时使用固定角度 |
| `skyTiltStrength` | `1.0` | 自动倾斜强度（纬度 × 强度） |
| `skyManualTilt` | `0.0` | 固定倾斜角（度） |
| `sunTextureScale` | `1.0` | 太阳贴图渲染缩放 |
| `moonTextureScale` | `1.0` | 月亮贴图渲染缩放 |
| `celestialTextureScale` | `1.0` | 其它天体（如 Ad Astra 行星）缩放 |
| `adAstraCompatibility` | `true` | 是否对 Ad Astra 自定义天空应用倾斜与缩放 |

所有选项都位于客户端配置 `config/e2generator-client.toml`，并可在游戏的模组配置界面中调整。

## 公开 API 与事件

`com.aigcs.e2generator.api` 下保留了查询入口，供其它模组查询当前世界的**星球半径**与**地理信息**。

### 星球半径

```java
OptionalDouble chunks = SphericalWorldApi.getPlanetRadiusChunks(level);
OptionalDouble blocks = SphericalWorldApi.getPlanetRadiusBlocks(level);
OptionalDouble circumference = SphericalWorldApi.getPlanetCircumference(level);
```

### 地理信息（0.9.5）

```java
// 一次性拿到完整快照
SphericalWorldApi.getPlanetGeography(level).ifPresent(geo -> {
    double primeMeridianX = geo.primeMeridianX();         // 本初子午线，恒为 0
    double antimeridianX = geo.antimeridianX();           // 赤道处的反子午线（接缝）
    double meridianMapLength = geo.meridianLengthOnMap(); // 地图上的子午线长度 2H
    double meridianArc = geo.meridianArcLength();         // 星球上的真实弧长 πR
    double equatorZ = geo.equatorZ();
    double northPoleZ = geo.northPoleZ();
    MapBounds bounds = geo.bounds();
    double distance = geo.surfaceDistance(39.9, 116.4, 31.2, 121.5); // 大圆距离（方块）
});

// 也可以单独查询
OptionalDouble primeMeridian = SphericalWorldApi.getPrimeMeridianX(level);
OptionalDouble seamX = SphericalWorldApi.getAntimeridianXAt(level, z);
OptionalDouble latitude = SphericalWorldApi.getLatitude(level, z);
Optional<GeographicPosition> position = SphericalWorldApi.getGeographicPosition(level, x, z);
```

| 方法 | 含义 |
| --- | --- |
| `getPlanetGeography(level)` | 完整快照 `PlanetGeography` |
| `getPrimeMeridianX(level)` | 本初子午线 x（恒为 0） |
| `getAntimeridianX(level)` / `getAntimeridianXAt(level, z)` | 反子午线 x（赤道处 / 指定 z） |
| `getMeridianLengthOnMap(level)` / `getMeridianArcLength(level)` | 地图子午线长度 `2H` / 真实弧长 `πR` |
| `getEquatorZ(level)` / `getEquatorWidth(level)` / `getEquatorLength(level)` | 赤道位置、地图宽度、真实周长 |
| `getNorthPoleZ(level)` / `getSouthPoleZ(level)` | 南北极线 z |
| `getMapBounds(level)` | 地图包围盒 `MapBounds` |
| `getPlanetSurfaceArea(level)` / `getMapArea(level)` | 球面面积 / 投影地图面积 |
| `getLatitude(level, z)` / `getLongitude(level, x, z)` | 方块坐标 → 经纬度 |
| `getZForLatitude(level, lat)` / `getXForLongitude(level, lon, z)` | 经纬度 → 方块坐标 |
| `getGeographicPosition(level, x, z)` | 经纬度 + 是否在地图内 + 陆地掩码 |
| `isInsideMap(level, x, z)` / `getLandMask(level, x, z)` | 地图轮廓 / 陆地掩码 |
| `getSurfaceDistance(level, lat1, lon1, lat2, lon2)` | 大圆距离（方块） |

- 半径由半子午线 H 与宽高比按等面积关系反推，与创建世界时选择的值一致。
- 所有方法允许传入 `null`；非球形世界返回空值（`Optional.empty()` / `OptionalDouble.empty()` / `false` / `null`），不会抛异常。
- 服务端结果权威；客户端使用登录/换维度时同步过来的形状。
- 生成器侧同样可用：`level.getChunkSource().getGenerator() instanceof PlanetRadiusProvider provider`
  后调用 `provider.planetRadiusChunks()` 或 `provider.planetGeography()`。
- 另有 `getRecommendedDistantHorizonsCurveRatio(level)`。

### 事件

所有事件都发布在 `NeoForge.EVENT_BUS` 上：

| 事件 | 时机 | 可取消 | 用途 |
| --- | --- | --- | --- |
| `WorldShapeCreatedEvent` | 新世界首次创建形状 | ✅（或用 `setShape` 替换） | 整合包定制/禁用球形世界 |
| `WorldShapeAttachedEvent` | 形状挂载到维度生成器 | ❌ | 地图模组、调试工具 |
| `SeamWrapEvent` | 实体即将跨接缝包裹 | ✅ | 接管自定义载具/传送逻辑 |
| `PolarPushbackEvent` | 实体即将被极线推回 | ✅ | 自定义极地玩法 |

```java
NeoForge.EVENT_BUS.addListener((WorldShapeCreatedEvent event) -> {
    event.setShape(myCustomShape);           // 或 event.setCanceled(true);
});

NeoForge.EVENT_BUS.addListener((SeamWrapEvent event) -> {
    if (myVehicleHandlesItself(event.getEntity())) {
        event.setCanceled(true);
    }
});

// 自定义 ChunkGenerator 接入
WorldgenAdapters.register(new MyWorldgenAdapter());
```

自定义生成器实现 `WorldgenAdapter` 后，`ShapeManager` 会在维度加载时调用
`attach(level, shape)`；未适配的生成器会退化为"实体环绕 + 有限边界"，并在日志中给出警告。

## 命令

- `/sphere info` —— 显示形状参数
- `/sphere geography`（别名 `/sphere geo`）—— 星球半径、本初子午线、反子午线、赤道、极线、地图范围
- `/sphere coords` —— 当前经纬度
- `/sphere locate <lat> <lon>` —— 经纬度换算为 x/z
- `/sphere tp <lat> <lon>` —— 传送到经纬度（需要 OP）
- `/sphere preset list` —— 列出内置与数据包形状预设
- `/sphere preset info <id>` —— 查看某个预设的尺寸参数

## 兼容性说明

- **原版 `NoiseBasedChunkGenerator`**：完整支持（密度/群系/结构/特征/高度）。
- **数据包 / Terralith / TerraBlender**：不解析、不改写数据包，只在运行时做掩码。
- **TFC**：独立可选适配模块（`e2generator.tfc.mixins.json` + `TfcWorldgenAdapter`）；未安装时自动跳过，核心 jar 不依赖 TFC。
- **c2me**：不触碰 `ChunkMap` / `ServerChunkCache` / 存储层；形状不可变，生成上下文与掩码缓存使用 `ThreadLocal` 并显式清理；可用 `densityMask=false` 退化。
- **Distant Horizons**：接缝与地图外是连续海面；会输出推荐的 Earth Curve Ratio，但不引用 DH 类。
- **自定义 `ChunkGenerator`**：通过 `WorldgenAdapter` SPI 接入；未适配的生成器会退化为"实体环绕 + 有限边界"。

## 实施状态

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| Phase 0–3 | 投影、形状、生成钩子、接缝包裹、HUD、结构/特征过滤、命令 | ✅ 已完成 |
| Phase 4 | TFC 适配（可选 Mixin + 生成器适配器） | ✅ 0.9.4 |
| Phase 5 | c2me/DH 兼容说明、性能优化（每区块掩码缓存、退化开关） | ✅ 0.9.4 |
| Phase 6 | 数据包形状预设、创建世界 UI、公开事件/API、文档 | ✅ 0.9.4 |
| 0.9.5 | 地理信息查询 API（本初子午线、反子午线、赤道、极线、地图范围、经纬度换算、大圆距离） | ✅ 已完成 |
| 0.9.6 | 天空与环境配置（自动日月倾斜、贴图缩放、8 槽位预设库、内置 gameassets 资源包、Ad Astra 兼容） | ✅ 已完成 |

## 构建

```bash
./gradlew build        # 产物在 build/libs/
./gradlew runClient    # 客户端开发环境
./gradlew runServer    # 专用服务器开发环境
```

## AIGC 声明与免责

### 生成方式

本项目的源代码、数据包、语言文件与文档使用 **DeepSeek AI** 辅助生成，并由维护者进行人工整理、编译与测试后发布。AI 生成内容可能包含错误、遗漏、过时信息或不符合预期的行为，**不保证**在所有 Minecraft / NeoForge / TFC / c2me / Distant Horizons 版本组合下都能正常工作。

### 免责声明

- 本模组按 **MIT License** 的 “AS IS” 条款提供，不附带任何明示或暗示的担保，包括但不限于适销性、特定用途适用性与非侵权担保。
- 因使用或无法使用本模组而导致的**存档损坏、数据丢失、服务器崩溃、客户端异常、整合包冲突、性能问题或其他损失**，作者与贡献者不承担责任。
- 本模组会通过 Mixin、密度函数包装、群系源与结构/特征过滤等方式修改世界生成。**请勿直接在已有重要存档上使用**；使用前请完整备份存档与服务端配置。
- AI 生成代码可能包含与特定模组版本不兼容、边界条件处理不完整或存在潜在崩溃风险的逻辑。请在**测试世界/测试服务器**中先行验证。
- 本模组不是 Mojang、Microsoft、NeoForged、TerraFirmaCraft 或 TFC Eratosthenes 的官方产品，也未获得上述项目的背书或认可。
- 若 AI 生成内容无意中使用了受版权保护的代码、文本或素材，请通过 GitHub Issue 联系维护者；核实后将修改或删除相关内容。
- 使用者应自行遵守 GitHub、CurseForge、Modrinth 等平台关于 AIGC 内容的政策，以及所在地区的法律法规。

### 使用注意

- 新建世界前先备份 `saves/` 与 `config/`；服务端升级前备份世界目录。
- 先在单人测试世界中验证世界生成、接缝环绕、极线推回与 TFC 兼容行为，再应用到正式存档。
- 服务端与客户端应使用相同模组版本；客户端未安装本模组时仅缺少 HUD，但版本不一致仍可能产生同步问题。
- 与 c2me、Distant Horizons、Terralith、TerraBlender、TFC 等模组组合时，请先阅读上方兼容性说明并在测试环境验证。
- 如遇到问题，请在 GitHub Issues 中提供 Minecraft/NeoForge 版本、模组列表、`latest.log` 与崩溃报告。

## 许可证与致谢

本模组以 **MIT License** 发布，完整文本见 [`LICENSE`](LICENSE)。第三方组件与灵感来源的说明见 [`NOTICE.md`](NOTICE.md)。

### 设计灵感：TFC Eratosthenes

本模组在**设计思路**上受到 **TFC Eratosthenes** 的启发（有限球形世界、180° 经线环绕、极地区域处理、以地图投影约束大陆生成、Distant Horizons 曲率建议等）。本项目是**独立实现**，不包含、不复制、不链接 TFC Eratosthenes 的源代码，因此可以按 MIT 发布。

- 项目：TFC Eratosthenes
- 作者：Trygve55
- 许可证：**GNU General Public License v3.0 or later（GPL-3.0-or-later）**
- 源码：<https://github.com/trygve55/tfc_eratosthenes>
- Modrinth：<https://modrinth.com/mod/tfc-eratosthenes>
- CurseForge：<https://www.curseforge.com/minecraft/mc-mods/tfc-eratosthenes>

TFC Eratosthenes 的代码与素材版权归其作者所有，并遵循 GPL-3.0-or-later；本仓库对它的引用仅用于说明设计来源与兼容目标。

### 其他致谢

- **Equal Earth 投影**：Šavrič, Patterson & Jenny (2018)；参考实现为 [d3-geo 的 `equalEarth`](https://github.com/d3/d3-geo/blob/main/src/projection/equalEarth.js)（ISC License）。本模组的投影代码为自行实现，仅使用论文公开的公式与常数。
- **NeoForge MDK**：构建模板来自 github.com/NeoForged/MDK，MIT License，见 [`TEMPLATE_LICENSE.txt`](TEMPLATE_LICENSE.txt)。
- **TerraFirmaCraft**：仅作为可选的编译期兼容目标，不随本模组分发。

