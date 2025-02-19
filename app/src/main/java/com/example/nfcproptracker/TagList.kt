package com.example.nfcproptracker

class NfcTagList : ArrayList<NfcTag>()

data class NfcTag(
    val tagUid: String,
    val nickname: String,
    val timestamp: String
)
