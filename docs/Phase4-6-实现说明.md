# Phase 4–6 实现说明

本文档对应设计文档 `fromgithub/docs/球形世界模组设计方案.md` 中 Phase 4–6 的实现细节，
版本 **0.9.4**；地理信息查询 API 的补充说明见 `docs/0.9.5-地理信息API.md`。

---

## Phase 4：TFC 适配（可选）

### 为什么要独立模块

TFC 不使用 `NoiseBasedChunkGenerator`，而是自己的 `TFCChunkGenerator` +
`RegionGenerator` + `Region.Point`。因此核心 Mixin（密度/群系/结构）无法直接裁剪 TFC 的大陆。

### 实现

| 文件 | 作用 |
| --- | --- |
| `compat/tfc/TfcMixinPlugin` | `IMixinConfigPlugin`，TFC 未安装时跳过整个 TFC Mixin 配置 |
| `compat/tfc/mixin/RegionGeneratorMixin` | 在 `continentFactor(Region.Point)` HEAD 返回形状掩码 |
| `compat/tfc/mixin/RegionPointMixin` | 在六个 `setXxx` 标记方法 HEAD 取消地图外的点；同时实现 `TfcPointAccess` |
| `compat/tfc/TfcShapeBridge` | 保存当前 `WorldShape`、网格→方块换算、掩码查询；不引用 TFC 类型 |
| `compat/tfc/TfcWorldgenAdapter` | 通过 `WorldgenAdapters` 在维度加载时把形状交给桥 |
| `e2generator.tfc.mixins.json` | 独立 Mixin 配置，`plugin` 指向 `TfcMixinPlugin` |

- TFC 1.21.1 的 `Units.GRID_BITS = 7`，所以一格 = 128 方块；可用
  `tfcGridBlockSize` 覆盖。
- 采用 `@Mixin(targets = "...")` + `@Coerce Object`，编译期不需要 TFC 依赖。
- `ChunkGeneratorMixin#spherical$shape()` 对非 `ShapeAware` 生成器回退到
  `TfcShapeBridge.shapeForGenerator(this)`，因此 TFC 的结构/特征同样被过滤。
- 未安装 TFC：Mixin 配置被插件跳过；`TfcWorldgenAdapter.supports()` 返回
  `false`；核心 jar 不加载任何 TFC 类。

### 测试清单

- TFC 单独：出生点、大陆轮廓、海洋缓冲、极线推回。
- TFC + 本模组：`/sphere info`、`/sphere coords`、接缝环绕、HUD。
- 去掉 TFC：客户端/服务端均正常启动，日志无 TFC 相关错误。

---

## Phase 5：c2me / Distant Horizons 与性能

### 性能优化

1. **每区块 16×16 掩码缓存**
   - `NoiseBasedChunkGenerator#fillFromNoise` HEAD：`SphericalContext.beginChunk(shape, pos)`；
   - RETURN：`SphericalContext.endChunk()`；
   - 平坦海洋捷径取消时也显式 `endChunk()`，避免 RETURN 注入被跳过导致泄漏；
   - `MaskedDensity` 通过 `SphericalContext.maskAt(x, z)` 查表，未命中时回退直接计算。
2. **配置开关**
   - `chunkMaskCache`（默认 `true`）：关闭后不预计算，逐点计算掩码；
   - `densityMask`（默认 `true`）：关闭后不包装密度函数，保留平坦海洋捷径、
     群系/结构/特征过滤与实体包裹（c2me 极端冲突时的退化模式）。

### c2me

- 不注入 `ChunkMap` / `ServerChunkCache` / `ChunkHolder` / 存储层。
- `WorldShape` 不可变；形状挂在每个生成器实例上。
- 所有 `ThreadLocal`（形状上下文、区块掩码）都在 `fillFromNoise` 结束时清理。
- 启动日志会输出 `c2me=true/false` 与当前缓存/密度模式。

### Distant Horizons

- 接缝/极线外使用 `OceanChunkFiller` 生成连续平坦海洋，LOD 不会看到虚空。
- 不引用任何 DH 类，不做 DH Mixin。
- 挂载形状时输出推荐值：

$$
\text{EarthCurveRatio} = \text{dhReferenceCurveRatio} \times
\frac{\text{dhReferenceHalfMeridian}}{H}
$$

默认参考点 20,000 方块 → 160，与 TFC Eratosthenes 的惯例一致。
API：`SphericalWorldApi.getRecommendedDistantHorizonsCurveRatio(level)`。

LOD 镜像插件（把对侧 LOD 平移）需要依赖 DH 的版本化 API，0.9.4 未包含；
本版本以“连续海洋 + 曲率推荐值”为兼容方案，避免与 DH 内部类产生硬绑定。

---

## Phase 6：数据包预设、API/事件、文档

### 形状预设

文件：`data/<namespace>/e2generator/shape/<path>.json`。

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `description` | `""` | 显示在 `/sphere preset info` |
| `planetRadiusChunks` | — | 星球半径（区块），优先于 `halfMeridian` |
| `halfMeridian` | `8000` | 赤道到极线距离 H |
| `widthToHeightRatio` | `2.0` | 赤道半宽 / H |
| `boundaryMode` | `OUTLINE` | `OUTLINE` / `RECTANGLE` |
| `equatorZ` | `0` | 赤道 z |
| `seamBuffer` | `384` | 接缝海洋带 |
| `polarBuffer` | `192` | 极线海洋带 |
| `softEdgeWidth` | `96` | 软过渡宽度 |
| `structureMargin` | `256` | 结构余量 |
| `meridianGrace` | `32` | 接缝触发余量 |
| `oceanFloorY` | `24` | 海床高度 |
| `fallbackBiome` | `minecraft:deep_ocean` | 兜底海洋群系 |

内置预设：`e2generator:earth`、`half_world`、`compact`、`large`、`rectangle`。
配置 `shapePreset` 选择预设；已有存档不受影响。`/reload` 会重新扫描数据包。

### 形状解析优先级

1. `PendingWorldSettings.consumePreset()`（世界创建流程指定）；
2. `PendingWorldSettings.consume()`（创建世界控制器里的星球参数）；
3. 配置 `shapePreset`；
4. 单独的形状配置项。

随后发布 `WorldShapeCreatedEvent`，监听器可以 `setShape(...)` 或取消。

### 公开事件

| 事件 | 可取消 | 说明 |
| --- | --- | --- |
| `WorldShapeCreatedEvent` | ✅ | 新世界首次创建形状；`getSource()` 返回 `CONFIG` / `PLANET_SETTINGS` / `PRESET` |
| `WorldShapeAttachedEvent` | ❌ | 形状挂载到维度生成器 |
| `SeamWrapEvent` | ✅ | 实体即将跨接缝包裹（根载具） |
| `PolarPushbackEvent` | ✅ | 实体即将被极线推回 |

### 生成器 SPI

```java
public interface WorldgenAdapter {
    ResourceLocation id();
    boolean supports(ServerLevel level, ChunkGenerator generator);
    void attach(ServerLevel level, @Nullable WorldShape shape);
    default double maskAt(ServerLevel level, double x, double z) { return 1.0; }
}
```

注册：`WorldgenAdapters.register(adapter)`。内置 TFC 适配器在模组构造期注册。

### 文档

- `README.md`：功能、配置、预设、TFC、优化模组、API/事件、实施状态。
- `docs/0.9.5-地理信息API.md`：地理信息查询入口的坐标约定、字段语义与示例。
- `docs/0.9.6-天空与环境配置.md`：天空倾斜、贴图缩放、Ad Astra 兼容、8 槽位预设库与 `gameassets` 资源包。
- 设计文档路线图已标记 Phase 4–6 完成。
- 本文件：实现与测试清单。

---

## 回归测试建议

1. **原版**：新建世界 → 出生点、边界海洋、接缝环绕、极线推回、HUD、命令。
2. **数据包预设**：放置自定义 JSON → `/sphere preset list` → 在配置中指定 → 新建世界。
3. **事件**：注册 `WorldShapeCreatedEvent` 替换形状；注册 `SeamWrapEvent` 取消包裹。
4. **TFC**：安装 TFC 后重复第 1 项；检查大陆不越过接缝/极线。
5. **c2me**：并行生成下无死锁、无跨区块掩码串线；关闭 `densityMask` 后仍可玩。
6. **DH**：接缝与地图外为连续海面；按日志建议设置 Earth Curve Ratio。
7. **专用服务器 + 无模组客户端**：可进服，服务端功能正常，客户端无 HUD。
