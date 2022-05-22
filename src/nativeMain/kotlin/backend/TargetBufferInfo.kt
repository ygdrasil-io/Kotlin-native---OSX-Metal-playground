package backend

class TargetBufferInfo {

    // texture to be used as render target
    var handle: HwTexture? = null

    // level to be used
    var level: UShort = 0u

    // for cubemaps and 3D textures. See TextureCubemapFace for the face->layer mapping
    var layer = 0u

    companion object {
        const val MIN_SUPPORTED_RENDER_TARGET_COUNT = 4

        // When updating this, make sure to also take care of RenderTarget.java
        const val MAX_SUPPORTED_RENDER_TARGET_COUNT = 8
    }
}