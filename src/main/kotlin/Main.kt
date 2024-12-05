package org.example

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun main() {
    val iv = encryptedData.take(16).toByteArray()

    val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    cipher.init(
        Cipher.DECRYPT_MODE,
        SecretKeySpec(key128, "AES"),
        IvParameterSpec(iv)
    )
    val decrypted = cipher.doFinal(encryptedData).drop(16).toByteArray()

    fun expandKey(keyHex: Int): ByteArray {
        // Convierte el Int hexadecimal a un arreglo de bytes
        val keyBytes = ByteArray(2)
        keyBytes[0] = (keyHex shr 8 and 0xff).toByte() // Extrae el primer byte
        keyBytes[1] = (keyHex and 0xff).toByte() // Extrae el segundo byte
        // Expande los bytes a 16 bytes
        return ByteArray(16) { keyBytes[it % keyBytes.size] }
    }

}