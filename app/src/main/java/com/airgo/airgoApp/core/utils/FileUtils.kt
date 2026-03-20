package com.airgo.airgoApp.core.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    /**
     * Toma la URI de la galería y crea un archivo físico temporal en la caché de la app.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            // Abrimos el canal de comunicación con la galería
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            // Creamos un archivo temporal en la carpeta oculta de caché de nuestra app
            // Le ponemos un nombre genérico como "temp_profile_pic.jpg"
            val tempFile = File(context.cacheDir, "temp_profile_pic_${System.currentTimeMillis()}.jpg")

            // Copiamos los datos de la galería a nuestro archivo temporal
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)

            // Cerramos los canales para no gastar memoria
            inputStream?.close()
            outputStream.close()

            tempFile // Devolvemos el archivo físico listo para enviar
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
