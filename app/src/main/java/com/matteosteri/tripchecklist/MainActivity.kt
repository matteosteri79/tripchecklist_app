package com.matteosteri.tripchecklist

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.matteosteri.tripchecklist.theme.ChecklistTheme
import androidx.compose.ui.platform.LocalContext
import com.matteosteri.tripchecklist.data.preset.PresetIconMapper
import com.matteosteri.tripchecklist.data.preset.getVisiblePresets
import com.matteosteri.tripchecklist.theme.AppTheme
import com.matteosteri.tripchecklist.theme.ThemeManager
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.matteosteri.tripchecklist.config.AppConfig
import com.matteosteri.tripchecklist.data.database.CategoryEntity
import com.matteosteri.tripchecklist.data.database.ChecklistEntity
import com.matteosteri.tripchecklist.theme.OswaldFontFamily
import com.matteosteri.tripchecklist.utils.LanguageManager
import com.matteosteri.tripchecklist.viewmodel.ChecklistViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ChecklistTheme {
                ChecklistApp()
            }
        }
    }
}

/* ---------------- NAVIGATION ---------------- */

@Composable
fun ChecklistApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("checklist/{id}/{name}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLong() ?: 0L
            val name = backStackEntry.arguments?.getString("name")?.let { Uri.decode(it) } ?: ""
            ChecklistDetailScreen(id, name, navController)
        }
    }
}

fun NavController.goToChecklist(id: Long, name: String) {
    navigate("checklist/$id/${Uri.encode(name)}")
}
fun Context.findActivity(): ComponentActivity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }

        currentContext = currentContext.baseContext
    }

    return null
}

/* ---------------- HOME ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ChecklistViewModel = viewModel()
) {
    val checklists by viewModel.checklists.collectAsState()
    var name by remember { mutableStateOf("") }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentLanguage = LanguageManager.getLanguage(context)
    val presets = remember(currentLanguage) { getVisiblePresets(context) }
    var showPresetSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { fabMenuExpanded = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, null)
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.load_checklist)) },
                        leadingIcon = {
                            Icon(Icons.Default.Upload, contentDescription = null)
                        },
                        onClick = {
                            fabMenuExpanded = false
                            showPresetSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.create_checklist)) },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        },
                        onClick = {
                            fabMenuExpanded = false
                            showDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AppHeader(
                onDonate = { showDonateDialog = true }
            )

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (checklists.isEmpty()) {
                    EmptyState()
                } else {
                    ChecklistList(
                        checklists = checklists,
                        onDelete = { viewModel.deleteChecklist(it) },
                        onClick = { navController.goToChecklist(it.id, it.name) },
                        onDonate = { showDonateDialog = true }
                    )
                }
            }
        }
    }

    if (showDialog) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.new_checklist)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.checklist_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {

                        viewModel.addChecklist(name, null) { id ->
                            val checklistName = name
                            name = ""
                            showDialog = false

                            navController.goToChecklist(id, checklistName)
                        }
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showPresetDialog) {

        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.choose_checklist)) },
            text = {

                LazyColumn {

                    items(presets) { preset ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {

                                    viewModel.importPreset(preset) { id ->
                                        showPresetDialog = false
                                        navController.goToChecklist(id, preset.name)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // 👉 ICONA (placeholder per ora)
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showPresetSheet) {

        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.choose_checklist),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn {
                    items(presets) { preset ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    viewModel.importPreset(preset) { id ->
                                        showPresetSheet = false
                                        navController.goToChecklist(id, preset.name)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = PresetIconMapper.getIcon(preset.icon),
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // DIALOG DONATE
    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            icon = { Text(text = "☕", fontSize = 32.sp) },
            title = {
                Text(text = stringResource(R.string.donate_coffee_title))
            },
            text = {
                Text(text = stringResource(R.string.donate_coffee_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDonateDialog = false

                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://ko-fi.com/appchecklist")
                        )

                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.donate_coffee_button)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDonateDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel)
                    )
                }
            }
        )
    }
}


/* ---------------- EMPTY ---------------- */

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(220.dp)
        )
        Text(
            text = stringResource(R.string.app_tagline),
            fontFamily = OswaldFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.welcome_message))
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.empty_home_message))
    }
}

/* ---------------- LIST ---------------- */
@Composable
fun ChecklistList(
    checklists: List<ChecklistEntity>,
    onDelete: (ChecklistEntity) -> Unit,
    onClick: (ChecklistEntity) -> Unit,
    onDonate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.your_checklists),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(checklists) { c ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onClick(c) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PresetIconMapper.getIcon(c.icon),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        IconButton(onClick = { onDelete(c) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }

        // Banner "Dona un caffè"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 16.dp,
                    bottom = 100.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onDonate() }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "☕",
                    fontSize = 40.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.donate_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.donate_banner_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------------- DETAIL ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    checklistId: Long,
    checklistName: String,
    navController: NavController,
    viewModel: ChecklistViewModel = viewModel()
) {
    val items by viewModel.getItemsByChecklist(checklistId)
        .collectAsState(initial = emptyList())
    val categories by viewModel.getCategoriesByChecklist(checklistId)
        .collectAsState(initial = emptyList())
    val grouped = items.groupBy { it.categoryId }
    val total = items.size
    val checked = items.count { it.isChecked }
    val progress = if (total == 0) 0f else checked.toFloat() / total
    val expandedMap = remember { mutableStateMapOf<Long, Boolean>() }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    var showItemDialog by remember { mutableStateOf(false) }
    var itemText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(checklistName) }

    Scaffold(
        topBar = {
            Column {
                AppHeader(
                    onDonate = {
                        // ...
                    }
                )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp),

                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    FloatingActionButton(
                        onClick = { fabMenuExpanded = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_category)) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                            },
                            onClick = {
                                fabMenuExpanded = false
                                showCategoryDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename_checklist)) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            onClick = {
                                fabMenuExpanded = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reset_checklist)) },
                            leadingIcon = {
                                Icon(Icons.Default.RestartAlt, contentDescription = null)
                            },
                            onClick = {
                                fabMenuExpanded = false
                                viewModel.resetChecklist(checklistId)
                            }
                        )
                    }
                }
            }
        }

    ) { padding ->

        Column(Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.selected_items_count, checked, total))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${(progress * 100).toInt()}%")

                        if (progress == 1f && total > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = stringResource(R.string.completed),
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

            }
            Spacer(Modifier.height(12.dp))
            if (categories.isEmpty()) {
                EmptyCategoriesState(
                    onCreateClick = { showCategoryDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        bottom = padding.calculateBottomPadding() + 60.dp
                    )
                ) {
                    categories.forEach { category ->
                        val itemsInCat = grouped[category.id] ?: emptyList()
                        val expanded = expandedMap[category.id] ?: true
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = PresetIconMapper.getIcon(category.icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                category.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                expandedMap[category.id] = !expanded
                                            }) {
                                                Icon(
                                                    if (expanded)
                                                        Icons.Default.KeyboardArrowUp
                                                    else
                                                        Icons.Default.KeyboardArrowDown,
                                                    null
                                                )
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Row {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        selectedCategory = category
                                                        showItemDialog = true
                                                    }
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        viewModel.deleteCategorySmart(category)
                                                        selectedCategory = null
                                                    }
                                            )
                                        }
                                    }

                                    if (expanded) {

                                        itemsInCat.forEach { item ->

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 1.dp)
                                                    .background(MaterialTheme.colorScheme.surface),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = item.isChecked,
                                                    onCheckedChange = {
                                                        viewModel.toggleItem(item)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                Text(
                                                    item.name,
                                                    modifier = Modifier.weight(1f),
                                                    style = if (item.isChecked)
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                            textDecoration = TextDecoration.LineThrough,
                                                            color = Color.Gray
                                                        )
                                                    else MaterialTheme.typography.bodyLarge
                                                )
                                                IconButton(onClick = {
                                                    viewModel.deleteItemSmart(item)
                                                }) {
                                                    Icon(Icons.Default.Delete, null)
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
    }

    /* ---------------- CATEGORY DIALOG ---------------- */

    if (showCategoryDialog) {

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(stringResource(R.string.new_category)) },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (categoryName.isNotBlank()) {
                        viewModel.addCategory(checklistId, categoryName)
                        categoryName = ""
                        showCategoryDialog = false
                    }
                }) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    /* ---------------- ITEM DIALOG ---------------- */

    if (showItemDialog && selectedCategory != null) {

        AlertDialog(
            onDismissRequest = { showItemDialog = false },
            title = { Text(stringResource(R.string.new_item)) },
            text = {
                OutlinedTextField(
                    value = itemText,
                    onValueChange = { itemText = it },
                    label = { Text(stringResource(R.string.name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (itemText.isNotBlank()) {
                        val validCategory = categories.find { it.id == selectedCategory?.id }
                        if (itemText.isNotBlank() && validCategory != null) {
                            viewModel.addItem(
                                checklistId,
                                itemText,
                                selectedCategory!!.id
                            )
                        }

                        itemText = ""
                        showItemDialog = false
                    }
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showItemDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    /* ---------------- RENAME CHECKLIST DIALOG ---------------- */

    if (showRenameDialog) {

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_checklist)) },
            text = {

                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.checklist_name)) }
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        if (renameText.isNotBlank()) {

                            viewModel.renameChecklist(
                                checklistId,
                                renameText
                            )

                            showRenameDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showRenameDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}



/* ---------------- EMPTY CATEGORY ---------------- */

@Composable
fun EmptyCategoriesState(onCreateClick: () -> Unit) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(stringResource(R.string.no_categories))

        Spacer(Modifier.height(12.dp))

        Button(onClick = onCreateClick) {
            Text(stringResource(R.string.create_category))
        }
    }
}


/* ---------------- HEADER APP ---------------- */
@Composable
fun AppHeader(
    onDonate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val versionName = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName ?: ""
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // LOGO
        Image(
            painter = painterResource(R.drawable.header),
            contentDescription = stringResource(R.string.header),
            modifier = Modifier
                .height(60.dp)
                .align(Alignment.CenterStart)
        )

        // MENU ⋮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
        ) {

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.language)) },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        showLanguageDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.theme)) },
                    leadingIcon = {
                        Icon(Icons.Default.Palette, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        showThemeDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.donate_coffee)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDonate()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info)) },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        showInfoDialog = true
                    }
                )
            }
        }
    }

    // DIALOG LANGUAGE
    if (showLanguageDialog) {
        val currentLanguage = LanguageManager.getLanguage(context)
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.choose_language)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_SYSTEM
                            )
                            showLanguageDialog = false

                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_system),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource((R.string.system_language)),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_SYSTEM) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_ITALIAN
                            )
                            showLanguageDialog = false

                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_it),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource((R.string.italian)),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_ITALIAN) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_ENGLISH
                            )
                            showLanguageDialog = false
                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_gb),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.english),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_ENGLISH) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_FRENCH
                            )
                            showLanguageDialog = false

                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_fr),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.french),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_FRENCH) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_SPANISH
                            )
                            showLanguageDialog = false

                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_es),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.spanish),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_SPANISH) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            LanguageManager.setLanguage(
                                context,
                                LanguageManager.LANGUAGE_GERMAN
                            )
                            showLanguageDialog = false

                            val activity = context.findActivity()
                            activity?.finish()
                            activity?.startActivity(
                                Intent(activity, MainActivity::class.java)
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.flag_de),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.german),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentLanguage == LanguageManager.LANGUAGE_GERMAN) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // DIALOG THEME
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(stringResource(R.string.choose_theme))
            },
            text = {
                Column {
                    AppTheme.entries.forEach { theme ->
                        TextButton(
                            onClick = {
                                ThemeManager.setTheme(
                                    context,
                                    theme
                                )
                                showThemeDialog = false
                                val activity = context.findActivity()
                                activity?.finish()
                                activity?.startActivity(
                                    Intent(activity, MainActivity::class.java)
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val previewColor = when (theme) {
                                    AppTheme.GREEN -> Color(0xFF279861)
                                    AppTheme.BLUE -> Color(0xFF1565C0)
                                    AppTheme.ORANGE -> Color(0xFFEF6C00)
                                    AppTheme.RED -> Color(0xFFC62828)
                                    AppTheme.PURPLE -> Color(0xFF6A1B9A)
                                }
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = previewColor
                                )
                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )
                                Text(
                                    text = theme.displayName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (ThemeManager.getTheme(context) == theme) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showThemeDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // DIALOG INFO
    if (showInfoDialog) {

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            title = { Text(stringResource(R.string.info_app)) },
            text = {

                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        fontFamily = OswaldFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.version_label, versionName),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.app_description))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.contact_support))
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${AppConfig.SUPPORT_EMAIL}")
                            }
                            context.startActivity(intent)
                        }
                    ) {

                        Icon(Icons.Default.Email, contentDescription = null)

                        Spacer(Modifier.width(8.dp))

                        Text(
                            stringResource(R.string.write_us),
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        )
    }


}
