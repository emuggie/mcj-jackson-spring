# My Controversial Java - Jackson - Spring

## Contents
* [Overview](#overview)
* [Usage](#usage)

<a name="overview"></a>
## Overview

Using Jackson's custom JsonFilter(@JsonOnly), Apply Controller method based serialization rules.
Java version 8 required.

<a name="usage"></a>
## Usage

Register ControllerAdvice bean to Spring project.
```java

@Autowired
private ObjectMapper mapper;

@Bean
public JsonOnlyFilterAdvice jsonOnlyFilterAdvice() {
	return new JsonOnlyFilterAdvice(this.mapper);
}
```

Configured ObjectMapper will be used if single mapper bean exists.

```java
@Bean
public JsonOnlyFilterAdvice jsonOnlyFilterAdvice() {
	return new JsonOnlyFilterAdvice();
}
```

Annotate Controller Method with @JsonOnly.
```java
@JsonOnly({"field1","field2","field3"})
public Test someMethod(){
    return new Test();
}
```

If you look for details about JsonOnly filter, go to mcj-jackson.

