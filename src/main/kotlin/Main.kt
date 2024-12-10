package org.example

import java.io.File
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun expandKey(keyHex: Int): ByteArray {
    val keyBytes = ByteArray(2)
    keyBytes[0] = (keyHex shr 8 and 0xff).toByte()
    keyBytes[1] = (keyHex and 0xff).toByte()
    return ByteArray(16) { keyBytes[it % keyBytes.size] }
}

fun main() {
    println("hola")
    val directory = "./cifradas" // Ruta del directorio de las imágenes cifradas
    val images = File(directory).listFiles()?.filter { it.extension == "bin" } ?: return

    println("Archivos a procesar: ${images.size}")

    for (image in images) {
        println("Procesando imagen: ${image.name}")

        for (key in 0x0000..0xFFFF) {
            println("Probando clave: 0x${key.toString(16).toUpperCase()}")

            val encryptedData = image.readBytes()
            val iv = encryptedData.take(16).toByteArray() // Los primeros 16 bytes como IV
            val key128 = expandKey(key) // Expande la clave a 16 bytes

            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key128, "AES"), IvParameterSpec(iv))

            try {
                val decrypted = cipher.doFinal(encryptedData).drop(16).toByteArray() // Descifra y elimina el IV

                // Verificar si los primeros 3 bytes corresponden a la cabecera JPEG
                if (decrypted.size > 3 && decrypted[0] == 0xFF.toByte() && decrypted[1] == 0xD8.toByte() && decrypted[2] == 0xFF.toByte()) {
                    val outputFile = File(directory, "${image.nameWithoutExtension}_decrypted.jpg")
                    outputFile.writeBytes(decrypted) // Guarda la imagen descifrada
                    println("Imagen descifrada con clave 0x${key.toString(16).toUpperCase()}: ${outputFile.name}")
                    break
                } else {
                    println("Clave 0x${key.toString(16).toUpperCase()} no funciona para esta imagen")
                }
            } catch (e: Exception) {
                println("Error al descifrar con clave 0x${key.toString(16).uppercase(Locale.getDefault())}: ${e.message}")
            }
        }
    }
}