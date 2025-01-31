package com.group5.safehomenotifier

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun saveImageToHistory(context: Context, imageUrl: String) {
    val database = HistoryDatabase.getDatabase(context)
    val historyDao = database.historyImageDao()

    CoroutineScope(Dispatchers.IO).launch {
        historyDao.insertImage(HistoryImage(imageUrl = imageUrl))
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Image successfully saved to History", Toast.LENGTH_SHORT).show()
        }
    }
}
