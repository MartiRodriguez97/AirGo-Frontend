package com.airgo.airgoApp.core.network

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.utils.UiText
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object NetworkErrorMapper {
    fun fromIOException(error: IOException): UiText =
        when (error) {
            is UnknownHostException ->
                UiText.StringResource(R.string.error_network_unknown_host)
            is SocketTimeoutException ->
                UiText.StringResource(R.string.error_network_timeout)
            is SSLException ->
                UiText.StringResource(R.string.error_network_ssl)
            else ->
                UiText.StringResource(R.string.error_network_generic)
        }
}
