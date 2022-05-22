package backend

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//! \file


/**
 * Types and enums used by filament's driver.
 *
 * Effectively these types are public but should not be used directly. Instead use public classes
 * internal redeclaration of these types.
 * For e.g. Use Texture::Sampler instead of filament::SamplerType.
 */

const val SWAP_CHAIN_CONFIG_TRANSPARENT = 0x1;
const val SWAP_CHAIN_CONFIG_READABLE = 0x2;
const val SWAP_CHAIN_CONFIG_ENABLE_XCB = 0x4;
const val SWAP_CHAIN_CONFIG_APPLE_CVPIXELBUFFER = 0x8;

const val MAX_VERTEX_ATTRIBUTE_COUNT = 16;   // This is guaranteed by OpenGL ES.
const val MAX_VERTEX_SAMPLER_COUNT = 16;   // This is guaranteed by OpenGL ES.
const val MAX_FRAGMENT_SAMPLER_COUNT = 16;   // This is guaranteed by OpenGL ES.
const val MAX_SAMPLER_COUNT = 32;   // This is guaranteed by OpenGL ES.
const val MAX_VERTEX_BUFFER_COUNT = 16;   // Max number of bound buffer objects.

/*assert(MAX_VERTEX_BUFFER_COUNT <= MAX_VERTEX_ATTRIBUTE_COUNT,
"The number of buffer objects that can be attached to a VertexBuffer must be "
"less than or equal to the maximum number of vertex attributes.")*/

const val CONFIG_BINDING_COUNT = 12;  // This is guaranteed by OpenGL ES.

/**
 * Selects which driver a particular Engine should use.
 */
enum class Backend {
    DEFAULT,  //!< Automatically selects an appropriate driver for the platform.
    OPENGL,   //!< Selects the OpenGL/ES driver (default on Android)
    VULKAN,   //!< Selects the Vulkan driver if the platform supports it (default on Linux/Windows)
    METAL,    //!< Selects the Metal driver if the platform supports it (default on MacOS/iOS).
    NOOP     //!< Selects the no-op driver for testing purposes.
}


private infix fun UInt.or(right: TargetBufferFlags): UInt {
    return this or right.value
}

/**
 * Bitmask for selecting render buffers
 */
enum class TargetBufferFlags(val value: UInt) {
    NONE(0x0u),                            //!< No buffer selected.
    COLOR0(0x00000001u),                   //!< Color buffer selected.
    COLOR1(0x00000002u),                   //!< Color buffer selected.
    COLOR2(0x00000004u),                   //!< Color buffer selected.
    COLOR3(0x00000008u),                   //!< Color buffer selected.
    COLOR4(0x00000010u),                   //!< Color buffer selected.
    COLOR5(0x00000020u),                   //!< Color buffer selected.
    COLOR6(0x00000040u),                   //!< Color buffer selected.
    COLOR7(0x00000080u),                   //!< Color buffer selected.

    COLOR(COLOR0.value),                         //!< \deprecated
    COLOR_ALL(COLOR0 or COLOR1 or COLOR2 or COLOR3 or COLOR4 or COLOR5 or COLOR6 or COLOR7),

    DEPTH(0x10000000u),                  //!< Depth buffer selected.
    STENCIL(0x20000000u),                  //!< Stencil buffer selected.
    DEPTH_AND_STENCIL(DEPTH or STENCIL),    //!< depth and stencil buffer selected.
    ALL(COLOR_ALL or DEPTH or STENCIL);      //!< Color, depth and stencil buffer selected.

    private infix fun or(right: TargetBufferFlags): UInt {
        return value or right.value
    }

};

fun getTargetBufferFlagsAt(index: UInt): TargetBufferFlags  {
    return when (index) {
        0u -> TargetBufferFlags.COLOR0
        1u -> TargetBufferFlags.COLOR1
        2u -> TargetBufferFlags.COLOR2
        3u -> TargetBufferFlags.COLOR3
        4u -> TargetBufferFlags.COLOR4
        5u -> TargetBufferFlags.COLOR5
        6u -> TargetBufferFlags.COLOR6
        7u -> TargetBufferFlags.COLOR7
        8u -> TargetBufferFlags.DEPTH
        9u -> TargetBufferFlags.STENCIL
        else -> TargetBufferFlags.NONE
    }
}

/**
 * Frequency at which a buffer is expected to be modified and used. This is used as an hint
 * for the driver to make better decisions about managing memory internally.
 */
enum class BufferUsage {
    STATIC,      //!< content modified once, used many times
    DYNAMIC,     //!< content modified frequently, used many times
    STREAM,      //!< content invalidated and modified frequently, used many times
};

/**
 * Defines a viewport, which is the origin and extent of the clip-space.
 * All drawing is clipped to the viewport.
 */
/*data class Viewport {
    int32_t left;       //!< left coordinate in window space.
    int32_t bottom;     //!< bottom coordinate in window space.
    uint32_t width;     //!< width in pixels
    uint32_t height;    //!< height in pixels
    //! get the right coordinate in window space of the viewport
    int32_t right () const noexcept { return left + int32_t(width); }
    //! get the top coordinate in window space of the viewport
    int32_t top () const noexcept { return bottom + int32_t(height); }
};*/

/**
 * Specifies the mapping of the near and far clipping plane to window coordinates.
 */
/*data class DepthRange {
    float near = 0.0f;    //!< mapping of the near plane to window coordinates.
    float far = 1.0f;     //!< mapping of the far plane to window coordinates.
}*/

/**
 * Error codes for Fence::wait()
 * @see Fence, Fence::wait()
 */
/*enum class FenceStatus : int8_t {
    ERROR = -1,                 //!< An error occurred. The Fence condition is not satisfied.
    CONDITION_SATISFIED = 0,    //!< The Fence condition is satisfied.
    TIMEOUT_EXPIRED = 1,        //!< wait()'s timeout expired. The Fence condition is not satisfied.
}*/

/**
 * Status codes for sync objects
 */
/*enum class SyncStatus : int8_t {
    ERROR = -1,          //!< An error occurred. The Sync is not signaled.
    SIGNALED = 0,        //!< The Sync is signaled.
    NOT_SIGNALED = 1,    //!< The Sync is not signaled yet
};*/

val FENCE_WAIT_FOR_EVER = -1

/**
 * Shader model.
 *
 * These enumerants are used across all backends and refer to a level of functionality, rather
 * than to an OpenGL specific shader model.
 */
/*enum class ShaderModel : uint8_t {
    //! For testing
    UNKNOWN    = 0,
    GL_ES_30   = 1,    //!< Mobile level functionality
    GL_CORE_41 = 2,    //!< Desktop level functionality
};
const val SHADER_MODEL_COUNT = 3;*/

/**
 * Primitive types
 */
enum class PrimitiveType(value: Int) {
    // don't change the enums values (made to match GL)
    POINTS         (0),    //!< points
    LINES          (1),    //!< lines
    LINE_STRIP     (3),    //!< line strip
    TRIANGLES      (4),    //!< triangles
    TRIANGLE_STRIP (5),    //!< triangle strip
    NONE           (0xFF)
}

/**
 * Supported uniform types
 */
/*enum class UniformType : uint8_t {
    BOOL,
    BOOL2,
    BOOL3,
    BOOL4,
    FLOAT,
    FLOAT2,
    FLOAT3,
    FLOAT4,
    INT,
    INT2,
    INT3,
    INT4,
    UINT,
    UINT2,
    UINT3,
    UINT4,
    MAT3,   //!< a 3x3 float matrix
    MAT4,   //!< a 4x4 float matrix
    STRUCT
};

enum class Precision : uint8_t {
    LOW,
    MEDIUM,
    HIGH,
    DEFAULT
};*/

//! Texture sampler type
enum class SamplerType {
    SAMPLER_2D,         //!< 2D texture
    SAMPLER_2D_ARRAY,   //!< 2D array texture
    SAMPLER_CUBEMAP,    //!< Cube map texture
    SAMPLER_EXTERNAL,   //!< External texture
    SAMPLER_3D,         //!< 3D texture
}
/*

//! Subpass type
enum class SubpassType : uint8_t {
    SUBPASS_INPUT
};*/

//! Texture sampler format
enum class SamplerFormat(value: Int) {
    INT  (0),        //!< signed integer sampler
    UINT  (1),       //!< unsigned integer sampler
    FLOAT  (2),      //!< float sampler
    SHADOW  (3)      //!< shadow sampler (PCF)
}

/**
 * Supported element types
 */
enum class ElementType {
    BYTE,
    BYTE2,
    BYTE3,
    BYTE4,
    UBYTE,
    UBYTE2,
    UBYTE3,
    UBYTE4,
    SHORT,
    SHORT2,
    SHORT3,
    SHORT4,
    USHORT,
    USHORT2,
    USHORT3,
    USHORT4,
    INT,
    UINT,
    FLOAT,
    FLOAT2,
    FLOAT3,
    FLOAT4,
    HALF,
    HALF2,
    HALF3,
    HALF4,
}

/*//! Buffer object binding type
enum class BufferObjectBinding : uint8_t {
    VERTEX,
    UNIFORM
};

//! Face culling Mode
enum class CullingMode : uint8_t {
    NONE,               //!< No culling, front and back faces are visible
    FRONT,              //!< Front face culling, only back faces are visible
    BACK,               //!< Back face culling, only front faces are visible
    FRONT_AND_BACK      //!< Front and Back, geometry is not visible
};
*/
//! Pixel Data Format
enum class PixelDataFormat {
    R,                  //!< One Red channel, float
    R_INTEGER,          //!< One Red channel, integer
    RG,                 //!< Two Red and Green channels, float
    RG_INTEGER,         //!< Two Red and Green channels, integer
    RGB,                //!< Three Red, Green and Blue channels, float
    RGB_INTEGER,        //!< Three Red, Green and Blue channels, integer
    RGBA,               //!< Four Red, Green, Blue and Alpha channels, float
    RGBA_INTEGER,       //!< Four Red, Green, Blue and Alpha channels, integer
    UNUSED,             // used to be rgbm
    DEPTH_COMPONENT,    //!< Depth, 16-bit or 24-bits usually
    DEPTH_STENCIL,      //!< Two Depth (24-bits) + Stencil (8-bits) channels
    ALPHA               //! One Alpha channel, float
}
/*
//! Pixel Data Type
enum class PixelDataType : uint8_t {
    UBYTE,                //!< unsigned byte
    BYTE,                 //!< signed byte
    USHORT,               //!< unsigned short (16-bit)
    SHORT,                //!< signed short (16-bit)
    UINT,                 //!< unsigned int (32-bit)
    INT,                  //!< signed int (32-bit)
    HALF,                 //!< half-float (16-bit float)
    FLOAT,                //!< float (32-bits float)
    COMPRESSED,           //!< compressed pixels, @see CompressedPixelDataType
    UINT_10F_11F_11F_REV, //!< three low precision floating-point numbers
    USHORT_565,           //!< unsigned int (16-bit), encodes 3 RGB channels
    UINT_2_10_10_10_REV,  //!< unsigned normalized 10 bits RGB, 2 bits alpha
};

//! Compressed pixel data types
enum class CompressedPixelDataType : uint16_t {
    // Mandatory in GLES 3.0 and GL 4.3
    EAC_R11, EAC_R11_SIGNED, EAC_RG11, EAC_RG11_SIGNED,
    ETC2_RGB8, ETC2_SRGB8,
    ETC2_RGB8_A1, ETC2_SRGB8_A1,
    ETC2_EAC_RGBA8, ETC2_EAC_SRGBA8,

    // Available everywhere except Android/iOS
    DXT1_RGB, DXT1_RGBA, DXT3_RGBA, DXT5_RGBA,
    DXT1_SRGB, DXT1_SRGBA, DXT3_SRGBA, DXT5_SRGBA,

    // ASTC formats are available with a GLES extension
    RGBA_ASTC_4x4,
    RGBA_ASTC_5x4,
    RGBA_ASTC_5x5,
    RGBA_ASTC_6x5,
    RGBA_ASTC_6x6,
    RGBA_ASTC_8x5,
    RGBA_ASTC_8x6,
    RGBA_ASTC_8x8,
    RGBA_ASTC_10x5,
    RGBA_ASTC_10x6,
    RGBA_ASTC_10x8,
    RGBA_ASTC_10x10,
    RGBA_ASTC_12x10,
    RGBA_ASTC_12x12,
    SRGB8_ALPHA8_ASTC_4x4,
    SRGB8_ALPHA8_ASTC_5x4,
    SRGB8_ALPHA8_ASTC_5x5,
    SRGB8_ALPHA8_ASTC_6x5,
    SRGB8_ALPHA8_ASTC_6x6,
    SRGB8_ALPHA8_ASTC_8x5,
    SRGB8_ALPHA8_ASTC_8x6,
    SRGB8_ALPHA8_ASTC_8x8,
    SRGB8_ALPHA8_ASTC_10x5,
    SRGB8_ALPHA8_ASTC_10x6,
    SRGB8_ALPHA8_ASTC_10x8,
    SRGB8_ALPHA8_ASTC_10x10,
    SRGB8_ALPHA8_ASTC_12x10,
    SRGB8_ALPHA8_ASTC_12x12,
};*/

/** Supported texel formats
 * These formats are typically used to specify a texture's internal storage format.
 *
 * Enumerants syntax format
 * ========================
 *
 * `[components][size][type]`
 *
 * `components` : List of stored components by this format.\n
 * `size`       : Size in bit of each component.\n
 * `type`       : Type this format is stored as.\n
 *
 *
 * Name     | Component
 * :--------|:-------------------------------
 * R        | Linear Red
 * RG       | Linear Red, Green
 * RGB      | Linear Red, Green, Blue
 * RGBA     | Linear Red, Green Blue, Alpha
 * SRGB     | sRGB encoded Red, Green, Blue
 * DEPTH    | Depth
 * STENCIL  | Stencil
 *
 * \n
 * Name     | Type
 * :--------|:---------------------------------------------------
 * (none)   | Unsigned Normalized Integer [0, 1]
 * _SNORM   | Signed Normalized Integer [-1, 1]
 * UI       | Unsigned Integer @f$ [0, 2^{size}] @f$
 * I        | Signed Integer @f$ [-2^{size-1}, 2^{size-1}-1] @f$
 * F        | Floating-point
 *
 *
 * Special color formats
 * ---------------------
 *
 * There are a few special color formats that don't follow the convention above:
 *
 * Name             | Format
 * :----------------|:--------------------------------------------------------------------------
 * RGB565           |  5-bits for R and B, 6-bits for G.
 * RGB5_A1          |  5-bits for R, G and B, 1-bit for A.
 * RGB10_A2         | 10-bits for R, G and B, 2-bits for A.
 * RGB9_E5          | **Unsigned** floating point. 9-bits mantissa for RGB, 5-bits shared exponent
 * R11F_G11F_B10F   | **Unsigned** floating point. 6-bits mantissa, for R and G, 5-bits for B. 5-bits exponent.
 * SRGB8_A8         | sRGB 8-bits with linear 8-bits alpha.
 * DEPTH24_STENCIL8 | 24-bits unsigned normalized integer depth, 8-bits stencil.
 * DEPTH32F_STENCIL8| 32-bits floating-point depth, 8-bits stencil.
 *
 *
 * Compressed texture formats
 * --------------------------
 *
 * Many compressed texture formats are supported as well, which include (but are not limited to)
 * the following list:
 *
 * Name             | Format
 * :----------------|:--------------------------------------------------------------------------
 * EAC_R11          | Compresses R11UI
 * EAC_R11_SIGNED   | Compresses R11I
 * EAC_RG11         | Compresses RG11UI
 * EAC_RG11_SIGNED  | Compresses RG11I
 * ETC2_RGB8        | Compresses RGB8
 * ETC2_SRGB8       | compresses SRGB8
 * ETC2_EAC_RGBA8   | Compresses RGBA8
 * ETC2_EAC_SRGBA8  | Compresses SRGB8_A8
 * ETC2_RGB8_A1     | Compresses RGB8 with 1-bit alpha
 * ETC2_SRGB8_A1    | Compresses sRGB8 with 1-bit alpha
 *
 *
 * @see Texture
 */
enum class TextureFormat {
    // 8-bits per element
    R8, R8_SNORM, R8UI, R8I, STENCIL8,

    // 16-bits per element
    R16F, R16UI, R16I,
    RG8, RG8_SNORM, RG8UI, RG8I,
    RGB565,
    RGB9_E5, // 9995 is actually 32 bpp but it's here for historical reasons.
    RGB5_A1,
    RGBA4,
    DEPTH16,

    // 24-bits per element
    RGB8, SRGB8, RGB8_SNORM, RGB8UI, RGB8I,
    DEPTH24,

    // 32-bits per element
    R32F, R32UI, R32I,
    RG16F, RG16UI, RG16I,
    R11F_G11F_B10F,
    RGBA8, SRGB8_A8, RGBA8_SNORM,
    UNUSED, // used to be rgbm
    RGB10_A2, RGBA8UI, RGBA8I,
    DEPTH32F, DEPTH24_STENCIL8, DEPTH32F_STENCIL8,

    // 48-bits per element
    RGB16F, RGB16UI, RGB16I,

    // 64-bits per element
    RG32F, RG32UI, RG32I,
    RGBA16F, RGBA16UI, RGBA16I,

    // 96-bits per element
    RGB32F, RGB32UI, RGB32I,

    // 128-bits per element
    RGBA32F, RGBA32UI, RGBA32I,

    // compressed formats

    // Mandatory in GLES 3.0 and GL 4.3
    EAC_R11, EAC_R11_SIGNED, EAC_RG11, EAC_RG11_SIGNED,
    ETC2_RGB8, ETC2_SRGB8,
    ETC2_RGB8_A1, ETC2_SRGB8_A1,
    ETC2_EAC_RGBA8, ETC2_EAC_SRGBA8,

    // Available everywhere except Android/iOS
    DXT1_RGB, DXT1_RGBA, DXT3_RGBA, DXT5_RGBA,
    DXT1_SRGB, DXT1_SRGBA, DXT3_SRGBA, DXT5_SRGBA,

    // ASTC formats are available with a GLES extension
    RGBA_ASTC_4x4,
    RGBA_ASTC_5x4,
    RGBA_ASTC_5x5,
    RGBA_ASTC_6x5,
    RGBA_ASTC_6x6,
    RGBA_ASTC_8x5,
    RGBA_ASTC_8x6,
    RGBA_ASTC_8x8,
    RGBA_ASTC_10x5,
    RGBA_ASTC_10x6,
    RGBA_ASTC_10x8,
    RGBA_ASTC_10x10,
    RGBA_ASTC_12x10,
    RGBA_ASTC_12x12,
    SRGB8_ALPHA8_ASTC_4x4,
    SRGB8_ALPHA8_ASTC_5x4,
    SRGB8_ALPHA8_ASTC_5x5,
    SRGB8_ALPHA8_ASTC_6x5,
    SRGB8_ALPHA8_ASTC_6x6,
    SRGB8_ALPHA8_ASTC_8x5,
    SRGB8_ALPHA8_ASTC_8x6,
    SRGB8_ALPHA8_ASTC_8x8,
    SRGB8_ALPHA8_ASTC_10x5,
    SRGB8_ALPHA8_ASTC_10x6,
    SRGB8_ALPHA8_ASTC_10x8,
    SRGB8_ALPHA8_ASTC_10x10,
    SRGB8_ALPHA8_ASTC_12x10,
    SRGB8_ALPHA8_ASTC_12x12,
}

//! Bitmask describing the intended Texture Usage
enum class TextureUsage(val value: Int)  {
    NONE                (0x0),
    COLOR_ATTACHMENT    (0x1),                                  //!< Texture can be used as a color attachment
    DEPTH_ATTACHMENT    (0x2),                                  //!< Texture can be used as a depth attachment
    STENCIL_ATTACHMENT  (0x4),                                  //!< Texture can be used as a stencil attachment
    UPLOADABLE          (0x8),                                  //!< Data can be uploaded into this texture (default)
    SAMPLEABLE          (0x10),                                 //!< Texture can be sampled (default)
    SUBPASS_INPUT       (0x20),                                 //!< Texture can be used as a subpass input
    DEFAULT             (UPLOADABLE.value or SAMPLEABLE.value)  //!< Default texture usage
};
/*
//! Texture swizzle
enum class TextureSwizzle : uint8_t {
    SUBSTITUTE_ZERO,
    SUBSTITUTE_ONE,
    CHANNEL_0,
    CHANNEL_1,
    CHANNEL_2,
    CHANNEL_3
};

//! returns whether this format a depth format
static constexpr bool isDepthFormat(TextureFormat format) noexcept {
    switch(format) {
        case TextureFormat ::DEPTH32F:
        case TextureFormat ::DEPTH24:
        case TextureFormat ::DEPTH16:
        case TextureFormat ::DEPTH32F_STENCIL8:
        case TextureFormat ::DEPTH24_STENCIL8:
        return true;
        default:
        return false;
    }
}

static constexpr bool isUnsignedIntFormat(TextureFormat format) {
    switch(format) {
        case TextureFormat ::R8UI:
        case TextureFormat ::R16UI:
        case TextureFormat ::R32UI:
        case TextureFormat ::RG8UI:
        case TextureFormat ::RG16UI:
        case TextureFormat ::RG32UI:
        case TextureFormat ::RGB8UI:
        case TextureFormat ::RGB16UI:
        case TextureFormat ::RGB32UI:
        case TextureFormat ::RGBA8UI:
        case TextureFormat ::RGBA16UI:
        case TextureFormat ::RGBA32UI:
        return true;

        default:
        return false;
    }
}

static constexpr bool isSignedIntFormat(TextureFormat format) {
    switch(format) {
        case TextureFormat ::R8I:
        case TextureFormat ::R16I:
        case TextureFormat ::R32I:
        case TextureFormat ::RG8I:
        case TextureFormat ::RG16I:
        case TextureFormat ::RG32I:
        case TextureFormat ::RGB8I:
        case TextureFormat ::RGB16I:
        case TextureFormat ::RGB32I:
        case TextureFormat ::RGBA8I:
        case TextureFormat ::RGBA16I:
        case TextureFormat ::RGBA32I:
        return true;

        default:
        return false;
    }
}

//! returns whether this format a compressed format
static constexpr bool isCompressedFormat(TextureFormat format) noexcept {
    return format >= TextureFormat::EAC_R11;
}

//! returns whether this format is an ETC2 compressed format
static constexpr bool isETC2Compression(TextureFormat format) noexcept {
    return format >= TextureFormat::EAC_R11 && format <= TextureFormat::ETC2_EAC_SRGBA8;
}

//! returns whether this format is an ETC3 compressed format
static constexpr bool isS3TCCompression(TextureFormat format) noexcept {
    return format >= TextureFormat::DXT1_RGB && format <= TextureFormat::DXT5_SRGBA;
}

static constexpr bool isS3TCSRGBCompression(TextureFormat format) noexcept {
    return format >= TextureFormat::DXT1_SRGB && format <= TextureFormat::DXT5_SRGBA;
}*/

//! Texture Cubemap Face
/*enum TextureCubemapFace : uint8_t {
    // don't change the enums values
    POSITIVE_X = 0, //!< +x face
    NEGATIVE_X = 1, //!< -x face
    POSITIVE_Y = 2, //!< +y face
    NEGATIVE_Y = 3, //!< -y face
    POSITIVE_Z = 4, //!< +z face
    NEGATIVE_Z = 5, //!< -z face
};

inline int operator +(TextureCubemapFace rhs) noexcept {
    return int(rhs);
}

//! Face offsets for all faces of a cubemap
data class FaceOffsets {
    using size_type = size_t;
    union {
        struct {
            size_type px;   //!< +x face offset in bytes
            size_type nx;   //!< -x face offset in bytes
            size_type py;   //!< +y face offset in bytes
            size_type ny;   //!< -y face offset in bytes
            size_type pz;   //!< +z face offset in bytes
            size_type nz;   //!< -z face offset in bytes
        };
        size_type offsets [6];
    };
    size_type operator [](size_t n) const noexcept { return offsets[n]; }
    size_type& operator[](size_t n) { return offsets[n]; }
    FaceOffsets() noexcept = default;
    explicit FaceOffsets (size_type faceSize) noexcept {
        px = faceSize * 0;
        nx = faceSize * 1;
        py = faceSize * 2;
        ny = faceSize * 3;
        pz = faceSize * 4;
        nz = faceSize * 5;
    }
    FaceOffsets(const FaceOffsets & rhs) noexcept {
        px = rhs.px;
        nx = rhs.nx;
        py = rhs.py;
        ny = rhs.ny;
        pz = rhs.pz;
        nz = rhs.nz;
    }
    FaceOffsets& operator = (const FaceOffsets& rhs) noexcept {
        px = rhs.px;
        nx = rhs.nx;
        py = rhs.py;
        ny = rhs.ny;
        pz = rhs.pz;
        nz = rhs.nz;
        return * this;
    }
};*/

//! Sampler Wrap mode
enum class SamplerWrapMode {
    CLAMP_TO_EDGE,      //!< clamp-to-edge. The edge of the texture extends to infinity.
    REPEAT,             //!< repeat. The texture infinitely repeats in the wrap direction.
    MIRRORED_REPEAT,    //!< mirrored-repeat. The texture infinitely repeats and mirrors in the wrap direction.
};

//! Sampler minification filter
enum class SamplerMinFilter(val value: Int) {
    // don't change the enums values
    NEAREST(0),                //!< No filtering. Nearest neighbor is used.
    LINEAR(1),                 //!< Box filtering. Weighted average of 4 neighbors is used.
    NEAREST_MIPMAP_NEAREST(2), //!< Mip-mapping is activated. But no filtering occurs.
    LINEAR_MIPMAP_NEAREST(3),  //!< Box filtering within a mip-map level.
    NEAREST_MIPMAP_LINEAR(4),  //!< Mip-map levels are interpolated, but no other filtering occurs.
    LINEAR_MIPMAP_LINEAR(5)    //!< Both interpolated Mip-mapping and linear filtering are used.
};

//! Sampler magnification filter
enum class SamplerMagFilter(val value: Int)  {
    // don't change the enums values
    NEAREST(0),                //!< No filtering. Nearest neighbor is used.
    LINEAR(1)               //!< Box filtering. Weighted average of 4 neighbors is used.
};

//! Sampler compare mode
enum class SamplerCompareMode(val value: Int) {
    // don't change the enums values
    NONE(0),
    COMPARE_TO_TEXTURE(1)
};

//! comparison function for the depth / stencil sampler
enum class SamplerCompareFunc {
    // don't change the enums values
    LE, // = 0,     //!< Less or equal
    GE,         //!< Greater or equal
    L,          //!< Strictly less than
    G,          //!< Strictly greater than
    E,          //!< Equal
    NE,         //!< Not equal
    A,          //!< Always. Depth / stencil testing is deactivated.
    N           //!< Never. The depth / stencil test always fails.
};

//! Sampler paramters
data class SamplerParams(
    val u: Int = 0, // TODO: has no default value
    val filterMag: SamplerMagFilter = SamplerMagFilter.LINEAR,    //!< magnification filter (NEAREST)
    val filterMin: SamplerMinFilter = SamplerMinFilter.LINEAR_MIPMAP_NEAREST,    //!< minification filter  (NEAREST)
    val wrapS: SamplerWrapMode = SamplerWrapMode.MIRRORED_REPEAT,    //!< s-coordinate wrap mode (CLAMP_TO_EDGE)
    val wrapT: SamplerWrapMode =  SamplerWrapMode.MIRRORED_REPEAT,    //!< t-coordinate wrap mode (CLAMP_TO_EDGE)

    val wrapR: SamplerWrapMode = SamplerWrapMode.MIRRORED_REPEAT,    //!< r-coordinate wrap mode (CLAMP_TO_EDGE)
    val anisotropyLog2: Int = 3,    //!< anisotropy level (0)
    val compareMode: SamplerCompareMode = SamplerCompareMode.COMPARE_TO_TEXTURE,    //!< sampler compare mode (NONE)
    val padding0: Int = 2,    //!< reserved. must be 0.

    val compareFunc: Int = 3,    //!< sampler comparison function (LE)
    val padding1: Int = 5,    //!< reserved. must be 0.

    val padding2: Int = 8    //!< reserved. must be 0.
) {

    fun lessThan(lhs: SamplerParams, rhs: SamplerParams): Boolean {
        return lhs.u < rhs.u
    }
}

//static_assert(sizeof(SamplerParams) == sizeof(uint32_t), "SamplerParams must be 32 bits");

//! blending equation function
enum class BlendEquation {
    ADD,                    //!< the fragment is added to the color buffer
    SUBTRACT,               //!< the fragment is subtracted from the color buffer
    REVERSE_SUBTRACT,       //!< the color buffer is subtracted from the fragment
    MIN,                    //!< the min between the fragment and color buffer
    MAX                     //!< the max between the fragment and color buffer
};

//! blending function
enum class BlendFunction {
    ZERO,                   //!< f(src, dst) = 0
    ONE,                    //!< f(src, dst) = 1
    SRC_COLOR,              //!< f(src, dst) = src
    ONE_MINUS_SRC_COLOR,    //!< f(src, dst) = 1-src
    DST_COLOR,              //!< f(src, dst) = dst
    ONE_MINUS_DST_COLOR,    //!< f(src, dst) = 1-dst
    SRC_ALPHA,              //!< f(src, dst) = src.a
    ONE_MINUS_SRC_ALPHA,    //!< f(src, dst) = 1-src.a
    DST_ALPHA,              //!< f(src, dst) = dst.a
    ONE_MINUS_DST_ALPHA,    //!< f(src, dst) = 1-dst.a
    SRC_ALPHA_SATURATE      //!< f(src, dst) = (1,1,1) * min(src.a, 1 - dst.a), 1
};

//! stencil operation
enum class StencilOperation {
    KEEP,                   //!< Keeps the current value.
    ZERO,                   //!< Sets the value to 0.
    REPLACE,                //!< Sets the value to the stencil reference value.
    INCR,                   //!< Increments the current value. Clamps to the maximum representable unsigned value.
    INCR_WRAP,              //!< Increments the current value. Wraps value to zero when incrementing the maximum representable unsigned value.
    DECR,                   //!< Decrements the current value. Clamps to 0.
    DECR_WRAP,              //!< Decrements the current value. Wraps value to the maximum representable unsigned value when decrementing a value of zero.
    INVERT,                 //!< Bitwise inverts the current value.
};

//! Stream for external textures
enum class StreamType {
    NATIVE,     //!< Not synchronized but copy-free. Good for video.
    ACQUIRED,   //!< Synchronized, copy-free, and take a release callback. Good for AR but requires API 26+.
};

//! Releases an ACQUIRED external texture, guaranteed to be called on the application thread.
typealias StreamCallback = (image: Any?, user: Any?) -> Any?

//! Vertex attribute descriptor
open class Attribute {

    companion object {
        //! attribute is normalized (remapped between 0 and 1)
        const val FLAG_NORMALIZED = 0x1
        const val BUFFER_UNUSED = 0xFF
        const val FLAG_INTEGER_TARGET = 0x2
    }

    var offset = 0                    //!< attribute offset in bytes
    var stride: ULong = 0u                     //!< attribute stride in bytes
    var buffer = BUFFER_UNUSED         //!< attribute buffer index
    var type = ElementType.BYTE   //!< attribute element type
    val flags = 0x0                    //!< attribute flags


}

typealias AttributeArray = Array<Attribute>

//! Raster state descriptor
/*data class RasterState {

    using CullingMode = backend ::CullingMode;
    using DepthFunc = backend ::SamplerCompareFunc;
    using BlendEquation = backend ::BlendEquation;
    using BlendFunction = backend ::BlendFunction;
    using StencilFunction = backend ::SamplerCompareFunc;
    using StencilOperation = backend ::StencilOperation;

    RasterState() noexcept { // NOLINT
        static_assert(
            sizeof(RasterState) == sizeof(uint64_t),
            "RasterState size not what was intended"
        );
        culling = CullingMode::BACK;
        blendEquationRGB = BlendEquation::ADD;
        blendEquationAlpha = BlendEquation::ADD;
        blendFunctionSrcRGB = BlendFunction::ONE;
        blendFunctionSrcAlpha = BlendFunction::ONE;
        blendFunctionDstRGB = BlendFunction::ZERO;
        blendFunctionDstAlpha = BlendFunction::ZERO;
        stencilFunc = StencilFunction::A;
        stencilOpStencilFail = StencilOperation::KEEP;
        stencilOpDepthFail = StencilOperation::KEEP;
        stencilOpDepthStencilPass = StencilOperation::KEEP;
    }

    bool operator ==(RasterState rhs) const noexcept { return u == rhs.u; }
    bool operator !=(RasterState rhs) const noexcept { return u != rhs.u; }

    void disableBlending () noexcept {
        blendEquationRGB = BlendEquation::ADD;
        blendEquationAlpha = BlendEquation::ADD;
        blendFunctionSrcRGB = BlendFunction::ONE;
        blendFunctionSrcAlpha = BlendFunction::ONE;
        blendFunctionDstRGB = BlendFunction::ZERO;
        blendFunctionDstAlpha = BlendFunction::ZERO;
    }

    // note: clang reduces this entire function to a simple load/mask/compare
    bool hasBlending () const noexcept {
        // This is used to decide if blending needs to be enabled in the h/w
        return !(blendEquationRGB == BlendEquation::ADD &&
                blendEquationAlpha == BlendEquation::ADD &&
                blendFunctionSrcRGB == BlendFunction::ONE &&
                blendFunctionSrcAlpha == BlendFunction::ONE &&
                blendFunctionDstRGB == BlendFunction::ZERO &&
                blendFunctionDstAlpha == BlendFunction::ZERO);
    }

    union {
        struct {
            //! culling mode
            CullingMode culling : 2;        //  2

            //! blend equation for the red, green and blue components
            BlendEquation blendEquationRGB : 3;        //  5
            //! blend equation for the alpha component
            BlendEquation blendEquationAlpha : 3;        //  8

            //! blending function for the source color
            BlendFunction blendFunctionSrcRGB : 4;        // 12
            //! blending function for the source alpha
            BlendFunction blendFunctionSrcAlpha : 4;        // 16
            //! blending function for the destination color
            BlendFunction blendFunctionDstRGB : 4;        // 20
            //! blending function for the destination alpha
            BlendFunction blendFunctionDstAlpha : 4;        // 24

            //! Whether depth-buffer writes are enabled
            bool depthWrite : 1;        // 25
            //! Depth test function
            DepthFunc depthFunc : 3;        // 28

            //! Whether color-buffer writes are enabled
            bool colorWrite : 1;        // 29

            //! use alpha-channel as coverage mask for anti-aliasing
            bool alphaToCoverage : 1;        // 30

            //! whether front face winding direction must be inverted
            bool inverseFrontFaces : 1;        // 31

            //! Whether stencil-buffer writes are enabled
            bool stencilWrite : 1;        // 32
            //! Stencil reference value
            uint8_t stencilRef : 8;        // 40
            //! Stencil test function
            StencilFunction stencilFunc : 3;        // 43
            //! Stencil operation when stencil test fails
            StencilOperation stencilOpStencilFail : 3;        // 46
            //! padding, must be 0
            uint8_t padding0 : 2;        // 48
            //! Stencil operation when stencil test passes but depth test fails
            StencilOperation stencilOpDepthFail : 3;        // 51
            //! Stencil operation when both stencil and depth test pass
            StencilOperation stencilOpDepthStencilPass : 3;        // 54
            //! padding, must be 0
            uint8_t padding1 : 2;        // 56
            //! padding, must be 0
            uint8_t padding2 : 8;        // 64
        };
        uint64_t u = 0;
    };
};*/

/**
 **********************************************************************************************
 * \privatesection
 */

enum class ShaderType(value: Int) {
    VERTEX(0),
    FRAGMENT(1)
};
const val PIPELINE_STAGE_COUNT = 2;
/*
struct ShaderStageFlags {
    bool vertex : 1;
    bool fragment : 1;
    bool hasShaderType (ShaderType type) const {
        return (vertex && type == ShaderType::VERTEX) ||
                (fragment && type == ShaderType::FRAGMENT);
    }
};
static constexpr ShaderStageFlags ALL_SHADER_STAGE_FLAGS = { true, true };*/

/**
 * Selects which buffers to clear at the beginning of the render pass, as well as which buffers
 * can be discarded at the beginning and end of the render pass.
 *
 */
/*data class RenderPassFlags {
    *//**
     * bitmask indicating which buffers to clear at the beginning of a render pass.
     * This implies discard.
     *//*
    TargetBufferFlags clear;

    *//**
     * bitmask indicating which buffers to discard at the beginning of a render pass.
     * Discarded buffers have uninitialized content, they must be entirely drawn over or cleared.
     *//*
    TargetBufferFlags discardStart;

    *//**
     * bitmask indicating which buffers to discard at the end of a render pass.
     * Discarded buffers' content becomes invalid, they must not be read from again.
     *//*
    TargetBufferFlags discardEnd;
};

*//**
 * Parameters of a render pass.
 *//*
struct RenderPassParams {
    RenderPassFlags flags {};    //!< operations performed on the buffers for this pass

    Viewport viewport {};        //!< viewport for this pass
    DepthRange depthRange {};    //!< depth range for this pass

    //! Color to use to clear the COLOR buffer. RenderPassFlags::clear must be set.
    math::float4 clearColor = {};

    //! Depth value to clear the depth buffer with
    double clearDepth = 0.0;

    //! Stencil value to clear the stencil buffer with
    uint32_t clearStencil = 0;

    *//**
     * The subpass mask specifies which color attachments are designated for read-back in the second
     * subpass. If this is zero, the render pass has only one subpass. The least significant bit
     * specifies that the first color attachment in the render target is a subpass input.
     *
     * For now only 2 subpasses are supported, so only the lower 8 bits are used, one for each color
     * attachment (see MRT::MAX_SUPPORTED_RENDER_TARGET_COUNT).
     *//*
    uint16_t subpassMask = 0;

    *//**
     * This mask makes a promise to the backend about read-only usage of the depth attachment (bit
     * 0) and the stencil attachment (bit 1). Some backends need to know if writes are disabled in
     * order to allow sampling from the depth attachment.
     *//*
    uint16_t readOnlyDepthStencil = 0;

    static constexpr uint16_t READONLY_DEPTH = 1 << 0;
    static constexpr uint16_t READONLY_STENCIL = 1 << 1;
};

struct PolygonOffset {
    float slope = 0;        // factor in GL-speak
    float constant = 0;     // units in GL-speak
};*/


typealias FrameScheduledCallback = (callable: PresentCallable, user: Any) -> Unit

typealias FrameCompletedCallback = (user: Any) -> Unit

enum class Workaround {
    // The EASU pass must split because shader compiler flattens early-exit branch
    SPLIT_EASU,

    // Backend allows feedback loop with ancillary buffers (depth/stencil) as long as they're read-only for
    // the whole render pass.
    ALLOW_READ_ONLY_ANCILLARY_FEEDBACK_LOOP
};



/*
template<> struct utils::EnableBitMaskOperators<filament::backend::TargetBufferFlags>
: public std::true_type {};
template<> struct utils::EnableBitMaskOperators<filament::backend::TextureUsage>
: public std::true_type {};
*/

