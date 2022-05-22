package backend.metal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Metal.MTLBufferProtocol
import platform.Metal.MTLResourceStorageModeManaged

// Immutable POD representing a shared CPU-GPU buffer.
data class MetalBufferPoolEntry(
    val buffer: MTLBufferProtocol,
    val capacity: ULong,
    var lastAccessed: Int,
    var referenceCount: Int = 1
)

const val TIME_BEFORE_EVICTION = 10;

// Manages a pool of Metal buffers, periodically releasing ones that have been unused for awhile.
class MetalBufferPool(val metalContext: MetalContext) {

    // Synchronizes access to mFreeStages, mUsedStages, and mutable data inside MetalBufferPoolEntrys.
    // acquireBuffer and releaseBuffer may be called on separate threads (the engine thread and a
    // Metal callback thread, for example).
    private val mMutex = Mutex()

    // Use an ordered multimap for quick (capacity => stage) lookups using lower_bound().
    private var mFreeStages = mutableSetOf<MetalBufferPoolEntry>()

    // Simple unordered set for stashing a list of in-use stages that can be reclaimed later.
    // In theory this need not exist, but is useful for validation and ensuring no leaks.
    private val mUsedStages = mutableSetOf<MetalBufferPoolEntry>();

    // Store the current "time" (really just a frame count) and LRU eviction parameters.
    private var currentFrame = 0

    // Evicts old unused buffers and bumps the current frame number.
    suspend fun gc() {
        currentFrame += 1
        // If this is one of the first few frames, return early to avoid wrapping unsigned integers.
        if (currentFrame <= TIME_BEFORE_EVICTION) {
            return
        }
        val evictionTime = currentFrame - TIME_BEFORE_EVICTION;

        mMutex.withLock {
            mFreeStages = mFreeStages.filter { it.lastAccessed >= evictionTime }
                .toMutableSet()
        }
    }

    suspend fun releaseBuffer(stage: MetalBufferPoolEntry) {
        mMutex.withLock {
            stage.referenceCount -= 1
            if (stage.referenceCount > 0) return

            val iter = mUsedStages.indexOf(stage);
            if (iter == -1) {
                error("Unknown Metal buffer: ${stage.capacity} bytes")
            }
            stage.lastAccessed = currentFrame
            mUsedStages.remove(stage)
            mFreeStages.add(stage)
        }
    }

    suspend fun acquireBuffer(numBytes: ULong): MetalBufferPoolEntry {
        mMutex.withLock {

            // First check if a stage exists whose capacity is greater than or equal to the requested size.
            return mFreeStages.firstOrNull { it.capacity >= numBytes }
                ?.let { stage ->
                    mFreeStages.remove(stage)
                    mUsedStages.add(stage)
                    stage.referenceCount += 1
                    stage
                } ?:
            // We were not able to find a sufficiently large stage, so create a new one.
            metalContext.device.newBufferWithLength(numBytes, MTLResourceStorageModeManaged)
                ?.let { buffer ->
                    val stage = MetalBufferPoolEntry(
                        buffer = buffer,
                        capacity = numBytes,
                        lastAccessed = currentFrame
                    )
                    mUsedStages.add(stage)
                    stage

                } ?: error("fail to create buffer")
        }
    }


}