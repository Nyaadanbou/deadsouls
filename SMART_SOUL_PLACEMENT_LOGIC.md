# Smart Soul Placement 核心逻辑分析

## 概述
当配置选项 `smart-soul-placement: true` 启用时，DeadSouls 插件会记录玩家最后一个"安全位置"，并在玩家死亡时使用该位置作为灵魂生成位置，而不是直接在死亡位置生成。

---

## 核心数据结构

### PlayerSoulInfo 类（第937行）
```java
private static final class PlayerSoulInfo {
    static final double SOUL_HOVER_OFFSET = 1.2;  // 灵魂浮升偏移量
    
    @NotNull
    final Location lastKnownLocation = new Location(null, 0, 0, 0);  // 最后记录的位置
    
    @NotNull
    final Location lastSafeLocation = new Location(null, 0, 0, 0);   // 最后的安全位置
}
```

---

## 核心逻辑流程

### 1. **安全位置记录机制**（第165-175行）

**位置：** `DeadSoulsPlugin.processPlayers()` 方法

```java
if (playerGameMode != GameMode.SPECTATOR) {
    final Block underPlayer = world.getBlockAt(
        playerLocation.getBlockX(), 
        playerLocation.getBlockY() - 1, 
        playerLocation.getBlockZ()
    );
    if (underPlayer.getType().isSolid()) {  // 脚下是固体方块
        final Block atPlayer = world.getBlockAt(
            playerLocation.getBlockX(), 
            playerLocation.getBlockY(), 
            playerLocation.getBlockZ()
        );
        if (atPlayer.getType() != Material.LAVA) {  // 所在位置不是熔岩
            // 标记为安全位置
            set(info.lastSafeLocation, playerLocation);
        }
    }
}
```

**安全位置的条件：**
- 玩家游戏模式不是旁观者
- **玩家脚下必须有固体方块**
- **玩家所在位置不能是熔岩**

---

### 2. **安全位置查找机制**（第949-957行）

**位置：** `PlayerSoulInfo.findSafeSoulSpawnLocation()` 方法

```java
@NotNull
Location findSafeSoulSpawnLocation(@NotNull Player player) {
    final Location playerLocation = player.getLocation();
    
    // 检查最后安全位置是否距离玩家不超过20方块
    if (isNear(lastSafeLocation, playerLocation, 20)) {
        set(playerLocation, lastSafeLocation);
        playerLocation.setY(playerLocation.getY() + SOUL_HOVER_OFFSET);  // 加1.2格偏移
        return playerLocation;
    }
    
    // 距离超过20方块，使用备用方案
    return findFallbackSoulSpawnLocation(player, playerLocation, true);
}
```

**关键决策点：**
- 如果最后记录的安全位置**距离当前位置不超过20方块**，使用该位置
- 否则调用备用方案 `findFallbackSoulSpawnLocation()`

---

### 3. **备用灵魂生成位置逻辑**（第960-992行）

**位置：** `PlayerSoulInfo.findFallbackSoulSpawnLocation()` 静态方法

```java
@NotNull
static Location findFallbackSoulSpawnLocation(
    @NotNull Player player, 
    @NotNull Location playerLocation, 
    boolean improve
) {
    final World world = player.getWorld();
    
    final int x = playerLocation.getBlockX();
    int y = Util.clamp(playerLocation.getBlockY(), 
                       world.getMinHeight(), 
                       world.getMaxHeight());
    final int z = playerLocation.getBlockZ();
    
    if (improve) {  // improve=true 时执行优化
        int yOff = 0;
        while (true) {
            final Material type = world.getBlockAt(x, y + yOff, z).getType();
            
            if (type.isSolid()) {
                // 遇到固体方块，停止上升
                yOff = 0;
                break;
            } else if (type == Material.LAVA) {
                // 遇到熔岩，继续向上搜索
                yOff++;
                
                if (yOff > 8) {
                    // 熔岩柱超过8格深，放弃优化
                    yOff = 0;
                    break;
                }
            } else {
                // 找到安全空气位置，停止
                break;
            }
        }
        
        y += yOff;
    }
    
    // 最终高度：死亡位置 + 自动调整偏移 + 1.2格浮升
    playerLocation.setY(y + SOUL_HOVER_OFFSET);
    return playerLocation;
}
```

**备用方案的优化策略：**
1. 以死亡点的X、Z坐标为基础
2. 从死亡点Y坐标向上扫描方块
3. **固体方块** → 停止，不上升
4. **熔岩** → 继续向上扫描（最多8格）
5. **空气** → 停止，使用该位置
6. 最终位置加上1.2格浮升偏移

---

## 实际应用场景

### 场景1：掉进熔岩池
- 玩家在岩浆中死亡
- 最后安全位置：进入岩浆前的位置（脚下有固体、周围无岩浆）
- **结果：** 灵魂生成在岩浆外，便于回收

### 场景2：从悬崖坠落
- 玩家跳下悬崖死亡
- 最后安全位置：悬崖顶端（脚下有方块）
- 距离超过20方块，执行备用方案
- **结果：** 灵魂生成在悬崖下合理位置

### 场景3：在安全区域死亡
- 玩家在正常地面战斗时死亡
- 最后安全位置：死亡前站立位置
- 距离不超过20方块
- **结果：** 灵魂生成在最后安全位置 + 1.2格浮升

---

## 死亡时的完整逻辑（第817-830行）

```java
Location soulLocation = null;
try {
    if (smartSoulPlacement) {
        // 方案1：使用已记录的安全位置
        PlayerSoulInfo info = watchedPlayers.get(player);
        if (info == null) {
            // 玩家未被监视（未登录/刚加入），创建新信息
            info = new PlayerSoulInfo();
            watchedPlayers.put(player, info);
        }
        soulLocation = info.findSafeSoulSpawnLocation(player);
        info.lastSafeLocation.setWorld(null);  // 重置，防止重复使用
    } else {
        // 方案2：使用备用方案（smart disabled）
        soulLocation = PlayerSoulInfo.findFallbackSoulSpawnLocation(
            player, 
            player.getLocation(), 
            false
        );
    }
} catch (Exception bugException) {
    // 异常处理：默认使用死亡位置
    getLogger().log(Level.SEVERE, 
        "Failed to find soul location, defaulting to player location!", 
        bugException);
}

if (soulLocation == null) {
    soulLocation = player.getLocation();  // 最后保险方案
}
```

---

## 关键参数总结

| 参数 | 值 | 说明 |
|------|-----|------|
| SOUL_HOVER_OFFSET | 1.2 | 灵魂相对地面的浮升高度 |
| 安全位置更新频率 | 实时 | 玩家每次满足条件时更新 |
| 距离判断阈值 | 20 方块 | 最后安全位置与当前位置的最大距离 |
| 熔岩扫描深度 | 8 格 | 熔岩柱搜索的最大深度 |

---

## 配置相关

### config.yml 第21行
```yaml
smart-soul-placement: true
```

**启用时：** 使用上述复杂的安全位置逻辑
**禁用时：** 调用 `findFallbackSoulSpawnLocation(..., false)` 直接在死亡位置生成


