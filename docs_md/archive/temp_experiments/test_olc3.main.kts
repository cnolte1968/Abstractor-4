@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.openlocationcode:openlocationcode:1.0.4")

import com.google.openlocationcode.OpenLocationCode

val fullCode = "8FW4V75V+8Q"
val olc = OpenLocationCode(fullCode)
val decoded = olc.decode()
println("${decoded.centerLatitude}, ${decoded.centerLongitude}")
