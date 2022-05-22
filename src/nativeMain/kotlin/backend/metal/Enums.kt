package backend.metal

import backend.ElementType
import platform.Metal.MTLVertexFormat
import platform.Metal.MTLVertexFormatInvalid


fun  getMetalFormat(type: ElementType, normalized: Boolean) : MTLVertexFormat {
    if (normalized) {
        return when (type) {
            // Single Component Types
             ElementType.BYTE -> MTLVertexFormatCharNormalized
             ElementType.UBYTE -> MTLVertexFormatUCharNormalized
             ElementType.SHORT -> MTLVertexFormatShortNormalized
             ElementType.USHORT -> MTLVertexFormatUShortNormalized

            // Two Component Types
             ElementType.BYTE2 -> MTLVertexFormatChar2Normalized
             ElementType.UBYTE2 -> MTLVertexFormatUChar2Normalized
             ElementType.SHORT2 -> MTLVertexFormatShort2Normalized
             ElementType.USHORT2 -> MTLVertexFormatUShort2Normalized
            // Three Component Types
             ElementType.BYTE3 -> MTLVertexFormatChar3Normalized
             ElementType.UBYTE3 -> MTLVertexFormatUChar3Normalized
             ElementType.SHORT3 -> MTLVertexFormatShort3Normalized
             ElementType.USHORT3 -> MTLVertexFormatUShort3Normalized
            // Four Component Types
             ElementType.BYTE4 -> MTLVertexFormatChar4Normalized
             ElementType.UBYTE4 -> MTLVertexFormatUChar4Normalized
             ElementType.SHORT4 -> MTLVertexFormatShort4Normalized
             ElementType.USHORT4 -> MTLVertexFormatUShort4Normalized
            else ->MTLVertexFormatInvalid
        }
    }
    return when (type) {
        // Single Component Types
         ElementType.BYTE -> MTLVertexFormatChar
         ElementType.UBYTE -> MTLVertexFormatUChar
         ElementType.SHORT -> MTLVertexFormatShort
         ElementType.USHORT -> MTLVertexFormatUShort
         ElementType.HALF -> MTLVertexFormatHalf
         ElementType.INT -> MTLVertexFormatInt
         ElementType.UINT -> MTLVertexFormatUInt
         ElementType.FLOAT -> MTLVertexFormatFloat
        // Two Component Types
         ElementType.BYTE2 -> MTLVertexFormatChar2
         ElementType.UBYTE2 -> MTLVertexFormatUChar2
         ElementType.SHORT2 -> MTLVertexFormatShort2
         ElementType.USHORT2 -> MTLVertexFormatUShort2
         ElementType.HALF2 -> MTLVertexFormatHalf2
         ElementType.FLOAT2 -> MTLVertexFormatFloat2
        // Three Component Types
         ElementType.BYTE3 -> MTLVertexFormatChar3
         ElementType.UBYTE3 -> MTLVertexFormatUChar3
         ElementType.SHORT3 -> MTLVertexFormatShort3
         ElementType.USHORT3 -> MTLVertexFormatUShort3
         ElementType.HALF3 -> MTLVertexFormatHalf3
         ElementType.FLOAT3 -> MTLVertexFormatFloat3
        // Four Component Types
         ElementType.BYTE4 -> MTLVertexFormatChar4
         ElementType.UBYTE4 -> MTLVertexFormatUChar4
         ElementType.SHORT4 -> MTLVertexFormatShort4
         ElementType.USHORT4 -> MTLVertexFormatUShort4
         ElementType.HALF4 -> MTLVertexFormatHalf4
         ElementType.FLOAT4 -> MTLVertexFormatFloat4
        else ->MTLVertexFormatInvalid
    }
}