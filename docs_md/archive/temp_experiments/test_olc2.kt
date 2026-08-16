import com.google.openlocationcode.OpenLocationCode

fun main() {
    val methods = OpenLocationCode::class.java.methods.map { it.name }
    println(methods.joinToString(", "))
}
