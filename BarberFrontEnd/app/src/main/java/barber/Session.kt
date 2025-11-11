package barber

import android.content.Context
import org.json.JSONObject

object Session {
    private const val PREF = "app"

    @JvmStatic
    fun getToken(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("token", "") ?: ""

    @JvmStatic
    fun saveToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("token", token).apply()
    }

    @JvmStatic
    fun getRole(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("role", "CUSTOMER") ?: "CUSTOMER"

    @JvmStatic
    fun saveRole(ctx: Context, role: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("role", role).apply()
    }

    @JvmStatic
    fun applyLoginResponse(ctx: Context, res: JSONObject) {
        val token = res.optString("token", "")
        if (token.isNotEmpty()) saveToken(ctx, token)

        // lấy role từ user.role hoặc root.role
        val role = res.optJSONObject("user")?.optString("role")
            ?: res.optString("role", getRole(ctx))
        if (!role.isNullOrEmpty()) saveRole(ctx, role)
    }

    @JvmStatic
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
