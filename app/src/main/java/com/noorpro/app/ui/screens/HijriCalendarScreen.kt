package com.noorpro.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.noorpro.app.ui.islamic.IslamicCalendarScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

/** Former Hijri-only list. Folded into the merged Calendar screen; kept so old calls still resolve. */
@Composable
fun HijriCalendarScreen(viewModel: DeenViewModel, modifier: Modifier = Modifier) {
    IslamicCalendarScreen(viewModel)
}
