package com.divanshgandhi.attendai.ui.admin.stafflist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.divanshgandhi.attendai.R
import com.divanshgandhi.attendai.ui.components.PlaceholderScreen

@Composable
fun StaffListScreen(onSignOut: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.admin_staff_list),
        message = stringResource(R.string.staff_list_placeholder),
        actionLabel = stringResource(R.string.sign_out),
        onAction = onSignOut,
    )
}
