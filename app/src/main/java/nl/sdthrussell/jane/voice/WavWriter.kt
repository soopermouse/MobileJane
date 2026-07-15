package nl.sdthrussell.jane.voice

import java.io.File
import java.io.RandomAccessFile

class WavWriter(private val file: File, private val sampleRate: Int) {
    private val out = RandomAccessFile(file, "rw")
    private var bytes = 0L
    init { repeat(44) { out.write(0) } }
    fun write(data: ByteArray, length: Int) { out.write(data,0,length); bytes += length }
    fun close() {
        out.seek(0); ascii("RIFF"); intLE((36+bytes).toInt()); ascii("WAVE"); ascii("fmt ")
        intLE(16); shortLE(1); shortLE(1); intLE(sampleRate); intLE(sampleRate*2)
        shortLE(2); shortLE(16); ascii("data"); intLE(bytes.toInt()); out.close()
    }
    private fun ascii(v:String)=out.write(v.toByteArray(Charsets.US_ASCII))
    private fun intLE(v:Int){ out.write(v and 255); out.write(v shr 8 and 255); out.write(v shr 16 and 255); out.write(v shr 24 and 255) }
    private fun shortLE(v:Int){ out.write(v and 255); out.write(v shr 8 and 255) }
}
