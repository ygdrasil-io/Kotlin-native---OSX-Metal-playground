package backend.metal

import backend.SamplerMagFilter
import kotlinx.cinterop.CValue
import platform.Metal.MTLRegion
import platform.Metal.MTLTextureProtocol

class MetalBlitter(metalContext: MetalContext) {

    class BlitArgs(
        val filter: SamplerMagFilter
    ) {
        class  Attachment {
            var color: MTLTextureProtocol? = null
            var depth: MTLTextureProtocol? = null
            var region: CValue<MTLRegion>? = null
            val level: UShort = 0u
            val slice: UInt = 0u      // must be 0 on source attachment
        }

        // Valid source formats:       2D, 2DArray, 2DMultisample, 3D
        // Valid destination formats:  2D, 2DArray, 3D, Cube
        val source = Attachment()
        val destination = Attachment()

        fun blitColor() : Boolean {
            return source.color != null && destination.color != null
        }

        fun blitDepth() : Boolean {
            return source.depth != null && destination.depth != null
        }

        fun colorDestinationIsFullAttachment() : Boolean {
            return destination.color?.width == destination.region?.size.width &&
                    destination.color?.height == destination.region?.size.height
        }

        fun depthDestinationIsFullAttachment() : Boolean {
            return destination.depth?.width == destination.region?.size.width &&
                    destination.depth?.height == destination.region?.size.height
        }
    }

}