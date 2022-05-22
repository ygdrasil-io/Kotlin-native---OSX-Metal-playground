package backend

class CircularBuffer(val bufferSize: Int) {

    val BLOCK_BITS = 12;    // 4KB
    val BLOCK_SIZE = 1 shl BLOCK_BITS;
    val BLOCK_MASK = BLOCK_SIZE - 1

    fun size(): Int {
        return bufferSize
    }
}