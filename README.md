# Amber

「ORDER REBUILD 秩序重构」辉光钟（设备名 NIXIE / BT24-T）的 Android 蓝牙控制 App。

用一部手机完成辉光钟的全部设置：时间校准、定时开关机、闹钟、背光颜色与亮度，不再依赖微信小程序。App
与设备之间保持长连接，断连后自动回连，随时打开都是设备的实时状态。

## 功能特性

### 时钟

- **校时**：连接成功后自动把设备时间同步为手机时间，也可手动触发；同步成功会记录最近一次同步时间
- **定时开关机**：设置每天自动开机 / 关机的时间
- **闹钟**：设置每日闹钟时间，独立开关
- **显示设置**：12 / 24 小时制切换、冒号闪烁开关
- **其他开关**：蜂鸣静音、锁定红外遥控器（防止误触）

### 灯光

- **逐管调色**：4 组灯管各自独立调节色相与饱和度（对比度），加全局亮度滑条
- **灯光模式**：光谱循环、呼吸灯、静态、跑马灯、彩虹灯
- **实时预览**：辉光管数字预览跟随当前配色，调完再发
- **预设管理**：把当前配色存入预设槽，点按应用、长按保存，本地持久化，换配色一键切换

### 设备与连接

- **扫描连接**：自动发现附近的辉光钟（设备名 `NIXIE` / `BT24-T`），连接后读取设备当前全量状态，App
  界面即为设备真实状态
- **设备管理面板**：记住连接过的设备，支持重命名、删除、直接连接、断开；可管理多台设备
- **自动回连**：链路意外断开后按 1s / 2s / 4s 退避重试，耗尽后转为低频续试，直到手动断开才停止；蓝牙开关机自动适配，不会在蓝牙关闭时空转

### 调试工具（设置页内）

- **帧日志**：蓝牙收 / 发 / 系统事件分色显示，排查通信问题一目了然
- **原始帧下发**：手动发送任意 14 字节帧，供协议实验与验证

## 技术栈

| 项    | 值                                                     |
|------|-------------------------------------------------------|
| 语言   | Kotlin 2.4.20（JVM 21）                                 |
| UI   | Jetpack Compose + Material 3                          |
| 架构   | 多模块 + MVI（feature / domain / data / core / shared 五层） |
| 导航   | Navigation 3                                          |
| 依赖注入 | Hilt                                                  |
| 蓝牙   | Nordic BLE Kotlin Client                              |
| 持久化  | Room（设备记忆、灯光预设）                                       |
| 最低系统 | Android 9（API 28）                                     |

## 构建

需要 JDK 21 与最新版 Android Studio。

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug

# 运行单元测试
gradlew.bat test
```

## 开源协议

    Copyright 2026 WangZhiYao
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
