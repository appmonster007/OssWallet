package com.osscabinet.ossWallet.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

@SuppressLint("MissingPermission")
object LastLocationProvider {
    fun getLastLocation(context: Context, onResult: (Location?) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val location = manager
            ?.getProviders(true)
            ?.mapNotNull { provider -> manager.getLastKnownLocation(provider) }
            ?.maxByOrNull { it.time }
        onResult(location)
    }
}
