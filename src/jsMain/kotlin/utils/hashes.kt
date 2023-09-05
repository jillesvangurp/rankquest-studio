package utils

import korlibs.crypto.MD5
import korlibs.crypto.encoding.base64

fun md5Hash(vararg components:Any?): String {
    return with(MD5.create()) {
        components.filterNotNull().forEach {
            update(it.toString().encodeToByteArray())
        }
        val bytesOut = ByteArray(digestSize)
        digestOut(bytesOut)
        bytesOut.base64
    }
}