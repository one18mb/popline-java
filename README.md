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

测试数据：`package.json`（17011 B）→ `package.pln`（13074 B，**76.9%**），5000 次迭代

| 操作 | org.json | popline | 比 |
|------|---------|---------|------|
| 解析 | 1437 ms (287 µs/op) | 1402 ms (280 µs/op) | **0.98x** |
| 序列化 | 1733 ms (347 µs/op) | 703 ms (141 µs/op) | **0.41x** |

## 构建

```bash
mvn test
mvn package
```
