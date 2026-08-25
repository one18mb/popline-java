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
PlnValue v = Pln.parse("{\nkey: \"value\"\n");

// Serialize
String text = Pln.stringify(v);

// Build DOM
PlnValue obj = PlnValue.newObject();
obj.addToObject("name", PlnValue.newString("test"));
obj.addToObject("count", PlnValue.newInt(42));
```

## Performance

Data: `test.json` (17011 B) / `test.pln` (13074 B, 76.9%)

| Operation | org.json | popline | Ratio |
|-----------|---------|---------|-------|
| Parse | 1941 ms (388 µs/op) | 1861 ms (372 µs/op) | **0.96x** |
| Serialize | 2028 ms (405 µs/op) | 857 ms (171 µs/op) | **0.42x** |

## Build

```bash
mvn test
mvn package
```

## Acknowledgments
This project was developed with the assistance of:
- [DeepSeek](https://deepseek.com) (DeepSeek)
