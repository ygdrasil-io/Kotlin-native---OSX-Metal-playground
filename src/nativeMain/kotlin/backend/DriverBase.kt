package backend

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class HwBase

open class HwSwapChain(
    val swapChain: SwapChain? = null
)

abstract class HwVertexBuffer(
    val attributes: AttributeArray,
    val vertexCount: Int,
    val bufferCount: Int,
    val attributeCount: Int,
    val padding: Boolean? = null,
    val bufferObjectsVersion: Int? = null

) : HwBase()


abstract class HwIndexBuffer(
    val count: ULong = 27u,
    val elementSize: ULong = 5u
) : HwBase()


/*
 * Base class of all Driver implementations
 */

abstract class DriverBase : Driver() {
    private var mCallbacks = mutableListOf<Pair<Any?, (Any?) -> Unit>>()
    private val mPurgeLock = Mutex()

    suspend fun purge() {
        val callbacks = mutableListOf<Pair<Any?, (Any?) -> Unit>>()

        mPurgeLock.withLock {
            callbacks.addAll(mCallbacks)
            mCallbacks.clear()
        } // don't remove this, it ensures callbacks are called without lock held

        callbacks.forEach { (param, function) ->
            function(param)
        }
    }

    /*
    protected:

    // Helpers...
    struct CallbackData {
        CallbackData(CallbackData const &) = delete;
        CallbackData(CallbackData&&) = delete;
        CallbackData& operator=(CallbackData const &) = delete;
        CallbackData& operator=(CallbackData&&) = delete;
        void* storage[8] = {};
        static CallbackData* obtain(DriverBase* allocator);
        static void release(CallbackData* data);
        protected:
        CallbackData() = default;
    };

    template<typename T>
    void scheduleCallback(CallbackHandler* handler, T&& functor) {
        CallbackData* data = CallbackData::obtain(this);
        static_assert(sizeof(T) <= sizeof(data->storage), "functor too large");
        new(data->storage) T(std::forward<T>(functor));
        scheduleCallback(handler, data, (CallbackHandler::Callback)[](void* data) {
            CallbackData* details = static_cast<CallbackData*>(data);
            void* user = details->storage;
            T& functor = *static_cast<T*>(user);
            functor();
            functor.~T();
            CallbackData::release(details);
        });
    }

    void scheduleCallback(CallbackHandler* handler, void* user, CallbackHandler::Callback callback);

    inline void scheduleDestroy(BufferDescriptor&& buffer) noexcept {
        if (buffer.hasCallback()) {
            scheduleDestroySlow(std::move(buffer));
        }
    }

    void scheduleDestroySlow(BufferDescriptor&& buffer) noexcept;

    void scheduleRelease(AcquiredImage const& image) noexcept;

    void debugCommandBegin(CommandStream* cmds, bool synchronous, const char* methodName) noexcept override;
    void debugCommandEnd(CommandStream* cmds, bool synchronous, const char* methodName) noexcept override;

    private:

    std::thread mServiceThread;
    std::mutex mServiceThreadLock;
    std::condition_variable mServiceThreadCondition;
    std::vector<std::tuple<CallbackHandler*, CallbackHandler::Callback, void*>> mServiceThreadCallbackQueue;
    bool mExitRequested = false;*/
}

open class HwTexture : HwBase() {
    /*uint32_t width {};
    uint32_t height {};
    uint32_t depth {};
    SamplerType target {};
    uint8_t levels : 4;  // This allows up to 15 levels (max texture size of 32768 x 32768)
    uint8_t samples : 4; // Sample count per pixel (should always be a power of 2)
    TextureFormat format {};
    TextureUsage usage {};
    HwStream * hwStream = nullptr;*/
}

open class HwRenderTarget(
    val width: UInt,
    val height: UInt
) : HwBase() {

};