package com.example.nfcproptracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NfcTagAdapter(private val tags: MutableList<NfcTag>) :
    RecyclerView.Adapter<NfcTagAdapter.NfcTagViewHolder>() {

    class NfcTagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nicknameTextView: TextView = itemView.findViewById(R.id.nicknameTextView)
        val tagIdTextView: TextView = itemView.findViewById(R.id.tagIdTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NfcTagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nfc_tag, parent, false)
        return NfcTagViewHolder(view)
    }

    override fun onBindViewHolder(holder: NfcTagViewHolder, position: Int) {
        val tag = tags[position]
        holder.timestampTextView.text = tag.timestamp
        holder.nicknameTextView.text = tag.nickname
        holder.tagIdTextView.text = tag.tagUid
    }

    override fun getItemCount(): Int = tags.size
}