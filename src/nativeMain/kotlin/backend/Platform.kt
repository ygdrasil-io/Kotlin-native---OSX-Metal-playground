package backend

abstract class SwapChain {

    val CONFIG_TRANSPARENT = SWAP_CHAIN_CONFIG_TRANSPARENT;
    /**
     * This flag indicates that the swap chain may be used as a source surface
     * for reading back render results.  This config must be set when creating
     * any swap chain that will be used as the source for a blit operation.
     *
     * @see
     * Renderer.copyFrame()
     */
    val CONFIG_READABLE = SWAP_CHAIN_CONFIG_READABLE;

    /**
     * Indicates that the native X11 window is an XCB window rather than an XLIB window.
     * This is ignored on non-Linux platforms and in builds that support only one X11 API.
     */
    val CONFIG_ENABLE_XCB = SWAP_CHAIN_CONFIG_ENABLE_XCB;

    /**
     * Indicates that the native window is a CVPixelBufferRef.
     *
     * This is only supported by the Metal backend. The CVPixelBuffer must be in the
     * kCVPixelFormatType_32BGRA format.
     *
     * It is not necessary to add an additional retain call before passing the pixel buffer to
     * Filament. Filament will call CVPixelBufferRetain during Engine::createSwapChain, and
     * CVPixelBufferRelease when the swap chain is destroyed.
     */
    val CONFIG_APPLE_CVPIXELBUFFER = SWAP_CHAIN_CONFIG_APPLE_CVPIXELBUFFER;
}