package com.example.pomodorotimer.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Typography
import com.example.pomodorotimer.R

@OptIn(ExperimentalTextApi::class)
private fun flexFont(
    weight: Float,
    width: Float,
    opsz: Float,
    grade: Float = 0f,
    slant: Float = 0f,
    round: Float = 0f
) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        // Using Normal weight for matching prevents the system from overriding our custom axes
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", weight),
            FontVariation.Setting("wdth", width),
            FontVariation.Setting("opsz", opsz),
            FontVariation.Setting("GRAD", grade),
            FontVariation.Setting("slnt", slant),
            // "ROND" is the common axis tag for Roundness in Google Sans Flex
            FontVariation.Setting("ROND", round)
        )
    )
)

val GoogleSansFlexDisplay = flexFont(weight = 950f, width = 85f, opsz = 32f, round = 100f)
val GoogleSansFlexHeadline = flexFont(weight = 700f, width = 115f, opsz = 32f, round = 60f)
val GoogleSansFlexBody = flexFont(weight = 450f, width = 100f, opsz = 16f, grade = 20f, round = 0f)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansFlexDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlexHeadline,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)
