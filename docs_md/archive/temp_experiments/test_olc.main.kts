@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.openlocationcode:openlocationcode:1.0.4")

import com.google.openlocationcode.OpenLocationCode

try {
    val code = OpenLocationCode("QXV3+893")
    val decoded = code.decode()
    println("Decoded: \${decoded.centerLatitude}, \${decoded.centerLongitude}")
} catch (e: Exception) {
    println("Error: \${e.message}")
}
