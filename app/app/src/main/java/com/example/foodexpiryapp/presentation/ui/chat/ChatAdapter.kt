package com.example.foodexpiryapp.presentation.ui.chat

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemChatMessageBinding

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(private val binding: ItemChatMessageBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
            
            val layoutParams = binding.messageCard.layoutParams as android.widget.FrameLayout.LayoutParams
            
            if (message.isFromUser) {
                binding.messageCard.setCardBackgroundColor(Color.parseColor("#2196F3"))
                layoutParams.gravity = Gravity.END
            } else {
                binding.messageCard.setCardBackgroundColor(Color.parseColor("#2D2D2D"))
                layoutParams.gravity = Gravity.START
            }
            binding.messageCard.layoutParams = layoutParams
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}