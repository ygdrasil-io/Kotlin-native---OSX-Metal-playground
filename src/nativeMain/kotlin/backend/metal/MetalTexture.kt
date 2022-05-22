package backend.metal

import backend.HwTexture
import backend.TextureFormat
import platform.Metal.*
import kotlin.math.max
import kotlin.math.min

class MetalTexture: HwTexture() {

    fun updateLodRange(level: UInt) {
        minLod = min(minLod, level)
        maxLod = max(maxLod, level)
    }

    var texture: MTLTextureProtocol? = null

    var minLod: UInt = UInt.MAX_VALUE
    var maxLod: UInt = 0u

}


internal fun decidePixelFormat(context: MetalContext, format: TextureFormat): MTLPixelFormat {
    val metalFormat = getMetalFormat(context, format)

    // If getMetalFormat can't find an exact match for the format, it returns MTLPixelFormatInvalid.
    if (metalFormat == MTLPixelFormatInvalid) {
        // These MTLPixelFormats are always supported.
        if (format == TextureFormat.DEPTH24_STENCIL8) return MTLPixelFormatDepth32Float_Stencil8
        if (format == TextureFormat.DEPTH16) return MTLPixelFormatDepth32Float
        if (format == TextureFormat.DEPTH24) {
            // DEPTH24 isn't supported at all by Metal. First try DEPTH24_STENCIL8. If that fails,
            // we'll fallback to DEPTH32F.
            val fallback = getMetalFormat(context, TextureFormat.DEPTH24_STENCIL8)
            if (fallback != MTLPixelFormatInvalid) return fallback
            return MTLPixelFormatDepth32Float
        }
    }

    // Metal does not natively support 3 component textures. We'll emulate support by using a 4
    // component texture and reshaping the pixel data during upload.
    return when (format) {
        TextureFormat.RGB8 ->  MTLPixelFormatRGBA8Unorm
        TextureFormat.SRGB8 ->  MTLPixelFormatRGBA8Unorm_sRGB
        TextureFormat.RGB8_SNORM ->  MTLPixelFormatRGBA8Snorm
        TextureFormat.RGB32F ->  MTLPixelFormatRGBA32Float
        TextureFormat.RGB16F ->  MTLPixelFormatRGBA16Float
        TextureFormat.RGB8UI ->  MTLPixelFormatRGBA8Uint
        TextureFormat.RGB8I ->  MTLPixelFormatRGBA8Sint
        TextureFormat.RGB16I ->  MTLPixelFormatRGBA16Sint
        TextureFormat.RGB16UI ->  MTLPixelFormatRGBA16Uint
        TextureFormat.RGB32UI ->  MTLPixelFormatRGBA32Uint
        TextureFormat.RGB32I ->  MTLPixelFormatRGBA32Sint

        else -> metalFormat
    }

}

fun getMetalFormat(context: MetalContext, format: TextureFormat) : MTLPixelFormat {
    when (format) {
        // 8-bits per element
        TextureFormat.R8 -> return MTLPixelFormatR8Unorm
        TextureFormat.R8_SNORM -> return MTLPixelFormatR8Snorm
        TextureFormat.R8UI -> return MTLPixelFormatR8Uint
        TextureFormat.R8I -> return MTLPixelFormatR8Sint
        TextureFormat.STENCIL8 -> return MTLPixelFormatStencil8

        // 16-bits per element
        TextureFormat.R16F -> return MTLPixelFormatR16Float
        TextureFormat.R16UI -> return MTLPixelFormatR16Uint
        TextureFormat.R16I -> return MTLPixelFormatR16Sint
        TextureFormat.RG8 -> return MTLPixelFormatRG8Unorm
        TextureFormat.RG8_SNORM -> return MTLPixelFormatRG8Snorm
        TextureFormat.RG8UI -> return MTLPixelFormatRG8Uint
        TextureFormat.RG8I -> return MTLPixelFormatRG8Sint

        // 24-bits per element, not supported by Metal.
        TextureFormat.RGB8, TextureFormat.SRGB8, TextureFormat.RGB8_SNORM, TextureFormat.RGB8UI, TextureFormat.RGB8I -> return MTLPixelFormatInvalid

        // 32-bits per element
        TextureFormat.R32F -> return MTLPixelFormatR32Float
        TextureFormat.R32UI -> return MTLPixelFormatR32Uint
        TextureFormat.R32I -> return MTLPixelFormatR32Sint
        TextureFormat.RG16F -> return MTLPixelFormatRG16Float
        TextureFormat.RG16UI -> return MTLPixelFormatRG16Uint
        TextureFormat.RG16I -> return MTLPixelFormatRG16Sint
        TextureFormat.R11F_G11F_B10F -> return MTLPixelFormatRG11B10Float
        TextureFormat.RGB9_E5 -> return MTLPixelFormatRGB9E5Float
        TextureFormat.RGBA8 -> return MTLPixelFormatRGBA8Unorm
        TextureFormat.SRGB8_A8 -> return MTLPixelFormatRGBA8Unorm_sRGB
        TextureFormat.RGBA8_SNORM -> return MTLPixelFormatRGBA8Snorm
        TextureFormat.RGB10_A2 -> return MTLPixelFormatRGB10A2Unorm
        TextureFormat.RGBA8UI -> return MTLPixelFormatRGBA8Uint
        TextureFormat.RGBA8I -> return MTLPixelFormatRGBA8Sint
        TextureFormat.DEPTH32F -> return MTLPixelFormatDepth32Float
        TextureFormat.DEPTH32F_STENCIL8 -> return MTLPixelFormatDepth32Float_Stencil8

        // 48-bits per element
        TextureFormat.RGB16F, TextureFormat.RGB16UI, TextureFormat.RGB16I  -> return MTLPixelFormatInvalid

        // 64-bits per element
        TextureFormat.RG32F -> return MTLPixelFormatRG32Float
        TextureFormat.RG32UI -> return MTLPixelFormatRG32Uint
        TextureFormat.RG32I -> return MTLPixelFormatRG32Sint
        TextureFormat.RGBA16F -> return MTLPixelFormatRGBA16Float
        TextureFormat.RGBA16UI -> return MTLPixelFormatRGBA16Uint
        TextureFormat.RGBA16I -> return MTLPixelFormatRGBA16Sint

        // 96-bits per element
        TextureFormat.RGB32F, TextureFormat.RGB32UI, TextureFormat.RGB32I -> return MTLPixelFormatInvalid

        // 128-bits per element
        TextureFormat.RGBA32F -> return MTLPixelFormatRGBA32Float
        TextureFormat.RGBA32UI -> return MTLPixelFormatRGBA32Uint
        TextureFormat.RGBA32I -> return MTLPixelFormatRGBA32Sint

        TextureFormat.UNUSED -> return MTLPixelFormatInvalid

    }

    // Packed 16 bit formats are only available on Apple GPUs.
    if (context.highestSupportedGpuFamily.apple >= 1) {
        when (format) {
                TextureFormat.RGB565 -> return MTLPixelFormatB5G6R5Unorm
                TextureFormat.RGB5_A1 -> return MTLPixelFormatA1BGR5Unorm
                TextureFormat.RGBA4 -> return MTLPixelFormatABGR4Unorm
            }
        }


    if (format == TextureFormat.DEPTH16) return MTLPixelFormatDepth16Unorm


    if (context.highestSupportedGpuFamily.mac >= 1 &&
    context.device.depth24Stencil8PixelFormatSupported) {
            if (format == TextureFormat.DEPTH24_STENCIL8) {
                return MTLPixelFormatDepth24Unorm_Stencil8
            }

    }
    // Only iOS 13.0 and Apple Silicon support the ASTC HDR profile. Older OS versions fallback to
    // LDR. The HDR profile is a superset of the LDR profile.
    if (context.highestSupportedGpuFamily.apple >= 2) {
            when (format) {
                TextureFormat.RGBA_ASTC_4x4 -> return MTLPixelFormatASTC_4x4_HDR
                TextureFormat.RGBA_ASTC_5x4 -> return MTLPixelFormatASTC_5x4_HDR
                TextureFormat.RGBA_ASTC_5x5 -> return MTLPixelFormatASTC_5x5_HDR
                TextureFormat.RGBA_ASTC_6x5 -> return MTLPixelFormatASTC_6x5_HDR
                TextureFormat.RGBA_ASTC_6x6 -> return MTLPixelFormatASTC_6x6_HDR
                TextureFormat.RGBA_ASTC_8x5 -> return MTLPixelFormatASTC_8x5_HDR
                TextureFormat.RGBA_ASTC_8x6 -> return MTLPixelFormatASTC_8x6_HDR
                TextureFormat.RGBA_ASTC_8x8 -> return MTLPixelFormatASTC_8x8_HDR
                TextureFormat.RGBA_ASTC_10x5 -> return MTLPixelFormatASTC_10x5_HDR
                TextureFormat.RGBA_ASTC_10x6 -> return MTLPixelFormatASTC_10x6_HDR
                TextureFormat.RGBA_ASTC_10x8 -> return MTLPixelFormatASTC_10x8_HDR
                TextureFormat.RGBA_ASTC_10x10 -> return MTLPixelFormatASTC_10x10_HDR
                TextureFormat.RGBA_ASTC_12x10 -> return MTLPixelFormatASTC_12x10_HDR
                TextureFormat.RGBA_ASTC_12x12 -> return MTLPixelFormatASTC_12x12_HDR
            }

    }

    // EAC / ETC2 formats are only available on Apple GPUs.
    if (context.highestSupportedGpuFamily.apple >= 1) {
            when (format) {
                TextureFormat.EAC_R11 -> return MTLPixelFormatEAC_R11Unorm
                TextureFormat.EAC_R11_SIGNED -> return MTLPixelFormatEAC_R11Snorm
                TextureFormat.EAC_RG11 -> return MTLPixelFormatEAC_RG11Unorm
                TextureFormat.EAC_RG11_SIGNED -> return MTLPixelFormatEAC_RG11Snorm
                TextureFormat.ETC2_RGB8 -> return MTLPixelFormatETC2_RGB8
                TextureFormat.ETC2_SRGB8 -> return MTLPixelFormatETC2_RGB8_sRGB
                TextureFormat.ETC2_RGB8_A1 -> return MTLPixelFormatETC2_RGB8A1
                TextureFormat.ETC2_SRGB8_A1 -> return MTLPixelFormatETC2_RGB8A1_sRGB
                TextureFormat.ETC2_EAC_RGBA8 -> return MTLPixelFormatEAC_RGBA8
                TextureFormat.ETC2_EAC_SRGBA8 -> return MTLPixelFormatEAC_RGBA8_sRGB
            }
        
    }

    // DXT (BC) formats are only available on macOS desktop.
    // See https://en.wikipedia.org/wiki/S3_Texture_Compression#S3TC_format_comparison
    if (context.highestSupportedGpuFamily.mac >= 1) {
        when (format) {
            TextureFormat.DXT1_RGBA -> return MTLPixelFormatBC1_RGBA
            TextureFormat.DXT1_SRGBA -> return MTLPixelFormatBC1_RGBA_sRGB
            TextureFormat.DXT3_RGBA -> return MTLPixelFormatBC2_RGBA
            TextureFormat.DXT3_SRGBA -> return MTLPixelFormatBC2_RGBA_sRGB
            TextureFormat.DXT5_RGBA -> return MTLPixelFormatBC3_RGBA
            TextureFormat.DXT5_SRGBA -> return MTLPixelFormatBC3_RGBA_sRGB

            TextureFormat.DXT1_RGB -> return MTLPixelFormatInvalid
            TextureFormat.DXT1_SRGB -> return MTLPixelFormatInvalid
        }
    }

    return MTLPixelFormatInvalid
}