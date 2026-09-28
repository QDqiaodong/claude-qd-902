# 生活垃圾焚烧发电厂 · 进厂与焚烧运行管理

以「垃圾进厂计重 → 储坑发酵 → 焚烧炉 → 余热锅炉发电 → 烟气净化 → 灰渣与渗滤液」为主线的焚烧发电厂运行管理系统。

## 技术栈

- 后端：Spring Boot 3.3.0 + Java 17 + Spring Data JPA(Hibernate 6.5) + MySQL 8 + Redis
- 前端：Vue 3 + Element Plus + Vite
- 端口：前端 8292、后端 8392、MySQL 3592、Redis 6592

## 启动

```bash
./start.sh            # 已构建过：直接起
./start.sh --build    # 首次或改过代码：构建并起
./stop.sh             # 暂停
./clean.sh            # 收摊（down -v）
```

访问 http://127.0.0.1:8292/ ，健康检查 http://127.0.0.1:8392/actuator/health

## 五大模块

| 模块 | 说明 | 主要接口 |
| --- | --- | --- |
| 进厂与计量 | 行政区划/中转站来源、车牌、垃圾类别、毛重/皮重/净重、地磅号 | `/api/intake/**` |
| 垃圾储坑 | 坑位容量与库存、堆酵天数、渗滤液抽出、坑温、抓斗投料 | `/api/pits/**` |
| 焚烧炉与炉型 | 炉排炉 / 流化床 / 回转窑 单表继承，炉膛温度、停留时间、启停炉 | `/api/incinerators/**` |
| 余热锅炉与发电 | 蒸汽压力/温度/蒸发量、装机、发电量、上网电量、厂用电率 | `/api/power/**` |
| 环保排放与灰渣 | 六项污染物、超标告警、灰渣与渗滤液台账 | `/api/environment/**` |

## 数据访问设计（本仓技术标识）

`Incinerator` 为抽象父类，`GrateFurnace` / `FluidizedBed` / `RotaryKiln` 三个子类共用一张 `incinerator` 表，
由 Hibernate `@DiscriminatorFormula`（SQL CASE WHEN 表达式）判别子类，而非 `@DiscriminatorColumn` 字符串列：

```java
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("CASE WHEN furnace_type_code = 'GRATE' THEN 'GRATE' ... END")
public abstract class Incinerator { ... }
```

配套查询：

- `TYPE()`：`/api/incinerators/stats/type` 按子类统计台数、当日入炉量、平均炉膛温度
- `treat()`：`/api/incinerators/fast-grate?speed=6` 访问子类专属字段（炉排速度）
- 手写 JPQL：按来源区域汇总进厂量与热值、按炉号统计月度处理量与发电量、排放超标次数排行

## 业务规则（service 内校验，抛 BizException）

1. 净重 = 毛重 − 皮重，必须为正；超出地磅量程上限（45 t）拒绝
2. 炉膛温度 < 850℃ 或烟气停留 < 2 s，不允许判定为达标燃烧（逐项列出不达标项）
3. 任一排放指标超标：自动登记环保告警，且不允许在该时段标记为「正常排放」
4. 储坑库存超过容量不允许继续卸料（提示剩余可容纳余量）
5. 发电量不得超过 装机容量 × 1000 × 运行小时 的理论上限

## 目录

```
backend/src/main/java/com/waste/power/
  common/      统一返回与异常处理、垃圾发电词典 Dict
  domain/      9 个实体（含 Incinerator 继承体系）
  dto/         请求/视图与 JPQL 投影对象
  repository/  数据访问（含 TYPE() / treat() / 手写 JPQL）
  service/     业务规则
  controller/  /api 前缀接口
mysql/schema.sql            建表 + 种子数据（构建期导入）
backend/src/main/resources/schema.sql  同上副本
frontend/src/               App.vue（顶部工艺流程导航）+ 四个页面
```
