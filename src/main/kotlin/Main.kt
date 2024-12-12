package org.example

import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.*

fun expandKey(keyHex: Int): ByteArray {
    val keyBytes = ByteArray(2)
    keyBytes[0] = (keyHex shr 8 and 0xff).toByte()
    keyBytes[1] = (keyHex and 0xff).toByte()
    return ByteArray(16) { keyBytes[it % keyBytes.size] }
}

suspend fun decryptImage(image: File, key: Int, directory: String): Boolean {
    val encryptedData = image.readBytes()
    val iv = encryptedData.take(16).toByteArray() // Los primeros 16 bytes como IV
    val key128 = expandKey(key) // Expande la clave a 16 bytes

    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key128, "AES"), IvParameterSpec(iv))

    return try {
        val decrypted = cipher.doFinal(encryptedData).drop(16).toByteArray() // Descifra y elimina el IV

        // Verificar si los primeros 3 bytes corresponden a la cabecera JPEG
        if (decrypted.size > 3 && decrypted[0] == 0xFF.toByte() && decrypted[1] == 0xD8.toByte() && decrypted[2] == 0xFF.toByte()) {
            val outputFile = File(directory, "${image.nameWithoutExtension}_decrypted.jpg")
            outputFile.writeBytes(decrypted) // Guarda la imagen descifrada
            println("Imagen descifrada con clave 0x${key.toString(16).toUpperCase()}: ${outputFile.name}")
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun processImage(image: File, directory: String): Long {
    println("Procesando imagen: ${image.name}")

    return measureTimeMillis {
        coroutineScope {
            val jobs = (0x0000..0xFFFF).chunked(1000).map { keysChunk ->
                async {
                    for (key in keysChunk) {
                        println("Probando clave: 0x${key.toString(16).toUpperCase()}")
                        val success = decryptImage(image, key, directory)
                        if (success) return@async true
                    }
                    false
                }
            }

            val success = jobs.awaitAll().any { it }
            if (!success) println("No se encontró ninguna clave válida para la imagen ${image.name}")
        }
    }
}

fun main() = runBlocking {
    println("hola")
    val directory = "./cifradas" // Ruta del directorio de las imágenes cifradas
    val images = File(directory).listFiles()?.filter { it.extension == "bin" } ?: return@runBlocking

    println("Archivos a procesar: ${images.size}")

    val totalDuration = measureTimeMillis {
        val results = images.map { image ->
            async {
                val duration = processImage(image, directory)
                println("Tiempo de procesamiento para ${image.name}: ${duration} ms")
            }
        }
        results.awaitAll()
    }

    println("Procesamiento total completado en ${totalDuration} ms")
}