package backend

import kotlinx.coroutines.sync.Mutex

const val EXIT_REQUESTED = 0x31415926

class CommandBufferQueue(
    requiredSize: Int,
    bufferSize: Int
) {

    var circularBuffer: CircularBuffer = CircularBuffer(bufferSize)
    val mRequiredSize: Int = requiredSize + circularBuffer.BLOCK_MASK and circularBuffer.BLOCK_MASK.inv()

    val mFreeSpace = circularBuffer.size()

    data class Slice(
        val begin: Any, val end: Any
    )


    // space available in the circular buffer

    var mLock = Mutex()
    var mCondition: Condition? = null
    var mCommandBuffersToExecute: Array<Slice>? = null
    val mHighWatermark = 0
    val mExitRequested = 0


}