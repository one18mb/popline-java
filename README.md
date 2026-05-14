# PopLine Java

PopLine 序列化格式的 Java 实现。

## Maven

```xml
<dependency>
    <groupId>com.popline</groupId>
    <artifactId>popline-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 使用

```java
import com.popline.*;

// 解析
PlnValue v = Pln.parse("{\nkey: \"value\"\n");

// 序列化
String text = Pln.stringify(v);

// 构建 DOM
PlnValue obj = PlnValue.newObject();
obj.addToObject("name", PlnValue.newString("test"));
obj.addToObject("count", PlnValue.newInt(42));
```

## 性能

测试数据：`test.json`（17011 B）→ `test.pln`（13074 B，**76.9%**），5000 次迭代

| 操作 | org.json | popline | 比 |
|------|---------|---------|------|
| 解析 | 1941 ms (388 µs/op) | 1861 ms (372 µs/op) | **0.96x** |
| 序列化 | 2028 ms (405 µs/op) | 857 ms (171 µs/op) | **0.42x** |

## 构建

```bash
mvn test
mvn package
```

## 致谢
本项目的开发得到了以下 AI 工具的大力协助：
- [Claude Code](https://claude.ai)（Anthropic）
- [DeepSeek](https://deepseek.com)（深度求索）
