package nhdeveloper.offlineai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class Message(
    val id: String,
    val content: String,
    val isUser: Boolean
)

/**
 * A single parsed piece of a message: either plain text or a fenced code block.
 */
private sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Code(val language: String, val code: String) : MessagePart()
}

/**
 * Splits a raw message string on ``` fences into alternating Text / Code parts.
 * Handles an unterminated trailing fence gracefully (still-streaming content).
 */
private fun parseMessageParts(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    val fence = "```"
    var index = 0
    while (index < content.length) {
        val fenceStart = content.indexOf(fence, index)
        if (fenceStart == -1) {
            if (index < content.length) parts.add(MessagePart.Text(content.substring(index)))
            break
        }
        if (fenceStart > index) {
            parts.add(MessagePart.Text(content.substring(index, fenceStart)))
        }
        val afterFence = fenceStart + fence.length
        val newlineIndex = content.indexOf('\n', afterFence)
        val fenceEnd = content.indexOf(fence, afterFence)

        if (newlineIndex == -1 || fenceEnd == -1) {
            // Unterminated / still-streaming code block: show what we have so far
            val lang = if (newlineIndex != -1) content.substring(afterFence, newlineIndex).trim() else ""
            val codeStart = if (newlineIndex != -1) newlineIndex + 1 else afterFence
            parts.add(MessagePart.Code(lang.ifEmpty { "code" }, content.substring(codeStart)))
            index = content.length
        } else {
            val lang = content.substring(afterFence, newlineIndex).trim()
            val code = content.substring(newlineIndex + 1, fenceEnd)
            parts.add(MessagePart.Code(lang.ifEmpty { "code" }, code))
            index = fenceEnd + fence.length
        }
    }
    return parts
}

class MessageAdapter(
    private val messages: List<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = layoutInflater.inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_message_assistant, parent, false)
            AssistantMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> {
                holder.content.text = message.content
            }
            is AssistantMessageViewHolder -> {
                bindAssistant(holder, message)
            }
        }
    }

    private fun bindAssistant(holder: AssistantMessageViewHolder, message: Message) {
        val context = holder.itemView.context
        holder.bubbleContainer.removeAllViews()

        if (message.content.isBlank()) {
            // Still waiting for the first token - show a subtle typing placeholder
            val placeholder = TextView(context).apply {
                text = "…"
                setTextColor(ContextCompat.getColor(context, R.color.nh_text_muted))
                textSize = 16f
            }
            holder.bubbleContainer.addView(placeholder)
            holder.copyBtn.visibility = View.GONE
            return
        }

        val inflater = LayoutInflater.from(context)
        for (part in parseMessageParts(message.content)) {
            when (part) {
                is MessagePart.Text -> {
                    if (part.text.isNotBlank()) {
                        val tv = TextView(context).apply {
                            text = part.text.trim()
                            setTextColor(ContextCompat.getColor(context, R.color.nh_gold_soft))
                            textSize = 14.5f
                            setTextIsSelectable(true)
                        }
                        holder.bubbleContainer.addView(tv)
                    }
                }
                is MessagePart.Code -> {
                    val codeView = inflater.inflate(
                        R.layout.item_code_block, holder.bubbleContainer, false
                    )
                    codeView.findViewById<TextView>(R.id.code_lang).text = part.language
                    val codeContent = codeView.findViewById<TextView>(R.id.code_content)
                    codeContent.text = part.code.trim('\n')
                    codeView.findViewById<ImageButton>(R.id.code_copy_btn).setOnClickListener {
                        copyToClipboard(context, "Code", part.code.trim('\n'))
                    }
                    holder.bubbleContainer.addView(codeView)
                }
            }
        }

        holder.copyBtn.visibility = View.VISIBLE
        holder.copyBtn.setOnClickListener {
            copyToClipboard(context, "Response", message.content)
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copy ho gaya", Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.msg_content)
    }

    class AssistantMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bubbleContainer: LinearLayout = view.findViewById(R.id.bubble_container)
        val copyBtn: ImageButton = view.findViewById(R.id.copy_btn)
    }
}
