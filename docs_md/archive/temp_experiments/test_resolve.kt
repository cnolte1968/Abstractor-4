import okhttp3.Request
import okhttp3.OkHttpClient
import java.net.URI

fun main() {
    val client = OkHttpClient.Builder().followRedirects(false).build()
    val url = "https://maps.app.goo.gl/Dmv1wmRazyu1hyacA"
    var currentUrl = url
    for (i in 1..5) {
        val request = Request.Builder().url(currentUrl).head().build()
        client.newCall(request).execute().use { response ->
            println("Status: ${response.code}")
            if (response.isRedirect) {
                val loc = response.header("Location")
                println("Location: $loc")
                currentUrl = loc ?: break
            } else {
                break
            }
        }
    }
}
