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

suspend fun decryptImage(imageBytes: ByteArray, iv: ByteArray, key: Int): ByteArray? {
    val key128 = expandKey(key)
    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key128, "AES"), IvParameterSpec(iv))

    return try {
        val decrypted = cipher.doFinal(iv + imageBytes).drop(16).toByteArray()


        if (decrypted.size > 3 && decrypted[0] == 0xFF.toByte() && decrypted[1] == 0xD8.toByte() && decrypted[2] == 0xFF.toByte()) {
            decrypted
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

suspend fun processImage(image: File, directory: String): Long {
    println("Procesando imagen: ${image.name}")

    val encryptedData = image.readBytes()
    val iv = encryptedData.take(16).toByteArray()
    val imageBytes = encryptedData.drop(16).toByteArray()

    val stopSignal = CompletableDeferred<Boolean>()
    return measureTimeMillis {
        val found = coroutineScope {
            (0x0000..0xFFFF).chunked(2000).map { keysChunk ->
                async(Dispatchers.Default) {
                    for (key in keysChunk) {
                        if (stopSignal.isCompleted) return@async false

                        if (key % 100 == 0) {
                            println("Probando clave: 0x${key.toString(16).toUpperCase()}")
                        }

                        val decrypted = decryptImage(imageBytes, iv, key)
                        if (decrypted != null) {
                            val outputFile = File(directory, "${image.nameWithoutExtension}_decrypted.jpg")
                            outputFile.writeBytes(decrypted)
                            println("Imagen descifrada con clave 0x${key.toString(16).toUpperCase()}: ${outputFile.name}")
                            stopSignal.complete(true)
                            return@async true
                        }
                    }
                    false
                }
            }.awaitAll().any { it }
        }

        if (!found && !stopSignal.isCompleted) println("No se encontró ninguna clave válida para la imagen ${image.name}")
    }
}

fun main() = runBlocking {
    val directory = "./cifradas"
    val images = File(directory).listFiles()?.filter { it.extension == "bin" } ?: return@runBlocking

    println("Archivos a procesar: ${images.size}")

    val totalDuration = measureTimeMillis {
        val parallelism = Runtime.getRuntime().availableProcessors()
        println("Usando paralelismo con $parallelism hilos")

        val results = images.map { image ->
            async(Dispatchers.Default.limitedParallelism(parallelism)) {

                val imageDuration = measureTimeMillis {
                    val duration = processImage(image, directory)
                }

            }
        }
        results.awaitAll()
    }
    println("Procesamiento total completado en ${totalDuration} ms")
}



