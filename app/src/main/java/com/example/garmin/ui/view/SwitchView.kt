package com.example.garmin.ui.view

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@SuppressLint("ResourceType")
@Composable
fun SwitchView(
    title: String,
    checkedState: MutableState<Boolean>,
    onClick: (check: Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .clickable {
                onClick(!checkedState.value)
                checkedState.value = !checkedState.value
            }
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            lineHeight = 19.2.sp,
          //  fontFamily = FontFamily(Font(R.font.onest_regular, FontWeight.Normal)),
            fontWeight = FontWeight(400),
          //  color = LabelMainColor,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(24.dp))
        Switch(
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Blue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray,
                checkedBorderColor = Color.White,
                uncheckedBorderColor = Color.White,
                uncheckedIconColor = Color.White,
                checkedIconColor = Color.White,

                ),
            checked = checkedState.value,
            onCheckedChange = {
                onClick(it)
                checkedState.value = it
            }
        )
    }
}

