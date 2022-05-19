package backend.metal

import platform.Metal.MTLCommandBufferProtocol
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol

@ThreadLocal
object MetalPlatform {

    private lateinit var commandQueue: MTLCommandQueueProtocol

    fun createDevice(): MTLDeviceProtocol {
        return MTLCreateSystemDefaultDevice()
            ?: error("fail to create device")
    }

    fun createCommandQueue(device: MTLDeviceProtocol ): MTLCommandQueueProtocol {
        return device.newCommandQueue()?.apply {
            label = "Filament"
            commandQueue = this
        } ?: error("fail to create command queue")
    }

    fun createAndEnqueueCommandBuffer(): MTLCommandBufferProtocol {
        return commandQueue.commandBuffer()?.apply {
            enqueue()
        } ?: error("fail to enqueue command buffer")
    }
}