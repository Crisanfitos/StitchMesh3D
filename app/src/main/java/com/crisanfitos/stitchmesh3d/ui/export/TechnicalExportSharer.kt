package com.crisanfitos.stitchmesh3d.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidad de interacción con el sistema operativo Android para compartir ficheros exportados
 * mediante [FileProvider] y Storage Access Framework (SM-058, SM-088).
 */
object TechnicalExportSharer {

    /**
     * Lanza el Intent nativo de compartición de Android con los URIs seguros generados por [FileProvider].
     */
    fun shareFiles(context: Context, files: List<GeneratedExportFileUiModel>) {
        if (files.isEmpty()) return

        val uris = ArrayList<Uri>()
        for (f in files) {
            val file = File(f.localPathOrUri)
            if (file.exists()) {
                val uri = runCatching {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }.getOrNull()
                if (uri != null) {
                    uris.add(uri)
                }
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(context, "No se encontraron ficheros para compartir", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = files.first().mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(intent, "Compartir entregables técnicos StitchMesh")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Muestra confirmación o abre selector de guardado para almacenamiento local (SAF).
     */
    fun saveFilesFeedback(context: Context, files: List<GeneratedExportFileUiModel>) {
        if (files.isEmpty()) return
        val count = files.size
        Toast.makeText(
            context,
            "Se han preparado $count ficheros técnicos en el almacenamiento de la app",
            Toast.LENGTH_LONG
        ).show()
    }
}
