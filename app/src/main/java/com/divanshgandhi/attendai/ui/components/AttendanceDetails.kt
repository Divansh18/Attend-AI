package com.divanshgandhi.attendai.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.divanshgandhi.attendai.data.local.AttendanceEntity
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** The same local date/time and coordinate presentation in staff success and Admin history. */
@Composable
fun AttendanceDetails(row: AttendanceEntity) {
    val date = Date(row.timestamp)
    Text("Date: " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(date))
    Text("Time: " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(date))
    Text(String.format(Locale.US, "Latitude: %.6f", row.latitude))
    Text(String.format(Locale.US, "Longitude: %.6f", row.longitude))
    row.accuracyMeters?.let { Text(String.format(Locale.US, "Accuracy: ±%.1f m", it)) }
}
