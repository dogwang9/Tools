package com.example.lib.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.lib.R
import com.example.lib.utils.AndroidUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

class PhotoLocationDialogFragment() : BottomSheetDialogFragment() {

    companion object {
        const val KEY_ID_PHOTO_URI: String = "photo_uri"
        const val KEY_ID_LONGITUDE: String = "location_longitude"
        const val KEY_ID_LATITUDE: String = "location_latitude"

        fun newInstance(
            uri: Uri,
            latitude: Double,
            longitude: Double
        ): PhotoLocationDialogFragment {
            val fragment = PhotoLocationDialogFragment()
            val args = Bundle()
            args.putParcelable(KEY_ID_PHOTO_URI, uri)
            args.putDouble(KEY_ID_LONGITUDE, longitude)
            args.putDouble(KEY_ID_LATITUDE, latitude)
            fragment.setArguments(args)
            return fragment
        }
    }

    private val leafletMapHtmlPath = "file:///android_asset/html/leaflet_map.html"
    private lateinit var mWebView: WebView

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.sheet_photo_location, container, false)
        initView(view)
        return view
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun initView(view: View) {
        val activity = activity ?: return
        val arguments = arguments ?: return
        dialog?.window?.navigationBarColor = Color.TRANSPARENT

        val locationTextView = view.findViewById<TextView>(R.id.tv_location)
        val latLongTextView = view.findViewById<TextView>(R.id.tv_latlong)
        val openInGoogleMapTextView = view.findViewById<TextView>(R.id.tv_open_in_google_map)
        val locationIconImageView = view.findViewById<ImageView>(R.id.iv_location)

        mWebView = view.findViewById(R.id.v_webview)
        mWebView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.apply {
                    outline?.setRoundRect(
                        0, 0,
                        measuredWidth, measuredHeight, AndroidUtils.dpToPx(12).toFloat()
                    )
                }
            }

        }
        mWebView.setClipToOutline(true)

        val uri = arguments.getParcelable<Uri>(KEY_ID_PHOTO_URI)
        val longitude = arguments.getDouble(KEY_ID_LONGITUDE)
        val latitude = arguments.getDouble(KEY_ID_LATITUDE)

        latLongTextView.text = String.format(
            Locale.getDefault(),
            "%.2f, %.2f",
            latitude,
            longitude
        )

        view.findViewById<Button>(R.id.btn_got_it).setOnClickListener {
            dismissAllowingStateLoss()
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            String.format(Locale.getDefault(), "geo:0,0?q=%f,%f", latitude, longitude).toUri()
        ).setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(activity.packageManager) != null) {
            openInGoogleMapTextView.visibility = View.VISIBLE
            openInGoogleMapTextView.setOnClickListener {
                activity.startActivity(intent)
            }

        } else {
            openInGoogleMapTextView.visibility = View.GONE
        }

        if (!AndroidUtils.isNetworkAvailable(activity)) {
            mWebView.visibility = View.GONE
            locationTextView.visibility = View.GONE
            return
        }

//        locationIconImageView.setOnClickListener{
//            val mapIntent = new Intent(context, PhotoPrivacyLocationActivity.class);
//            mapIntent.putExtra(KEY_ID_PHOTO_PATH, path);
//            mapIntent.putExtra(KEY_ID_LONGITUDE, longitude);
//            mapIntent.putExtra(KEY_ID_LATITUDE, latitude);
//            context.startActivity(mapIntent);
//        }

        lifecycleScope.launch(Dispatchers.IO) {
            val address = AndroidUtils.getAddress(activity, longitude, latitude)
            withContext(Dispatchers.Main) {
                if (TextUtils.isEmpty(address)) {
                    locationTextView.visibility = View.GONE

                } else {
                    locationTextView.text = address
                }
            }
        }

        mWebView.getSettings().javaScriptEnabled = true
        mWebView.loadUrl(leafletMapHtmlPath)
        mWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                uri ?: return

                Glide.with(activity)
                    .asBitmap()
                    .load(uri)
                    .override(150, 150)
                    .transform(CenterCrop(), RoundedCorners(AndroidUtils.dpToPx(5)))
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val outputStream = ByteArrayOutputStream()
                                resource.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                val base64 =
                                    Base64.encodeToString(
                                        outputStream.toByteArray(),
                                        Base64.NO_WRAP
                                    )
                                val safeBase64 = JSONObject.quote("data:image/png;base64,$base64")
                                val js = "showMap($latitude, $longitude, $safeBase64)"

                                withContext(Dispatchers.Main) {
                                    mWebView.evaluateJavascript(js, null)
                                }
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        }

        // 屏蔽webview的触摸事件
        mWebView.setOnTouchListener { v, event ->
            true
        }
    }

    override fun onDestroy() {
        mWebView.destroy()
        super.onDestroy()
    }
}