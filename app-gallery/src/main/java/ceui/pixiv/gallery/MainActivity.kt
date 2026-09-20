package ceui.pixiv.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GalleryApp() }
    }
}

private val GalleryFont = FontFamily(
    Font(ceui.pixiv.witstudio.R.font.montserrat_regular, FontWeight.Normal),
    Font(ceui.pixiv.witstudio.R.font.montserrat_semi_bold, FontWeight.SemiBold),
)

@Composable
private fun GalleryApp(state: GalleryState = viewModel()) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var destination by rememberSaveable { mutableStateOf("发现") }
    var category by rememberSaveable { mutableStateOf("全部") }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var theme by rememberSaveable { mutableIntStateOf(0) }
    val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFE9E5ED), background = Color(0xFF151416), surface = Color(0xFF151416),
        surfaceContainer = Color(0xFF252329), onBackground = Color(0xFFF3EFF5),
        onSurface = Color(0xFFF3EFF5), onSurfaceVariant = Color(0xFFBDB7C5)
    ) else lightColorScheme(
        primary = Color(0xFF262329), background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFEEEBE6), onBackground = Color(0xFF25222B),
        onSurface = Color(0xFF25222B), onSurfaceVariant = Color(0xFF706A77)
    )
    val activity = LocalContext.current as ComponentActivity
    SideEffect {
        val barStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
    }
    val grid = rememberLazyStaggeredGridState()
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            val selected = state.artworks.find { it.id == selectedId }
            if (selected != null) {
                ArtworkDetail(selected, selected.id in state.saved, { state.toggle(selected.id) }, { selectedId = null })
            } else {
                BackHandler(searching) { searching = false; query = "" }
                Scaffold(
                    containerColor = colors.background,
                    topBar = {
                        Column(Modifier.statusBarsPadding()) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    items(listOf("全部", "风景", "人物", "初音")) { label ->
                                        Column(Modifier.clickable { category = label }.padding(top = 16.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(label, fontSize = 16.sp, fontWeight = if (category == label) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (category == label) colors.onSurface else colors.onSurfaceVariant)
                                            Spacer(Modifier.height(7.dp))
                                            Box(Modifier.width(24.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (category == label) colors.primary else Color.Transparent))
                                        }
                                    }
                                }
                                ToolButton(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "切换深浅主题") { theme = if (dark) 1 else 2 }
                            }
                            if (searching) {
                                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 10.dp),
                                    singleLine = true, placeholder = { Text("搜索示例作品编号或分类") }, shape = RoundedCornerShape(24.dp),
                                    trailingIcon = { ToolButton(Icons.Outlined.Close, "关闭搜索") { searching = false; query = "" } })
                            }
                        }
                    },
                    bottomBar = {
                        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp, top = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(shape = RoundedCornerShape(28.dp), color = colors.surface, shadowElevation = 8.dp, tonalElevation = 0.dp) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    ToolButton(Icons.Outlined.Home, "发现", destination == "发现" && !searching) { destination = "发现"; searching = false; query = "" }
                                    ToolButton(Icons.Outlined.Search, "搜索示例作品", searching) { searching = !searching; if (!searching) query = "" }
                                    ToolButton(if (destination == "本地收藏") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "本地收藏", destination == "本地收藏") { destination = "本地收藏"; searching = false; query = "" }
                                }
                            }
                        }
                    }
                ) { padding ->
                    val visible = state.artworks.filter {
                        (destination != "本地收藏" || it.id in state.saved) &&
                            (category == "全部" || category == it.category) &&
                            (query.isBlank() || it.id.contains(query.trim()) || it.category.contains(query.trim()))
                    }
                    if (visible.isEmpty()) {
                        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.Image, null, Modifier.size(40.dp), tint = colors.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text(if (destination == "本地收藏") "还没有符合条件的收藏" else "没有找到作品")
                            TextButton(onClick = { category = "全部"; query = ""; destination = "发现" }) { Text("浏览全部") }
                        }
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(if (LocalConfiguration.current.screenWidthDp < 400) 140.dp else 200.dp), state = grid,
                            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = padding.calculateBottomPadding() + 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalItemSpacing = 6.dp
                        ) {
                            items(visible, key = { it.id }) { art ->
                                var menuOpen by remember { mutableStateOf(false) }
                                Box {
                                    Image(painterResource(art.resource), "作品 ${art.id}", contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(art.ratio).clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedId = art.id })
                                    if (art.id in state.saved) {
                                        Icon(Icons.Filled.Favorite, "已存到本地", Modifier.align(Alignment.TopEnd).padding(12.dp).size(20.dp), tint = Color.White)
                                    }
                                    Box(Modifier.align(Alignment.BottomEnd)) {
                                        IconButton(onClick = { menuOpen = true }) {
                                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.MoreHoriz, "作品 ${art.id} 的操作", Modifier.size(20.dp), tint = Color(0xFF262329))
                                            }
                                        }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            DropdownMenuItem(text = { Text(if (art.id in state.saved) "取消本地收藏" else "本地收藏") }, onClick = { state.toggle(art.id); menuOpen = false })
                                            DropdownMenuItem(text = { Text("查看作品") }, onClick = { selectedId = art.id; menuOpen = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, active: Boolean = false, action: () -> Unit) {
    IconButton(onClick = action) {
        Icon(icon, label, Modifier.size(22.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtworkDetail(art: Artwork, saved: Boolean, toggle: () -> Unit, close: () -> Unit) {
    var showInfo by rememberSaveable(art.id) { mutableStateOf(false) }
    var scale by remember(art.id) { mutableFloatStateOf(1f) }
    var offset by remember(art.id) { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    BackHandler {
        when { showInfo -> showInfo = false; scale > 1f -> { scale = 1f; offset = Offset.Zero }; else -> close() }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolButton(Icons.AutoMirrored.Outlined.ArrowBack, "返回作品列表") { close() }
            Text("作品 ${art.id}", Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToolButton(Icons.Outlined.Info, "作品信息") { showInfo = true }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()
            .pointerInput(art.id) {
                detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero })
            }
            .pointerInput(art.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val limitX = size.width * (scale - 1f) / 2f
                    val limitY = size.height * (scale - 1f) / 2f
                    offset = if (scale == 1f) Offset.Zero else Offset(
                        (offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
                }
            }, contentAlignment = Alignment.Center) {
            Image(painterResource(art.resource), "作品 ${art.id}，双击或双指缩放", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showInfo = true }) { Text("作品信息"); Icon(Icons.Outlined.ExpandLess, null) }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = toggle) {
                Icon(if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text(if (saved) "已存到本地" else "本地收藏")
            }
        }
    }
    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("作品 ${art.id}", style = MaterialTheme.typography.headlineSmall)
                Text("这是本地视觉样板，使用原比例预览图。作者与标题以 Pixiv 原作页面为准，收藏仅保存在本机。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pixiv.net/artworks/${art.id}"))) }) {
                    Text("查看 Pixiv 原作"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
                }
            }
        }
    }
}
