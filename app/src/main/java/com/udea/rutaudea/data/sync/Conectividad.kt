package com.udea.rutaudea.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Comprobación de conectividad usada antes de intentar sincronizar. */
object Conectividad {

    /**
     * `true` si hay una red activa con capacidad de Internet. No exige que
     * esté validada: en redes cautivas el intento fallará y se conservará
     * el banco local, que es el comportamiento deseado.
     */
    fun hayRed(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
        val red = manager.activeNetwork ?: return false
        val capacidades = manager.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
