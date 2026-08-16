@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.google.openlocationcode:openlocationcode:1.0.4")
import com.google.openlocationcode.OpenLocationCode
val olc = OpenLocationCode("8FW4V75V+8Q")
println(olc.decode().centerLatitude)
println(olc.decode().centerLongitude)
