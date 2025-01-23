package com.group5.safehomenotifier

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.group5.safehomenotifier.ui.theme.CharcoalBlue
import com.group5.safehomenotifier.ui.theme.poppinsFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun HistoryScreen(
    historyImages: List<String>, deviceName: String?, onBack: () -> Unit, context: Context,
) {

    val database = HistoryDatabase.getDatabase(context)
    val historyDao = remember { database.historyImageDao() }
    var imageList by remember { mutableStateOf(listOf<HistoryImage>()) }
    //val context = LocalContext.current

    LaunchedEffect(Unit) {
        imageList = withContext(Dispatchers.IO) {
            historyDao.getAllImages()
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlue)
            .padding(16.dp), // Add padding to the Box if desired
        contentAlignment = Alignment.Center // Center the contents
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White // Customize color if needed
                    )
                }
                Text(
                    "History of Images:",
                    fontFamily = poppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = White
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(imageList) { image ->
                        val painter = rememberAsyncImagePainter(image.imageUrl)
                        val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(image.timestamp))

                        Column(modifier = Modifier.padding(8.dp)) {
                            Image(
                                painter = painter,
                                contentDescription = "Historical image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Received at: $readableDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Delete button
                            Button(
                                onClick = {
                                    // Perform the delete operation
                                    deleteImageAndUpdateList(image, historyDao, context)
                                    { updatedList ->
                                        imageList = updatedList
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text(
                                    text = "Delete",
                                    color = White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun deleteImageAndUpdateList(
    image: HistoryImage,
    historyDao: HistoryImageDao,
    context: Context,
    updateImageList: (List<HistoryImage>) -> Unit
) {
    // Perform the delete operation in a background thread
    CoroutineScope(Dispatchers.IO).launch {
        historyDao.deleteImage(image)

        // Once deleted, update the image list on the main thread
        val updatedList = historyDao.getAllImages()
        withContext(Dispatchers.Main) {
            // Update the UI state in a Composable way
            updateImageList(updatedList)
            Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
        }
    }
}

