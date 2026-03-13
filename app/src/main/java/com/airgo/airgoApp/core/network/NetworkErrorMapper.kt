package com.airgo.airgoApp.core.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object NetworkErrorMapper {
    fun fromIOException(error: IOException): String =
        when (error) {
            is UnknownHostException ->
                "No se pudo conectar al servidor. Verifica tu conexion a internet."
            is SocketTimeoutException ->
                "La conexion al servidor ha tardado demasiado. Intenta nuevamente."
            is SSLException ->
                "No se pudo establecer una conexion segura."
            else ->
                "Error de red. Revisa tu conexion e intentalo otra vez."
        }
}
