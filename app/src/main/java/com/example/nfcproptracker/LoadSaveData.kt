package com.example.nfcproptracker

import android.content.Context
import java.io.File

fun saveScannedTagsToFile(context: Context, scannedTags: NfcTagList) {
    val file = File(context.filesDir, "scanned_tags.csv")
    file.printWriter().use { writer ->
        writer.println("TagUID,Nickname,Timestamp")
        scannedTags.forEach { tag ->
            writer.println("${tag.tagUid},${tag.nickname},${tag.timestamp}")
        }
    }
}

fun loadScannedTagsFromFile(context: Context): NfcTagList {
    val file = File(context.filesDir, "scanned_tags.csv")
    if (!file.exists()) return NfcTagList()

    val scannedTags = NfcTagList()
    val lines = file.readLines()

    for (i in 1 until lines.size) {
        val parts = lines[i].split(",")
        if (parts.size == 3) {
            val tag = NfcTag(parts[0], parts[1], parts[2])
            scannedTags.add(tag)
        }
    }
    return scannedTags
}
