package com.group5.safehomenotifier

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun saveImageToHistory(context: Context, imageUrl: String) {
    val database = HistoryDatabase.getDatabase(context)
    val historyDao = database.historyImageDao()

    CoroutineScope(Dispatchers.IO).launch {
        historyDao.insertImage(HistoryImage(imageUrl = imageUrl))
    }
}
