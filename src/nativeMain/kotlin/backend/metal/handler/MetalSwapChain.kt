package backend.metal

import backend.*
import kotlinx.cinterop.useContents
import platform.Metal.*
import platform.QuartzCore.CAMetalDrawableProtocol
import platform.QuartzCore.CAMetalLayer

class MetalSwapChain(metalContext: MetalContext, nativeWindow: CAMetalLayer, flags: ULong) : HwSwapChain() {

    enum class SwapChainType {
        CAMETALLAYER,
        CVPIXELBUFFERREF,
        HEADLESS
    }

    fun isCaMetalLayer() = type == SwapChainType.CAMETALLAYER
    fun isHeadless()  =  type == SwapChainType.HEADLESS
    fun isPixelBuffer() = type == SwapChainType.CVPIXELBUFFERREF

    // These two fields store a callback and user data to notify the client that a frame is ready
    // for presentation.
    // If frameScheduledCallback is nullptr, then the Metal backend automatically calls
    // presentDrawable when the frame is commited.
    // Otherwise, the Metal backend will not automatically present the frame. Instead, clients bear
    // the responsibility of presenting the frame by calling the PresentCallable object.
    private var frameCompletedUserData: Any? = null
    private var frameCompletedCallback: FrameCompletedCallback? = null

    private var frameScheduledUserData: Any? = null
    private var frameScheduledCallback: FrameScheduledCallback? = null

    private var context: MetalContext
    private var drawable: CAMetalDrawableProtocol? = null
    private var depthTexture: MTLTextureProtocol? = null
    private var headlessDrawable: MTLTextureProtocol? = null
    private var headlessWidth: UInt = 0u
    private var headlessHeight: UInt = 0u
    private var layer: CAMetalLayer? = null
    private var externalImage: MetalExternalImage
    private var type: SwapChainType

    fun setFrameScheduledCallback(callback: FrameScheduledCallback, user: Any) {
        frameScheduledCallback = callback
        frameScheduledUserData = user

    }

    fun setFrameCompletedCallback(callback: FrameCompletedCallback, user: Any) {
        frameCompletedCallback = callback
        frameCompletedUserData = user
    }

    fun releaseDrawable() {
        drawable = null
    }

    fun getSurfaceWidth() : UInt {
        if (isHeadless()) {
            return headlessWidth
        }
        if (isPixelBuffer()) {
            return externalImage.getWidth()
        }
        return layer?.drawableSize?.width?.toUInt() ?: error("")
    }

    fun getSurfaceHeight() : UInt {
        if (isHeadless()) {
            return headlessHeight
        }
        if (isPixelBuffer()) {
            return externalImage.getHeight()
        }
        return layer?.drawableSize.height?.toUInt() ?: error("")
    }

    init {
        context = metalContext
        layer = nativeWindow
        externalImage = MetalExternalImage(metalContext)
        type = SwapChainType.CAMETALLAYER

        if ((flags and SWAP_CHAIN_CONFIG_TRANSPARENT) != 0u.toULong() && !nativeWindow.opaque) {
            println("Warning: Filament SwapChain has no CONFIG_TRANSPARENT flag, but the CAMetaLayer($nativeWindow) has .opaque set to NO.")
        }
        if ((flags and SWAP_CHAIN_CONFIG_TRANSPARENT) > 0u && nativeWindow.opaque) {
            println("Warning: Filament SwapChain has the CONFIG_TRANSPARENT flag, but the CAMetaLayer($nativeWindow)  has .opaque set to YES.")
        }

        // Needed so we can use the SwapChain as a blit source.
        // Also needed for rendering directly into the SwapChain during refraction.
        nativeWindow.framebufferOnly = false

        layer?.device = context.device
    }


    fun acquireDrawable(): MTLTextureProtocol {
        val drawable = drawable
        if (drawable != null) {
            return drawable.texture
        }

        if (isHeadless()) {
            if (headlessDrawable == null) {
                // For headless surfaces we construct a "fake" drawable, which is simply a renderable
                // texture.
                val textureDescriptor = MTLTextureDescriptor()
                textureDescriptor.pixelFormat = MTLPixelFormatBGRA8Unorm
                textureDescriptor.width = headlessWidth.toULong()
                textureDescriptor.height = headlessHeight.toULong()
                // Specify MTLTextureUsageShaderRead so the headless surface can be blitted from.
                textureDescriptor.usage = MTLTextureUsageRenderTarget or MTLTextureUsageShaderRead
                textureDescriptor.storageMode = MTLStorageModeManaged
                headlessDrawable = context.device.newTextureWithDescriptor(textureDescriptor)
            }

            return headlessDrawable ?: error("Could not obtain drawable.")
        }

        if (isPixelBuffer()) {
            return externalImage.getMetalTextureForDraw()
        }

        this.drawable = layer?.nextDrawable()

        return drawable?.let { it.texture }
            ?: error("Could not obtain drawable.")
    }

}