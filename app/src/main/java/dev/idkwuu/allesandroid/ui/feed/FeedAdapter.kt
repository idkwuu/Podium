package dev.idkwuu.allesandroid.ui.feed

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dev.idkwuu.allesandroid.R
import dev.idkwuu.allesandroid.api.AllesEndpointsInterface
import dev.idkwuu.allesandroid.api.RetrofitClientInstance
import dev.idkwuu.allesandroid.models.AllesPost
import dev.idkwuu.allesandroid.models.AllesVote
import dev.idkwuu.allesandroid.util.SharedPreferences
import kotlinx.android.synthetic.main.item_post.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedAdapter(
    private val context: Context
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private var dataList = mutableListOf<AllesPost>()

    fun setListData(data: MutableList<AllesPost>) {
        dataList = data
    }

    private fun vote(itemView: View, slug: String, vote: Int, currentVote: Int) {
        val retrofit = RetrofitClientInstance().getRetrofitInstance()
            .create(AllesEndpointsInterface::class.java)
        when (vote) {
            0 -> {
                ImageViewCompat.setImageTintList(itemView.plus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.vote_nothing)))
                ImageViewCompat.setImageTintList(itemView.minus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.vote_nothing)))
                itemView.votesCount.text = (itemView.votesCount.text.toString().toInt() - currentVote).toString()
            }
            1 -> {
                ImageViewCompat.setImageTintList(itemView.plus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.plus_selected)))
                ImageViewCompat.setImageTintList(itemView.minus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.vote_nothing)))
                itemView.votesCount.text = (itemView.votesCount.text.toString().toInt() + vote).toString()
            }
            -1 -> {
                ImageViewCompat.setImageTintList(itemView.plus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.vote_nothing)))
                ImageViewCompat.setImageTintList(itemView.minus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.minus_selected)))
                itemView.votesCount.text = (itemView.votesCount.text.toString().toInt() + vote).toString()
            }
        }
        retrofit.vote(SharedPreferences.login_token!!, slug, AllesVote(vote = vote)).enqueue(object : Callback<AllesVote> {
            override fun onFailure(call: Call<AllesVote>, t: Throwable) {
                Snackbar.make(itemView, R.string.vote_snackbar_error, Snackbar.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<AllesVote>, response: Response<AllesVote>) {
            }
        })
    }

    inner class FeedViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bindView(post: AllesPost) {
            // Author name
            if (post.author?.plus!!) {
                itemView.user_title.text = "${post.author?.name}\u207A"
            } else {
                itemView.user_title.text = post.author?.name
            }
            // Author username
            itemView.user_handle.text = "@${post.author?.username}"
            // Votes
            itemView.votesCount.text = post.score.toString()
            var actualVote = post.vote!!
            itemView.plus.setOnClickListener {
                actualVote = if (actualVote == 1) {
                    vote(itemView, post.slug!!, 0, post.vote!!)
                    0
                } else {
                    vote(itemView, post.slug!!, 1, post.vote!!)
                    1
                }
            }
            itemView.minus.setOnClickListener {
                actualVote = if (actualVote == -1) {
                    vote(itemView, post.slug!!, 0, post.vote!!)
                    0
                } else {
                    vote(itemView, post.slug!!, -1, post.vote!!)
                    -1
                }
            }
            // Comments
            itemView.comments_count.text = post.replyCount.toString()
            if (post.replyCount!! > 0) {
                itemView.comments_icon.setImageResource(R.drawable.ic_fluent_chat_20_filled)
            }
            // Has user voted?
            if (post.vote == 1) ImageViewCompat.setImageTintList(itemView.plus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.plus_selected)))
            else if (post.vote == -1) ImageViewCompat.setImageTintList(itemView.minus, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.minus_selected)))
            // Set post content
            if (post.content!!.isNotEmpty()) {
                itemView.post_text.visibility = View.VISIBLE
                itemView.post_text.text = post.content
            }
            // Post image
            if (post.image != null) {
                itemView.post_image.visibility = View.VISIBLE
                Glide.with(context).load(post.image).into(itemView.post_image)
            }
            // Set post longevity
            /*val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            TimeZone.setDefault(null)
            sdf.timeZone = TimeZone.getDefault()
            val time = sdf.parse(post.createdAt!!)!!.time
            val now = System.currentTimeMillis()
            itemView.time.text = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS)*/
            itemView.time.text = post.createdAt
            // Set profile photo
            Glide.with(context).load("https://avatar.alles.cx/u/${post.author?.username}?size=100").into(itemView.profile_image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return FeedViewHolder(view)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val post = dataList[position]
        holder.bindView(post)
    }
}