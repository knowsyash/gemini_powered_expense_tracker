package com.expensetracker.ai.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.databinding.ActivityChatBinding
import com.expensetracker.ai.ui.adapter.ChatAdapter
import com.expensetracker.ai.ui.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private var scrollUpdateRunnable: Runnable? = null

    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(
                (application as ExpenseTrackerApplication).chatMessageRepository,
                (application as ExpenseTrackerApplication).expenseRepository,
                (application as ExpenseTrackerApplication).budgetRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle keyboard properly with adjustPan
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // Additional keyboard settings
        window.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Auto-scroll to bottom when activity starts
        binding.recyclerViewChat.post { scrollToBottomInstant() }

        // Let user manually focus when ready - don't force focus
        // binding.etMessage.requestFocus()
    }

    private fun scrollToBottom() {
        scrollToBottomSmooth()
    }

    private fun scrollToBottomSmooth() {
        if (chatAdapter.itemCount > 0) {
            val lastPosition = chatAdapter.itemCount - 1
            binding.recyclerViewChat.smoothScrollToPosition(lastPosition)

            // Ensure we actually reach the bottom
            binding.recyclerViewChat.postDelayed(
                    {
                        val layoutManager =
                                binding.recyclerViewChat.layoutManager as? LinearLayoutManager
                        if (layoutManager?.findLastCompletelyVisibleItemPosition() != lastPosition
                        ) {
                            binding.recyclerViewChat.scrollToPosition(lastPosition)
                        }
                    },
                    300
            )
        }
    }

    private fun scrollToBottomInstant() {
        // Scroll RecyclerView to last position first
        if (::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
            binding.recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
        }

        // Then scroll the NestedScrollView to bottom
        val nestedScrollView =
                binding.recyclerViewChat.parent.parent as? androidx.core.widget.NestedScrollView
        nestedScrollView?.post {
            nestedScrollView.fullScroll(android.view.View.FOCUS_DOWN)
            // Ensure we're really at the bottom
            nestedScrollView.postDelayed(
                    { nestedScrollView.smoothScrollTo(0, nestedScrollView.getChildAt(0).height) },
                    50
            )
        }
    }

    private fun forceScrollToBottom() {
        if (::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
            val lastPosition = chatAdapter.itemCount - 1

            // Scroll RecyclerView
            binding.recyclerViewChat.scrollToPosition(lastPosition)
            binding.recyclerViewChat.smoothScrollToPosition(lastPosition)

            // Scroll NestedScrollView to bottom
            val nestedScrollView =
                    binding.recyclerViewChat.parent.parent as? androidx.core.widget.NestedScrollView
            nestedScrollView?.let { scrollView ->
                scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }

                scrollView.postDelayed(
                        { scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height) },
                        300
                )
            }
        }
    }

    private fun isAtBottom(): Boolean {
        // The most reliable way to check if we're at the bottom is to see if the
        // RecyclerView can no longer scroll down.
        return !binding.recyclerViewChat.canScrollVertically(1)
    }

    private fun setupToolbar() {
        // No toolbar setup needed for modern layout
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this@ChatActivity)
        layoutManager.stackFromEnd = false

        binding.recyclerViewChat.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter

            // Disable RecyclerView's own scrolling since it's inside NestedScrollView
            isNestedScrollingEnabled = false
        }

        // Remove all scroll listeners to prevent keyboard interference
        // We'll add minimal scrolling functionality without the button
    }

    private fun setupObservers() {
        chatViewModel.chatMessages.observe(this) { messages ->
            if (messages.isNotEmpty()) {
                chatAdapter.submitList(messages) {
                    // Auto-scroll to bottom when messages are loaded or updated
                    binding.recyclerViewChat.post {
                        scrollToBottomInstant()
                        // Also ensure RecyclerView scrolls to last position
                        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        chatViewModel.isLoading.observe(this) { isLoading ->
            binding.btnSend.isEnabled = !isLoading
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            android.util.Log.d("ChatActivity", "Send button clicked")
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            android.util.Log.d("ChatActivity", "Editor action: $actionId")
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Simple input handling without complex focus management

        // Remove all focus listeners that might interfere with keyboard

        // Quick action chips - commented out since they don't exist in simplified layout
        /*
        binding.chipBalance.setOnClickListener {
            chatViewModel.sendMessage("What's my balance?")
            binding.recyclerViewChat.postDelayed({ scrollToBottomInstant() }, 100)
        }

        binding.chipCategories.setOnClickListener {
            chatViewModel.sendMessage("Show me expense categories")
            binding.recyclerViewChat.postDelayed({ scrollToBottomInstant() }, 100)
        }

        binding.chipAnalysis.setOnClickListener {
            chatViewModel.sendMessage("Give me spending analysis")
            binding.recyclerViewChat.postDelayed({ scrollToBottomInstant() }, 100)
        }

        binding.chipHelp.setOnClickListener {
            chatViewModel.sendMessage("Help")
            binding.recyclerViewChat.postDelayed({ scrollToBottomInstant() }, 100)
        }
        */
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        android.util.Log.d("ChatActivity", "sendMessage called with: '$message'")

        if (message.isNotEmpty()) {
            android.util.Log.d("ChatActivity", "Message is not empty, sending to ViewModel")

            chatViewModel.sendMessage(message)
            binding.etMessage.text?.clear()
            android.util.Log.d("ChatActivity", "Message cleared from input field")

            // Simple scroll to bottom after sending
            binding.recyclerViewChat.postDelayed({ scrollToBottomInstant() }, 100)
        } else {
            android.util.Log.d("ChatActivity", "Message is empty, not sending")
        }
    }

    override fun onResume() {
        super.onResume()
        // Let the system handle focus naturally
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Let the system handle focus naturally
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
