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
PlnValue v = PopLineParser.parse("{\nkey: \"value\"\n");

// 序列化
String text = PopLineSerializer.serialize(v);

// 构建 DOM
PlnValue obj = PlnValue.newObject();
obj.addToObject("name", PlnValue.newString("test"));
obj.addToObject("count", PlnValue.newInt(42));
```

## 构建

```bash
mvn test
mvn package
```
