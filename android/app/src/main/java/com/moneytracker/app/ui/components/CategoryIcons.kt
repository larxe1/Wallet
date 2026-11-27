package com.moneytracker.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moneytracker.app.ui.theme.ObsidianColors
import java.util.Locale

data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

val CATEGORY_ICON_OPTIONS: List<CategoryIconOption> = listOf(
    CategoryIconOption("home", "Home", Icons.Rounded.Home),
    CategoryIconOption("grocery", "Grocery", Icons.Rounded.ShoppingCart),
    CategoryIconOption("restaurant", "Dining", Icons.Rounded.Restaurant),
    CategoryIconOption("bills", "Bills", Icons.Rounded.ReceiptLong),
    CategoryIconOption("subscription", "Recurring", Icons.Rounded.Repeat),
    CategoryIconOption("transit", "Transit", Icons.Rounded.DirectionsBus),
    CategoryIconOption("car", "Auto", Icons.Rounded.DirectionsCar),
    CategoryIconOption("work", "Work", Icons.Rounded.WorkOutline),
    CategoryIconOption("household", "Household", Icons.Rounded.CleaningServices),
    CategoryIconOption("tech", "Tech", Icons.Rounded.LaptopMac),
    CategoryIconOption("health", "Health", Icons.Rounded.LocalHospital),
    CategoryIconOption("service", "Service", Icons.Rounded.Build),
    CategoryIconOption("clothing", "Clothing", Icons.Rounded.Checkroom),
    CategoryIconOption("utilities", "Utilities", Icons.Rounded.Bolt),
    CategoryIconOption("games", "Games", Icons.Rounded.SportsEsports),
    CategoryIconOption("entertainment", "Media", Icons.Rounded.Movie),
    CategoryIconOption("fitness", "Fitness", Icons.Rounded.FitnessCenter),
    CategoryIconOption("savings", "Savings", Icons.Rounded.AccountBalanceWallet),
    CategoryIconOption("card", "Card", Icons.Rounded.CreditCard),
    CategoryIconOption("gift", "Gift", Icons.Rounded.Redeem),
    CategoryIconOption("travel", "Travel", Icons.Rounded.Flight),
    CategoryIconOption("education", "Study", Icons.Rounded.MenuBook),
    CategoryIconOption("pets", "Pets", Icons.Rounded.Pets),
    CategoryIconOption("tag", "General", Icons.Rounded.Label)
)

private val ICON_BY_KEY: Map<String, ImageVector> = CATEGORY_ICON_OPTIONS.associate { it.key to it.icon }

// Maps legacy Unicode emoji strings saved in existing Supabase rows to crisp Material icons.
private val LEGACY_CODEPOINT_TO_KEY: Map<Int, String> = mapOf(
    0x1F3E0 to "home",
    0x1F354 to "restaurant",
    0x1F6D2 to "grocery",
    0x1F504 to "subscription",
    0x1F68C to "transit",
    0x1F4BC to "work",
    0x1F9FE to "bills",
    0x1F9FB to "household",
    0x1F4BB to "tech",
    0x1F3E5 to "health",
    0x1F6E0 to "service",
    0x1F455 to "clothing",
    0x1F697 to "car",
    0x26A1 to "utilities",
    0x1F3AE to "games",
    0x1F3AC to "entertainment",
    0x1F3CB to "fitness",
    0x1F4B0 to "savings",
    0x1F4B3 to "card",
    0x1F4E6 to "package",
    0x2764 to "favorite",
    0x1F381 to "gift",
    0x2708 to "travel",
    0x1F4DA to "education",
    0x1F454 to "work",
    0x1F43E to "pets",
    0x1F3F7 to "tag"
)

fun resolveCategoryIcon(categoryName: String?, storedIcon: String?): ImageVector {
    val rawKey = storedIcon?.trim()?.lowercase(Locale.US).orEmpty()
    ICON_BY_KEY[rawKey]?.let { return it }
    if (rawKey == "package") return Icons.Rounded.Inventory2
    if (rawKey == "favorite") return Icons.Rounded.FavoriteBorder

    if (!storedIcon.isNullOrBlank()) {
        val cp = storedIcon.trim().codePointAt(0)
        val mappedKey = LEGACY_CODEPOINT_TO_KEY[cp]
        if (mappedKey == "package") return Icons.Rounded.Inventory2
        if (mappedKey == "favorite") return Icons.Rounded.FavoriteBorder
        if (mappedKey != null) {
            ICON_BY_KEY[mappedKey]?.let { return it }
        }
    }

    val name = categoryName?.trim()?.lowercase(Locale.US).orEmpty()
    return when {
        "rent" in name || "home" in name || "house" in name -> Icons.Rounded.Home
        "groc" in name || "market" in name -> Icons.Rounded.ShoppingCart
        "restaur" in name || "food" in name || "din" in name || "coffee" in name -> Icons.Rounded.Restaurant
        "bill" in name || "tax" in name -> Icons.Rounded.ReceiptLong
        "sub" in name || "recur" in name -> Icons.Rounded.Repeat
        "trans" in name || "commute" in name || "bus" in name -> Icons.Rounded.DirectionsBus
        "car" in name || "gas" in name || "fuel" in name -> Icons.Rounded.DirectionsCar
        "work" in name || "school" in name || "office" in name -> Icons.Rounded.WorkOutline
        "household" in name || "clean" in name -> Icons.Rounded.CleaningServices
        "tech" in name || "electr" in name || "gadget" in name -> Icons.Rounded.LaptopMac
        "health" in name || "med" in name || "clinic" in name -> Icons.Rounded.LocalHospital
        "serv" in name || "repair" in name -> Icons.Rounded.Build
        "cloth" in name || "apparel" in name -> Icons.Rounded.Checkroom
        "util" in name || "power" in name || "electric" in name -> Icons.Rounded.Bolt
        "game" in name -> Icons.Rounded.SportsEsports
        "movie" in name || "entertain" in name -> Icons.Rounded.Movie
        "fit" in name || "gym" in name -> Icons.Rounded.FitnessCenter
        "sav" in name -> Icons.Rounded.AccountBalanceWallet
        "gift" in name -> Icons.Rounded.Redeem
        "travel" in name || "flight" in name -> Icons.Rounded.Flight
        "book" in name || "study" in name -> Icons.Rounded.MenuBook
        "pet" in name -> Icons.Rounded.Pets
        else -> Icons.Rounded.Label
    }
}

@Composable
fun CategoryIcon(
    categoryName: String?,
    storedIcon: String?,
    modifier: Modifier = Modifier,
    tint: Color = ObsidianColors.TextSecondary,
    size: Dp = 18.dp
) {
    Icon(
        imageVector = resolveCategoryIcon(categoryName = categoryName, storedIcon = storedIcon),
        contentDescription = null,
        tint = tint,
        modifier = if (size != Dp.Unspecified) modifier.size(size) else modifier
    )
}
