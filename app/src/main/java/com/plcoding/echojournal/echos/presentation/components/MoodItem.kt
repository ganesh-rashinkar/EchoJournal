package com.plcoding.echojournal.echos.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.plcoding.echojournal.echos.presentation.models.MoodUi

@Composable
fun MoodItem(
    selected:Boolean,
    moodUi: MoodUi,
    onClick:()->Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(64.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick=onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            imageVector = if(selected) {
                ImageVector.vectorResource(moodUi.iconSet.fill)
            }
            else {
                ImageVector.vectorResource(moodUi.iconSet.outline)
            },
            contentDescription = moodUi.title.asString(),
            modifier = Modifier.height(40.dp),
            contentScale = ContentScale.FillHeight
        )
        Text(
            moodUi.title.asString(),
            style = MaterialTheme.typography.labelMedium,
            color = if(selected){
                MaterialTheme.colorScheme.onSurface
            }
            else{
                MaterialTheme.colorScheme.outline
            }
        )
    }
}