package com.example.nfcproptracker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var csvFile: File
    private lateinit var tagScanListView: RecyclerView
    private lateinit var adapter: NfcTagAdapter
    private var scannedTags = NfcTagList()
    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_LONG).show()
            finish()
        }

        csvFile = File(filesDir, "nfc_scans.csv")

        if (!csvFile.exists()) {
            csvFile.appendText("Nickname,Tag ID,Date,Time\n")
        }

        val exportButton: Button = findViewById(R.id.export_csv)
        exportButton.setOnClickListener {
            createCsvFile()
        }

        val clearButton: Button = findViewById(R.id.clear)
        clearButton.setOnClickListener {
            clearTagList()
        }
        scannedTags = loadScannedTagsFromFile(this)

        tagScanListView = findViewById(R.id.recyclerView)
        adapter = NfcTagAdapter(scannedTags)
        tagScanListView.layoutManager = LinearLayoutManager(this)
        tagScanListView.adapter = adapter

        createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    writeCsvToUri(uri)  // Your function to save the file
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        tag?.let {
            val tagId = it.id.joinToString(":") { byte -> "%02X".format(byte) }
            onNfcTagScanned(tagId)
        }
    }

    private fun enableNfcForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techLists = arrayOf(arrayOf(android.nfc.tech.Ndef::class.java.name))

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techLists)
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun onNfcTagScanned(tagUid: String) {
        val existingTag = scannedTags.find { it.tagUid == tagUid }

        if (existingTag != null) {
            addTagScanData(existingTag.nickname, existingTag.tagUid)
        } else {
            showNicknameDialog(tagUid)
        }
    }

    private fun addTagScanData(nickname: String, tagId: String, doToast: Boolean = true){
        val timestamp = SimpleDateFormat("h:mm:ss a MM/dd/yy", Locale.getDefault()).format(Date())
        val nfcTag = NfcTag(tagId, nickname, timestamp)
        scannedTags.add(nfcTag)
        adapter.notifyItemInserted(scannedTags.size - 1)
        saveScannedTagsToFile(this, scannedTags)
        tagScanListView.scrollToPosition(scannedTags.size - 1)


        if (doToast)
        {
            Toast.makeText(this, "Scanned $nickname!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNicknameDialog(tagId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Nickname")

        val input = android.widget.EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val nickname = input.text.toString().ifEmpty { "Unknown" }
            addTagScanData(nickname, tagId, false)
            Toast.makeText(this, "Tag saved: $nickname", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun createCsvFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "nfc_tags.csv")
        }
        createFileLauncher.launch(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearTagList() {
        scannedTags.clear()
        Toast.makeText(this, "Cleared Tag list!", Toast.LENGTH_SHORT).show()
        adapter.notifyDataSetChanged()
        val file = File(filesDir, "scanned_tags.csv")
        if (file.exists()) file.delete()
    }

    private fun writeCsvToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("Tag UID,Nickname,Timestamp\n")
                    for (tag in scannedTags) {
                        writer.write("${tag.tagUid},${tag.nickname},${tag.timestamp}\n")
                    }
                }
            }
            Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}