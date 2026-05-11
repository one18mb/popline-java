# PopLine Java

Java implementation of the PopLine serialization format.

## Maven

```xml
<dependency>
    <groupId>com.popline</groupId>
    <artifactId>popline-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

```java
import com.popline.*;

// Parse
PlnValue v = PopLineParser.parse("{\nkey: \"value\"\n");

// Serialize
String text = PopLineSerializer.serialize(v);

// Build DOM
PlnValue obj = PlnValue.newObject();
obj.addToObject("name", PlnValue.newString("test"));
obj.addToObject("count", PlnValue.newInt(42));
```

## Performance

Data: `package.json` (17011 B) / `package.pln` (13074 B, 76.9%)

| Operation | org.json | popline | Ratio |
|-----------|---------|---------|-------|
| Parse | 1437 ms | 1402 ms | **0.98x** |
| Serialize | 1733 ms | 703 ms | **0.41x** |

## Build

```bash
mvn test
mvn package
```
