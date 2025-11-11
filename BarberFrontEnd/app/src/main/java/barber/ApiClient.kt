package barber

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE = "http://10.0.2.2:8080/api"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    @JvmStatic
    @Throws(IOException::class)
    fun postJson(path: String, json: JSONObject): JSONObject {
        val body = json.toString().toRequestBody(JSON)
        val req = Request.Builder().url("$BASE$path").post(body).build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return if (t.isNotEmpty()) JSONObject(t) else JSONObject()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun postAuth(path: String, json: JSONObject, token: String): JSONObject {
        val body = json.toString().toRequestBody(JSON)
        val req = Request.Builder().url("$BASE$path")
            .addHeader("Authorization", "Bearer $token").post(body).build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return if (t.isNotEmpty()) JSONObject(t) else JSONObject()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun putAuth(path: String, json: JSONObject, token: String): JSONObject {
        val body = json.toString().toRequestBody(JSON)
        val req = Request.Builder().url("$BASE$path")
            .addHeader("Authorization", "Bearer $token").put(body).build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return if (t.isNotEmpty()) JSONObject(t) else JSONObject()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun deleteAuth(path: String, token: String): JSONObject {
        val req = Request.Builder().url("$BASE$path")
            .addHeader("Authorization", "Bearer $token").delete().build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return if (t.isNotEmpty()) JSONObject(t) else JSONObject()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getArray(path: String): JSONArray {
        val req = Request.Builder().url("$BASE$path").get().build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return JSONArray(t)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getArrayAuth(path: String, token: String): JSONArray {
        val req = Request.Builder().url("$BASE$path")
            .addHeader("Authorization", "Bearer $token").get().build()
        client.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: $t")
            return JSONArray(t)
        }
    }
}
