# DeadSouls 插件 - 权限和指令总览

## 📋 目录
- [指令列表](#指令列表)
- [权限列表](#权限列表)
- [权限默认值说明](#权限默认值说明)

---

## 🎮 指令列表

### 主指令：`/souls`

#### 基础用法
```
/souls
```
列出玩家自己或所有灵魂（取决于权限）。

---

### 子指令详解

#### 1. **显示灵魂列表** (默认，无参数)
```
/souls
/souls page <页码>
```
**说明**：显示当前世界中可访问的灵魂列表
- 显示灵魂编号、存在时长、坐标（如有权限）、距离（如有权限）
- 支持分页显示（当灵魂数量超过配置的每页数量时）
- **所需权限**：`deadsouls.souls.list` 或 `deadsouls.souls.list.all`

**示例**：
```
/souls           # 显示第1页
/souls page 2    # 显示第2页
```

---

#### 2. **传送到灵魂** (goto 子命令)
```
/souls goto <灵魂ID>
```
**说明**：传送玩家到指定ID的灵魂位置
- 仅在游戏中可用（控制台无法使用）
- 玩家可传送到自己的灵魂（需要 `deadsouls.souls.teleport`）
- OP可传送到任何灵魂（需要 `deadsouls.souls.teleport.all`）
- 灵魂必须存在且在同一世界中

**示例**：
```
/souls goto 0    # 传送到ID为0的灵魂
```

---

#### 3. **传送到最新灵魂** (back 子命令)
```
/souls back
/souls back <玩家名>
```
**说明**：传送玩家到最新的灵魂位置（跨维度/世界）
- `/souls back` - 将自己传送到自己最新的灵魂（需要 `deadsouls.souls.back`）
- `/souls back <玩家名>` - 将指定玩家传送到该玩家最新的灵魂（需要 `deadsouls.souls.back.all`）
- 支持跨维度/世界传送
- 仅在游戏中可用（`/souls back` 无参数时），控制台可使用 `/souls back <玩家名>`

**示例**：
```
/souls back          # 传送自己到最新的灵魂
/souls back Steve    # 将 Steve 传送到他最新的灵魂
```

---

#### 4. **释放灵魂** (free 子命令)
```
/souls free <灵魂ID>
```
**说明**：释放灵魂，使其变为可自由获取状态
- 玩家可释放自己的灵魂（需要 `deadsouls.souls.release`）
- OP可释放任何灵魂（需要 `deadsouls.souls.release.all`）
- 仅在灵魂创建后的"释放计时器"时限内可释放
- 一旦释放，任何人都可以获取该灵魂中的物品和经验

**示例**：
```
/souls free 0    # 释放ID为0的灵魂
```

---

## 🔐 权限列表

### 权限总览表

| 权限节点 | 默认值 | 功能描述 |
|---------|--------|---------|
| `deadsouls.souls.spawn` | `true` | 死亡时生成灵魂 |
| `deadsouls.souls.save.items` | `true` | 灵魂保存玩家物品 |
| `deadsouls.souls.save.experience` | `true` | 灵魂保存玩家经验 |
| `deadsouls.souls.list` | `true` | 列出自己的灵魂 |
| `deadsouls.souls.list.all` | `op` | 列出所有玩家的灵魂 |
| `deadsouls.souls.release` | `true` | 释放自己的灵魂 |
| `deadsouls.souls.release.all` | `op` | 释放任何灵魂 |
| `deadsouls.souls.spectator` | `true` | 旁观者模式下查看所有灵魂 |
| `deadsouls.souls.coordinates` | `false` | 显示灵魂坐标 |
| `deadsouls.souls.distance` | `false` | 显示灵魂距离 |
| `deadsouls.souls.teleport` | `false` | 传送到自己的灵魂 |
| `deadsouls.souls.teleport.all` | `op` | 传送到任何灵魂 |
| `deadsouls.souls.back` | `false` | 传送到自己最新的灵魂（跨世界） |
| `deadsouls.souls.back.all` | `op` | 将任意玩家传送到其最新的灵魂（跨世界） |

---

### 权限详细说明

#### **死亡相关权限**

##### 1. `deadsouls.souls.spawn`
- **默认值**: `true`（所有人）
- **功能**: 允许玩家在死亡时生成灵魂
- **移除此权限的后果**: 玩家死亡时不会生成灵魂，物品和经验按原版方式掉落

##### 2. `deadsouls.souls.save.items`
- **默认值**: `true`（所有人）
- **功能**: 允许灵魂保存玩家的物品
- **前置要求**: 需要有 `deadsouls.souls.spawn` 权限
- **移除此权限的后果**: 死亡时物品不会被保存到灵魂，直接按原版方式掉落

##### 3. `deadsouls.souls.save.experience`
- **默认值**: `true`（所有人）
- **功能**: 允许灵魂保存玩家的经验值
- **前置要求**: 需要有 `deadsouls.souls.spawn` 权限
- **移除此权限的后果**: 死亡时经验不会被保存到灵魂，直接按原版方式掉落

---

#### **查询相关权限**

##### 4. `deadsouls.souls.list`
- **默认值**: `true`（所有人）
- **功能**: 允许使用 `/souls` 指令列出自己的灵魂
- **使用场景**: 玩家可以查看属于自己的灵魂列表

##### 5. `deadsouls.souls.list.all`
- **默认值**: `op`（仅管理员）
- **功能**: 允许使用 `/souls` 指令列出**所有玩家**的灵魂
- **前置要求**: 需要有 `deadsouls.souls.list` 权限
- **使用场景**: 管理员可以查看整个世界的所有灵魂

##### 6. `deadsouls.souls.coordinates`
- **默认值**: `false`（无人）
- **功能**: 在 `/souls` 列表和死亡信息中显示灵魂的坐标
- **格式**: 显示X、Y、Z坐标
- **使用场景**: 让玩家更容易找到自己的灵魂位置

##### 7. `deadsouls.souls.distance`
- **默认值**: `false`（无人）
- **功能**: 在 `/souls` 列表中显示灵魂距离玩家的远近
- **格式**: 显示方块距离（四舍五入整数）
- **使用场景**: 帮助玩家判断灵魂的远近

---

#### **管理相关权限**

##### 8. `deadsouls.souls.release`
- **默认值**: `true`（所有人）
- **功能**: 允许使用 `/souls free <id>` 指令释放**自己的灵魂**
- **限制**: 仅在灵魂生成后的"释放计时器"时限内可用
- **使用场景**: 玩家可以主动释放自己的灵魂，使其变为无主状态

##### 9. `deadsouls.souls.release.all`
- **默认值**: `op`（仅管理员）
- **功能**: 允许使用 `/souls free <id>` 指令释放**任何灵魂**
- **前置要求**: 需要有 `deadsouls.souls.release` 权限
- **使用场景**: 管理员可以释放其他玩家或无主的灵魂

##### 10. `deadsouls.souls.teleport`
- **默认值**: `false`（无人）
- **功能**: 允许使用 `/souls goto <id>` 指令传送到**自己的灵魂**
- **限制**: 仅在游戏中可用
- **使用场景**: 玩家可以快速传送回自己的死亡地点

##### 11. `deadsouls.souls.teleport.all`
- **默认值**: `op`（仅管理员）
- **功能**: 允许使用 `/souls goto <id>` 指令传送到**任何灵魂**
- **前置要求**: 需要有 `deadsouls.souls.teleport` 权限
- **使用场景**: 管理员可以传送到任何灵魂位置进行调查

##### 12. `deadsouls.souls.back`
- **默认值**: `false`（无人）
- **功能**: 允许使用 `/souls back` 指令传送到**自己最新的灵魂**（跨所有维度/世界）
- **限制**: 仅在游戏中可用
- **使用场景**: 玩家死亡后可以快速传送回最新的灵魂位置，无需记住灵魂ID

##### 13. `deadsouls.souls.back.all`
- **默认值**: `op`（仅管理员）
- **功能**: 允许使用 `/souls back <玩家>` 指令将**任意在线玩家**传送到其最新的灵魂
- **使用场景**: 管理员可以帮助玩家传送回灵魂位置，支持从控制台执行

---

#### **特殊权限**

##### 14. `deadsouls.souls.spectator`
- **默认值**: `true`（所有人）
- **功能**: 允许处于旁观者模式的玩家查看**所有灵魂**
- **特殊行为**: 旁观者模式下，无论灵魂属于谁，都能看到所有灵魂的粒子效果
- **使用场景**: 用于调试或管理员调查

---

## 📊 权限默认值说明

### 三种默认值类型

| 值 | 含义 | 说明 |
|----|------|------|
| `true` | 所有人 | 所有玩家默认拥有此权限 |
| `false` | 无人 | 所有玩家默认不拥有此权限（可通过权限插件赋予） |
| `op` | 仅OP | 仅服务器管理员(OP)默认拥有此权限 |

---

## 🎯 常见权限配置场景

### 场景 1: 纯生存模式（推荐普通玩家配置）
```
允许:
- deadsouls.souls.spawn (默认)
- deadsouls.souls.save.items (默认)
- deadsouls.souls.save.experience (默认)
- deadsouls.souls.list (默认)
- deadsouls.souls.release (默认)

可选赋予:
- deadsouls.souls.coordinates
- deadsouls.souls.distance
- deadsouls.souls.teleport
```

### 场景 2: 管理员配置
```
赋予所有权限 (使用权限插件的通配符):
deadsouls.souls.*
```

### 场景 3: 无脑玩家保护
```
允许:
- deadsouls.souls.spawn
- deadsouls.souls.save.items
- deadsouls.souls.save.experience
- deadsouls.souls.list

禁止:
- deadsouls.souls.release (防止玩家误操作释放灵魂)
- deadsouls.souls.teleport (防止玩家滥用传送)
```

### 场景 4: PvP 服务器配置
```
根据 pvp-behavior 配置值选择:
- NORMAL: 所有权限照常
- DISABLED: 移除 deadsouls.souls.spawn (PvP死亡不生成灵魂)
- RELEASED: 赋予 deadsouls.souls.list.all (所有人可看到所有灵魂)
```

---

## 🔗 与配置文件的关系

### 影响权限功能的配置项

| 配置项 | 相关权限 | 说明 |
|--------|---------|------|
| `soul-release-timer` | `deadsouls.souls.release` | 控制释放灵魂的时限 |
| `pvp-behavior` | `deadsouls.souls.spawn` | 控制PvP时是否生成灵魂 |
| `saved-experience` | `deadsouls.souls.save.experience` | 控制保存的经验比例 |
| `enabled-worlds` | `deadsouls.souls.spawn` | 控制哪些世界可以生成灵魂 |

---

## 📝 权限继承关系

```
deadsouls.souls.list.all
    └─ 需要: deadsouls.souls.list

deadsouls.souls.release.all
    └─ 需要: deadsouls.souls.release

deadsouls.souls.teleport.all
    └─ 需要: deadsouls.souls.teleport

deadsouls.souls.back.all
    └─ 需要: deadsouls.souls.back

deadsouls.souls.save.items
    └─ 需要: deadsouls.souls.spawn

deadsouls.souls.save.experience
    └─ 需要: deadsouls.souls.spawn
```

---

## 💡 提示

- 使用权限插件（如 LuckPerms）可以更灵活地管理这些权限
- 大多数权限检查在指令执行时进行，玩家在没有权限时会收到提示信息
- 灵魂的可见范围和收集范围由配置文件中的参数控制，与权限无关
- 旁观者模式有特殊的权限检查，允许查看所有灵魂


