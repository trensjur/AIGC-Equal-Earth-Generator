# Changelog

## 0.9.6

### 天空与环境视觉效果

- 新增客户端配置：`skyTiltEnabled`、`skyTiltAuto`、`skyTiltStrength`、`skyManualTilt`、
  `sunTextureScale`、`moonTextureScale`、`celestialTextureScale`、`adAstraCompatibility`。
- 原版天空 Mixin（`LevelRendererMixin`）：
  - 太阳 / 月亮四边形支持独立缩放；
  - 日月轨道可附加倾斜角，自动模式按玩家纬度变化；非球形维度退化为
    `z / 30000 * 90°`。
- 可选 Ad Astra 天空兼容（`e2generator.adastra.mixins.json` + `AdAstraMixinPlugin`）：
  对 `ModSkyRenderer` 的天体贴图按路径应用太阳 / 月亮 / 其它天体缩放，并叠加倾斜。
- 新增本地预设库（8 个槽位，`config/e2generator-sky-presets.json`），
  配置页底部新增“打开本地预设库”与“重置天空设置”按钮。
- 新增内置资源包 `gameassets`，首次启动默认启用，可在“资源包”界面关闭：
  - `assets/minecraft/textures/environment/sun.png`；
  - `assets/minecraft/textures/environment/moon_phases.png`（由 `moon.png` 生成）；
  - `assets/ad_astra/textures/environment/*.png`；
  - `assets/adastraextra/textures/sky/*.png`；
  - `assets/stellarview/textures/environment/...`（附加兼容）。
  - 贴图改编自 **Ad Astra 3D**（Yellowcake_3D，MIT），见 `NOTICE.md` 与
    `gameassets/LICENSE.txt`。
- 版本号 0.9.5 → 0.9.6。

## 0.9.5

- 许可证由 All Rights Reserved 改为 **MIT**，新增 `LICENSE` 与 `NOTICE.md`。
- 在 README、模组元数据（`credits`）中注明本模组受 **TFC Eratosthenes** 启发，并按该项目
  **GPL-3.0-or-later** 的要求注明作者（Trygve55）与源码链接。
- 删除 `fromgithub/docs/TFC-Eratosthenes-模组解析.md`。
- 在 README 与 `NOTICE.md` 中增加 **DeepSeek AI** 生成声明、AIGC 内容免责声明与使用注意事项。

### 地理信息查询 API

- 新增公开数据类型：
  - `PlanetGeography`：半径、赤道周长、半子午线、本初子午线、反子午线、赤道、极线、
    地图范围、球面面积/地图面积、大圆距离与经纬度换算。
  - `MapBounds`：地图包围盒（`minX` / `maxX` / `minZ` / `maxZ`）。
  - `GeographicPosition`：方块坐标对应的纬度、经度、是否在地图内、陆地掩码。
- `SphericalWorldApi` 新增查询入口：
  `getPlanetGeography`、`getPrimeMeridianX`、`getAntimeridianX`、`getAntimeridianXAt`、
  `getMeridianLengthOnMap`、`getMeridianArcLength`、`getEquatorLength`、`getEquatorZ`、
  `getEquatorWidth`、`getNorthPoleZ`、`getSouthPoleZ`、`getMapBounds`、
  `getPlanetSurfaceArea`、`getMapArea`、`getLatitude`、`getLongitude`、
  `getZForLatitude`、`getXForLongitude`、`getGeographicPosition`、`isInsideMap`、
  `getLandMask`、`getSurfaceDistance`。
- `PlanetRadiusProvider` 新增默认方法 `planetGeography()`，
  `NoiseBasedChunkGenerator` Mixin 已实现。
- 新命令：`/sphere geography`（别名 `/sphere geo`）。
- 版本号 0.9.4 → 0.9.5。

## 0.9.4

Phase 4–6 全部实现。

### Phase 4 — TFC 兼容（可选）

- 新增 `e2generator.tfc.mixins.json`，由 `TfcMixinPlugin` 在未安装 TFC 时自动跳过。
- `RegionGeneratorMixin`：用世界形状掩码替换 `RegionGenerator#continentFactor`。
- `RegionPointMixin`：在地图外取消 `setLand` / `setIsland` / `setMountain` /
  `setCoastalMountain` / `setVolcanic` / `setBarrierIsland`。
- 新增 `TfcShapeBridge`（不引用 TFC 类型）与 `TfcWorldgenAdapter`（按类名识别 TFC 生成器）。
- `ChunkGeneratorMixin` 对 TFC 生成器同样执行结构/特征过滤。
- 新配置：`tfcCompatibility`、`tfcGridBlockSize`。

### Phase 5 — 优化模组兼容与性能

- 每区块 16×16 掩码缓存（`SphericalContext.beginChunk/endChunk`），密度包装器查表，
  使用 `ThreadLocal` 并在填充结束时显式清理。
- 新配置：`chunkMaskCache`、`densityMask`（c2me 冲突时的退化开关）。
- 新增 `OptimizationCompat`：检测 c2me / Distant Horizons，输出兼容模式与推荐的
  “Earth Curve Ratio”（`160 × 20000 / H`，可配置参考值）。
- `SphericalWorldApi` 新增 `getPlanetCircumference` 与
  `getRecommendedDistantHorizonsCurveRatio`。

### Phase 6 — 数据包预设、API/事件、文档

- 新增 `ShapePreset` 与 `ShapePresetManager`：支持内置预设与
  `data/<namespace>/e2generator/shape/<path>.json`。
- 内置预设：`e2generator:earth`、`half_world`、`compact`、`large`、`rectangle`。
- 新配置：`shapePreset`；`WorldShapeData` 按“待应用预设 → 控制器参数 → 配置预设 → 单独配置”解析。
- 新命令：`/sphere preset list`、`/sphere preset info <id>`。
- 新增公开事件：`WorldShapeCreatedEvent`、`WorldShapeAttachedEvent`、
  `SeamWrapEvent`、`PolarPushbackEvent`。
- 新增 `WorldgenAdapter` SPI 与 `WorldgenAdapters` 注册表。
- 更新 README、实施状态与文档；版本号 0.9.3 → 0.9.4。
