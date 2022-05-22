package backend

import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.sizeOf

abstract class Driver {

    abstract val dispatcher: Dispatcher

    abstract suspend fun updateIndexBuffer(ibh: HwIndexBuffer, data: BufferDescriptor, byteOffset: Int)

    abstract fun createRenderPrimitive(): HwRenderPrimitive

    abstract fun setRenderPrimitiveBuffer(rph: HwRenderPrimitive, vbh: HwVertexBuffer, ibh: HwIndexBuffer)

    abstract fun setRenderPrimitiveRange(
        rph: HwRenderPrimitive,
        pt: PrimitiveType,
        offset: ULong,
        minIndex: Int,
        maxIndex: Int,
        count: Int
    )

    abstract fun isTextureFormatSupported(format: TextureFormat): Boolean

    abstract fun isTextureFormatMipmappable(format: TextureFormat): Boolean

    abstract fun canGenerateMipmaps(): Boolean

    abstract fun generateMipmaps(mHandle: HwTexture)

    abstract fun createRenderTarget(
        targetBufferFlags: TargetBufferFlags, width: UInt, height: UInt,
        samples: UShort, color: MRT, depth: TargetBufferInfo = TargetBufferInfo(), stencil: TargetBufferInfo = TargetBufferInfo()
    ): HwRenderTarget
}


fun getElementTypeSize(type: ElementType): ULong {
    return when (type) {
        ElementType.FLOAT -> sizeOf<FloatVar>()
        ElementType.FLOAT2 -> sizeOf<FloatVar>() * 2
        ElementType.FLOAT3 -> sizeOf<FloatVar>() * 3
        ElementType.FLOAT4 -> sizeOf<FloatVar>() * 4

        else -> TODO("not yet implemented")
    }.toULong()

    /*return when (type) {
         ElementType.BYTE->     sizeof(int8_t)
         ElementType.BYTE2->    sizeof(byte2)
         ElementType.BYTE3->    sizeof(byte3)
         ElementType.BYTE4->    sizeof(byte4)
         ElementType.UBYTE->    sizeof(uint8_t)
         ElementType.UBYTE2->   sizeof(ubyte2)
         ElementType.UBYTE3->   sizeof(ubyte3)
         ElementType.UBYTE4->   sizeof(ubyte4)
         ElementType.SHORT->    sizeof(int16_t)
         ElementType.SHORT2->   sizeof(short2)
         ElementType.SHORT3->   sizeof(short3)
         ElementType.SHORT4->   sizeof(short4)
         ElementType.USHORT->   sizeof(uint16_t)
         ElementType.USHORT2->  sizeof(ushort2)
         ElementType.USHORT3->  sizeof(ushort3)
         ElementType.USHORT4->  sizeof(ushort4)
         ElementType.INT->      sizeof(int32_t)
         ElementType.UINT->     sizeof(uint32_t)
         ElementType.FLOAT->    sizeof(float)
         ElementType.FLOAT2->   sizeof(float2)
         ElementType.FLOAT3->   sizeof(float3)
         ElementType.FLOAT4->   sizeof(float4)
         ElementType.HALF->     sizeof(half)
         ElementType.HALF2->    sizeof(half2)
         ElementType.HALF3->    sizeof(half3)
         ElementType.HALF4->    sizeof(half4)
    }*/
}