package dev.idkwuu.allesandroid.ui.profile

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import dev.idkwuu.allesandroid.R
import dev.idkwuu.allesandroid.api.AllesEndpointsInterface
import dev.idkwuu.allesandroid.api.RetrofitClientInstance
import dev.idkwuu.allesandroid.ui.feed.FeedAdapter
import dev.idkwuu.allesandroid.util.SharedPreferences
import dev.idkwuu.allesandroid.util.dont_care_lol
import jp.wasabeef.blurry.Blurry
import jp.wasabeef.blurry.internal.Blur
import jp.wasabeef.blurry.internal.BlurFactor
import kotlin.IllegalStateException

class ProfileFragment : Fragment() {

    fun newInstance(user: String, withBackButton: Boolean): ProfileFragment? {
        val bundle = Bundle()
        bundle.putString("user", user)
        bundle.putBoolean("withBackButton", withBackButton)
        val fragment = ProfileFragment()
        fragment.arguments = bundle
        return fragment
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this).get(ProfileViewModel::class.java)
    }
    private lateinit var adapter: FeedAdapter
    private lateinit var pfpBitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        retainInstance = true
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.shimmer)
        shimmer.startShimmer()

        adapter = FeedAdapter(view.context)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false

        val user: String = try {
            requireArguments().getString("user")
        } catch (e: IllegalStateException) {
            SharedPreferences.current_user
        }.toString()

        observeData(user)

        // Setup pull to refresh
        view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh).setOnRefreshListener {
            refresh(user)
        }

        // Back button
        val withBackButton = try {
            requireArguments().getBoolean("withBackButton")
        } catch (e: IllegalStateException) {
            false
        }
        if (withBackButton) {
            val back = view.findViewById<ImageButton>(R.id.back)
            back.setOnClickListener { requireActivity().finish() }
            back.visibility = View.VISIBLE
        }
        // Settings button
        if (user == SharedPreferences.current_user) {
            view.findViewById<ImageButton>(R.id.settings).visibility =View.VISIBLE
        }

        return view
    }

    private fun refresh(user: String) {
        val shimmer = requireView().findViewById<ShimmerFrameLayout>(R.id.shimmer)
        shimmer.startShimmer()
        shimmer.visibility = View.VISIBLE
        requireView().findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        requireView().findViewById<View>(R.id.profile).visibility = View.GONE
        observeData(user)
        requireView().findViewById<SwipeRefreshLayout>(R.id.pullToRefresh).isRefreshing = true
    }

    @SuppressLint("SetTextI18n")
    private fun observeData(user: String) {
        viewModel.fetchUser(user).observe(viewLifecycleOwner, Observer {
            val profileView = requireView().findViewById<View>(R.id.profile)
            profileView.visibility = View.VISIBLE
            // Text
            profileView.findViewById<TextView>(R.id.user_title).text = it.nickname
            profileView.findViewById<TextView>(R.id.user_handle).text = "@${it.username}"
            profileView.findViewById<TextView>(R.id.user_followers).text = "${it.followers.toString()} ${getString(R.string.followers)}"
            //profileView.findViewById<TextView>(R.id.user_following).text = "${it.following.toString()} ${getString(R.string.following)}"
            if (it.followingUser!!) {
                profileView.findViewById<TextView>(R.id.follows_you).visibility = View.VISIBLE
            }
            profileView.findViewById<TextView>(R.id.user_description).text = it.about
            if (user != SharedPreferences.current_user) {
                val followButton = profileView.findViewById<MaterialButton>(R.id.follow_button)
                followButton.visibility = View.VISIBLE
                if (it.following!!) {
                    setFollow(followButton, it.following!!, it.username!!)
                }
                var following = it.following!!
                followButton.setOnClickListener {_ ->
                    following = !following
                    setFollow(followButton, !following, it.username!!)
                }
            }

            // Profile picture
            Glide.with(requireView().context)
                .asBitmap()
                .load("https://avatar.alles.cx/u/${it.username!!}?size=100")
                .into(object: SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap,transition: Transition<in Bitmap>?) {
                        pfpBitmap = resource
                        setColorsProfile(resource)
                    }
                })

            // Set posts list
            requireView().findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
            requireView().findViewById<SwipeRefreshLayout>(R.id.pullToRefresh).isRefreshing = false
            adapter.setListData(it.posts!!.toMutableList())
            adapter.notifyDataSetChanged()

            val shimmer = requireView().findViewById<ShimmerFrameLayout>(R.id.shimmer)
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
        })
    }

    private fun setFollow(followButton: MaterialButton, follow: Boolean, username: String) {
        val retrofit = RetrofitClientInstance().getRetrofitInstance()
            .create(AllesEndpointsInterface::class.java)
        if (follow) {
            followButton.background = requireContext().getDrawable(R.drawable.solid_blue_rounded)
            followButton.setTextColor(requireContext().getColor(R.color.background))
            followButton.text = getString(R.string.unfollow)
            retrofit.follow(SharedPreferences.login_token!!, username).enqueue(dont_care_lol)

        } else {
            followButton.background = requireContext().getDrawable(R.drawable.rounded_rectangle_small)
            followButton.setTextColor(requireContext().getColor(R.color.colorAccent))
            followButton.text = getString(R.string.follow)
            retrofit.unfollow(SharedPreferences.login_token!!, username).enqueue(dont_care_lol)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        setColorsProfile(pfpBitmap)
        super.onConfigurationChanged(newConfig)
    }

    private fun setColorsProfile(profile_picture: Bitmap) {
        requireView().findViewById<ImageView>(R.id.profile_image).setImageBitmap(profile_picture)
        try {
            // Get the layout in a bitmap
            val layout = requireView().findViewById<ConstraintLayout>(R.id.info)
            val layoutBitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(layoutBitmap)
            layout.draw(canvas)

            // Create blurred version of profile picture
            val blurFactor = BlurFactor()
            blurFactor.width = profile_picture.width
            blurFactor.height = profile_picture.height
            val blurryPicture = Blur.of(requireContext(), profile_picture, blurFactor)
            val scaledBlurryPicture = Bitmap.createScaledBitmap(
                blurryPicture,
                layoutBitmap.width,
                layoutBitmap.width * (layoutBitmap.width / layoutBitmap.height),
                false
            )
            requireView().findViewById<ImageView>(R.id.background).setImageBitmap(blurryPicture)

            // Do motherfucking math (i thought that school shit was over)
            val alignment: Float
            val doYaxis: Boolean
            if (layoutBitmap.width > layoutBitmap.height) {
                // Y
                alignment = (scaledBlurryPicture.height - layoutBitmap.height) / 2f
                doYaxis = true
            } else {
                // X
                alignment = (scaledBlurryPicture.width - layoutBitmap.width) / 2f
                doYaxis = false
            }

            // Generate final bitmap
            val bitmapOut = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
            val canvasOut = Canvas(bitmapOut)

            val maskPaint = Paint()
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            // Source
            canvasOut.drawBitmap(
                scaledBlurryPicture,
                if (!doYaxis) -alignment else 0f,
                if (doYaxis) -alignment else 0f,
                null
            )
            // Destination
            canvasOut.drawBitmap(layoutBitmap, 0f, 0f, maskPaint)
            requireView().findViewById<ImageView>(R.id.overlay).setImageBitmap(bitmapOut)

            // Hide views
            requireView().findViewById<ConstraintLayout>(R.id.bar).visibility = View.INVISIBLE
            requireView().findViewById<ConstraintLayout>(R.id.info).visibility = View.INVISIBLE
        } catch (e: Exception) {
            //fuck my life
            Blurry.with(requireContext()).from(profile_picture).into(requireView().findViewById(R.id.background))
        }
    }
}