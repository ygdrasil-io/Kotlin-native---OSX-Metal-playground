package backend.metal

import platform.CoreVideo.CVBufferRetain
import platform.CoreVideo.CVMetalTextureRef
import platform.CoreVideo.CVPixelBufferRef
import platform.CoreVideo.CVPixelBufferRetain
import platform.Metal.MTLTextureProtocol

class MetalExternalImage(
    val metalContext: MetalContext
) {

    // If the external image has a single plane, mImage and mTexture hold references to the image
    // and created Metal texture, respectively.
    // mTextureView is a view of mTexture with any swizzling applied.
    var mImage: CVPixelBufferRef? = null
    var mTexture: CVMetalTextureRef? = null
    var mTextureView: MTLTextureProtocol? = null
    var mWidth = 0u
    var mHeight = 0u

    // If the external image is in the YCbCr format, this holds the result of the converted RGB
    // texture.
    var mRgbTexture: MTLTextureProtocol = null

    fun getMetalTextureForDraw() : MTLTextureProtocol {
        val mRgbTexture = mRgbTexture
        if (mRgbTexture != null) {
            return mRgbTexture
        }

        // Retain the image and Metal texture until the GPU has finished with this frame. This does
        // not need to be done for the RGB texture, because it is an Objective-C object whose
        // lifetime is automatically managed by Metal.
        val tracker = metalContext.resourceTracker
        val commandBuffer = metalContext.pendingCommandBuffer
        if (tracker.trackResource( commandBuffer, mImage, cvBufferDeleter)) {
            CVPixelBufferRetain(mImage)
        }
        if (tracker.trackResource( commandBuffer, mTexture, cvBufferDeleter)) {
            CVBufferRetain(mTexture)
        }

        return mTextureView ?: error("")
    }
}