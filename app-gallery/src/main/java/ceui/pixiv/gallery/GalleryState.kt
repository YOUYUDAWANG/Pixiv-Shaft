package ceui.pixiv.gallery

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** Local visual fixtures only. IDs link to the original work; categories are sample labels. */
data class Artwork(val id: String, val resource: Int, val category: String, val ratio: Float)

class GalleryState(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("gallery-preview", 0)
    val artworks = listOf(
        Triple("56099861", R.drawable.art_56099861, "初音"),
        Triple("68296699", R.drawable.art_68296699, "风景"),
        Triple("68698295", R.drawable.art_68698295, "人物"),
        Triple("51678256", R.drawable.art_51678256, "风景"),
        Triple("60095408", R.drawable.art_60095408, "初音"),
        Triple("73355141", R.drawable.art_73355141, "风景"),
        Triple("63120410", R.drawable.art_63120410, "人物"),
        Triple("35470184", R.drawable.art_35470184, "风景"),
        Triple("84026087", R.drawable.art_84026087, "初音"),
        Triple("56884826", R.drawable.art_56884826, "风景"),
        Triple("62258773", R.drawable.art_62258773, "人物"),
        Triple("47621790", R.drawable.art_47621790, "风景"),
    ).map { (id, resource, category) ->
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(application.resources, resource, bounds)
        Artwork(id, resource, category, bounds.outWidth.toFloat() / bounds.outHeight.coerceAtLeast(1))
    }
    var saved by mutableStateOf(preferences.getStringSet("saved", emptySet())!!.toSet())
        private set
    fun toggle(id: String) {
        saved = if (id in saved) saved - id else saved + id
        preferences.edit().putStringSet("saved", saved).apply()
    }
}
