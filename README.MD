# VaycoreAPI
The central API of the Vaycore Network

### Installation:
1. Add the vaycore maven repo:
```kotlin
repositories {
    // ...
    maven("https://mvn.c4vxl.de/vaycore/")
}
```

2. Add the Vaycore-API implementation:
```kotlin
dependencies {
    // ...
    
    // Vaycore API
    implementation("de.c4vxl:vaycore-api:${version}")
}
```

3. Install the VaycoreAPI plugin from the [latest release](https://github.com/Vaycore-Network/VaycoreAPI/releases/latest) and place it into your servers `plugins` folder