package com.splash.offline.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

object SplashStrings {
    fun t(lang: AppLanguage, es: String, en: String): String =
        if (lang == AppLanguage.EN) en else es

    fun streakLabel(lang: AppLanguage, days: Int): String =
        if (lang == AppLanguage.EN) {
            if (days == 1) "1 day" else "$days days"
        } else {
            if (days == 1) "1 día" else "$days días"
        }

    fun areaFor(lang: AppLanguage, esArea: String): String {
        if (lang == AppLanguage.ES) return esArea
        return when (esArea) {
            "Cuerpo y energía" -> "Body & Energy"
            "Calma mental" -> "Mental Calm"
            "Conexión social" -> "Social Connection"
            "Orden y entorno" -> "Order & Space"
            else -> esArea
        }
    }

    fun iconFor(name: String): ImageVector = when (name.lowercase()) {
        "visibility" -> Icons.Rounded.Visibility
        "accessibility_new" -> Icons.Rounded.AccessibilityNew
        "water_drop" -> Icons.Rounded.WaterDrop
        "fitness_center" -> Icons.Rounded.FitnessCenter
        "air" -> Icons.Rounded.Air
        "directions_walk" -> Icons.Rounded.DirectionsWalk
        "wb_sunny", "light_mode" -> Icons.Rounded.WbSunny
        "self_improvement" -> Icons.Rounded.SelfImprovement
        "spa" -> Icons.Rounded.Spa
        "menu_book", "book" -> Icons.Rounded.MenuBook
        "edit_note" -> Icons.Rounded.EditNote
        "coffee", "local_cafe" -> Icons.Rounded.Coffee
        "park", "forest" -> Icons.Rounded.Park
        "local_florist", "eco" -> Icons.Rounded.LocalFlorist
        "hiking" -> Icons.Rounded.Hiking
        "shower" -> Icons.Rounded.Shower
        "restaurant" -> Icons.Rounded.Restaurant
        "groups", "group" -> Icons.Rounded.Groups
        "forum", "chat" -> Icons.Rounded.Forum
        "music_note" -> Icons.Rounded.MusicNote
        "bedtime", "nights_stay" -> Icons.Rounded.Bedtime
        "home", "cleaning_services" -> Icons.Rounded.Home
        "photo_camera" -> Icons.Rounded.PhotoCamera
        "landscape", "terrain" -> Icons.Rounded.Landscape
        "bolt" -> Icons.Rounded.Bolt
        "check_circle" -> Icons.Rounded.CheckCircle
        "auto_awesome" -> Icons.Rounded.AutoAwesome
        else -> Icons.Rounded.Eco
    }
}
