package backend.metal

import backend.BufferUsage
import kotlinx.cinterop.*
import platform.posix.malloc
import platform.posix.memmove

class MetalBuffer(
    val context: MetalContext,
    usage: BufferUsage,
    private val size: ULong,
    private val forceGpuBuffer: Boolean
) {

    private var mCpuBuffer: CPointer<out CPointed>? = null
    private var mBufferPoolEntry: MetalBufferPoolEntry? = null
    val  usage: BufferUsage

    init {
        // If the buffer is less than 4K in size and is updated frequently, we don't use an explicit
        // buffer. Instead, we use immediate command encoder methods like setVertexBytes:length:atIndex:.
        if (size <= 4u * 1024u) {
            // We'll take the same approach for STREAM buffers under 4K.
            if (usage == BufferUsage.STREAM) {
                this.usage = BufferUsage.DYNAMIC;
            } else this.usage = usage;

            if (usage == BufferUsage.DYNAMIC && !forceGpuBuffer) {
                mBufferPoolEntry = null;
                mCpuBuffer = malloc(size);
            }
        } else this.usage = usage;


    }

    suspend fun copyIntoBuffer(src: CPointer<CPointed>, size: ULong, byteOffset: ULong) {
        if (size <= 0u) {
            return
        }
        assert(size + byteOffset <= size) {
            error("Attempting to copy $size bytes into a buffer of size $size at offset $byteOffset")
        }

        if (usage == BufferUsage.STREAM) {
            // byteOffset must be 0 for STREAM buffers.
            assert(byteOffset == 0.toULong())
            copyIntoStreamBuffer(src, size)
            return;
        }

        // Either copy into the Metal buffer or into our cpu buffer.
        if (mCpuBuffer != null) {
            val mCpuBufferWithOffest = interpretCPointer<CPointed>(mCpuBuffer.rawValue.plus(byteOffset.toLong()))
            memmove(mCpuBufferWithOffest, src, size);
            return;
        }

        // We're about to acquire a new buffer to hold the new contents. If we previously had obtained a
        // buffer we release it, decrementing its reference count, as we no longer needs it.
        mBufferPoolEntry?.let { context.bufferPool.releaseBuffer(it) }

        mBufferPoolEntry = context.bufferPool.acquireBuffer(size).apply {
            val contents = this.buffer.contents() ?: error("fail to get buffer")
            val mBufferPoolEntryWithOffset = interpretCPointer<CPointed>(contents.rawValue.plus(byteOffset.toLong()))
            memmove(mBufferPoolEntryWithOffset, src, size)
        }
    }

    private fun copyIntoStreamBuffer(src: CPointer<CPointed>, size: ULong) {
        TODO("Not yet implemented")
    }

}