package com.logflare.android.ui.theme

import android.graphics.drawable.ColorDrawable
import android.view.Window

/**
 * Paints the window behind the UI with [argb]. The XML theme only knows the system's day/night
 * mode, so the in-app theme choice (My > Appearance) sets this at runtime to the same colour
 * Compose draws; recreation and the keyboard then never flash the other theme's colour.
 */
fun Window.setAppBackground(argb: Int) = setBackgroundDrawable(ColorDrawable(argb))
