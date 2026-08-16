import com.google.openlocationcode.OpenLocationCode

fun main() {
    val fullCode = "8FW4V75V+8Q"
    val olc = OpenLocationCode(fullCode)
    val decoded = olc.decode()
    println("${decoded.centerLatitude}, ${decoded.centerLongitude}")
}
