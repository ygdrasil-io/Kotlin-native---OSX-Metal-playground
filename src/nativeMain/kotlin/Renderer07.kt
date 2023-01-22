import interop.insert
import kotlinx.cinterop.*
import org.jetbrains.kotlinx.multik.api.identity
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import platform.Foundation.NSError
import platform.Foundation.NSMakeRange
import platform.Metal.*
import platform.MetalKit.MTKView
import platform.darwin.*
import platform.posix.memcpy
import std.unreachable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Renderer07(device: MTLDeviceProtocol) : Renderer(device) {
    private val kInstanceRows = 10
    private val kInstanceColumns = 10
    private val kInstanceDepth = 10
    private val kNumInstances = (kInstanceRows * kInstanceColumns * kInstanceDepth)
    private val kMaxFramesInFlight = 3L

    private lateinit var depthStencilState: MTLDepthStencilStateProtocol
    private lateinit var library: MTLLibraryProtocol
    private lateinit var renderPipelineStateProtocol: MTLRenderPipelineStateProtocol
    private lateinit var instanceDataBuffer: List<MTLBufferProtocol>
    private lateinit var cameraDataBuffer: List<MTLBufferProtocol>
    private lateinit var pVertexBuffer: MTLBufferProtocol
    private lateinit var pIndexBuffer: MTLBufferProtocol
    private lateinit var texture: MTLTextureProtocol

    private var frame = 0
    private var angle = 0f
    private var semaphore: dispatch_semaphore_t

    class CameraData(rawPtr: NativePtr) : CStructVar(rawPtr) {

        val perspectiveTransform: simd_float4x4
            get() = interpretOpaquePointed(rawPtr).reinterpret()
        val worldTransform: simd_float4x4
            get() = rawPtr.plus(sizeOf<simd_float4x4>())
                .let(::interpretOpaquePointed)
                .let(NativePointed::reinterpret)

        val worldNormalTransform: simd_float3x3
            get() = rawPtr.plus(sizeOf<simd_float4x4>() * 2)
                .let(::interpretOpaquePointed)
                .let(NativePointed::reinterpret)

        @Suppress("DEPRECATION")
        companion object : Type(
            sizeOf<FloatVar>() * (4*4*2 + 3*3),
            alignOf<FloatVar>() * (4*4*2 + 3*3)
        )
    }

    class InstanceData(rawPtr: NativePtr) : CStructVar(rawPtr) {
        val instanceTransform: simd_float4x4
            get() = interpretOpaquePointed(rawPtr).reinterpret()
        val instanceNormalTransform: simd_float3x3
            get() = rawPtr.plus(sizeOf<simd_float4x4>())
                .let(::interpretOpaquePointed)
                .let(NativePointed::reinterpret)

        val instanceColor: simd_float3Var
            get() = rawPtr.plus(sizeOf<simd_float4x4>() + sizeOf<simd_float3x3>())
                .let(::interpretOpaquePointed)
                .let(NativePointed::reinterpret)

        @Suppress("DEPRECATION")
        companion object : Type(
            sizeOf<FloatVar>() * (4*4 + 3*3 + 3),
            alignOf<FloatVar>() * (4*4 + 3*3 + 3)
        )
    }

    init {
        buildShaders()
        buildDepthStencilStates()
        buildTextures()
        buildBuffers()

        semaphore = dispatch_semaphore_create(kMaxFramesInFlight) ?: error("fail to create semaphore")
    }

    override fun drawOnView(view: MTKView) = autoreleasepool {
        frame = (frame + 1) % kMaxFramesInFlight.toInt()
        val pInstanceDataBuffer = instanceDataBuffer[frame]
        val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        commandBuffer.addCompletedHandler {
            dispatch_semaphore_signal(semaphore)
        }

        angle += 0.002f

        val scl = 0.2f

        println("test ${pInstanceDataBuffer.contents().rawValue}")
        val pInstanceDataList = (pInstanceDataBuffer.contents() ?: unreachable())
            .reinterpret<InstanceData>()

        val objectPosition = listOf(0f, 0f, -10f)

        val rt = makeTranslate(objectPosition)
        val rr1 = makeYRotate(-angle)
        val rr0 = makeXRotate(angle * 0.5f)
        val rtInv = makeTranslate(listOf(-objectPosition[0], -objectPosition[1], -objectPosition[2]))
        val fullObjectRot = rt * rr1 * rr0 * rtInv

        var ix = 0f
        var iy = 0f
        var iz = 0f
        for (i in 0 until kNumInstances) {
            println("$i")
            if (ix.toInt() == kInstanceRows) {
                ix = 0f
                iy += 1f
            }
            if (iy.toInt() == kInstanceRows) {
                iy = 0f
                iz += 1f
            }

            val scale = makeScale(listOf(scl, scl, scl))
            val zrot = makeZRotate(angle * sin(ix))
            val yrot = makeYRotate(angle * cos(iy))

            val x = (ix - kInstanceRows / 2f) * (2f * scl) + scl
            val y = (iy - kInstanceColumns / 2f) * (2f * scl) + scl
            val z = (iz - kInstanceDepth / 2f) * (2f * scl)
            val translate = makeTranslate(add(objectPosition, listOf(x, y, z)))

            val instanceData = pInstanceDataList[i]

            val instanceTransform = (fullObjectRot * translate * yrot * zrot * scale)
            println("test ${instanceData.instanceTransform.rawPtr}")
            instanceData.instanceTransform.insert(instanceTransform)
            println("test")
            instanceData.instanceNormalTransform.insert(discardTranslation(instanceTransform))


            println("test")

            val iDivNumInstances = i / kNumInstances.toFloat()
            val r = iDivNumInstances
            val g = 1f - r
            val b = sin(PI * 2.0f * iDivNumInstances).toFloat()
            instanceData.instanceColor.insert(listOf(r, g, b, 1f))

            println("test")
            ix += 1
        }

        println("test last")
        pInstanceDataBuffer.didModifyRange(NSMakeRange(0, pInstanceDataBuffer.length()))

        // Update camera state:

        val pCameraDataBuffer = cameraDataBuffer[frame]
        val pCameraData = (pCameraDataBuffer.contents()?.reinterpret<CameraData>()
            ?: unreachable())
            .let { it[0L] }
        pCameraData.perspectiveTransform.insert(makePerspective((45f * PI / 180f).toFloat(), 1f, 0.03f, 500f))
        val worldTransform = makeIdentity()
        pCameraData.worldTransform.insert(worldTransform)
        pCameraData.worldNormalTransform.insert(discardTranslation(worldTransform))
        pCameraDataBuffer.didModifyRange(NSMakeRange(0, sizeOf<CameraData>().toULong()))

        // Begin render pass:

        val pRpd = view.currentRenderPassDescriptor() ?: unreachable()
        val pEnc = commandBuffer.renderCommandEncoderWithDescriptor(pRpd) ?: unreachable()

        pEnc.setRenderPipelineState(renderPipelineStateProtocol)
        pEnc.setDepthStencilState(depthStencilState)

        pEnc.setVertexBuffer(pVertexBuffer, 0, 0)
        pEnc.setVertexBuffer(pInstanceDataBuffer, 0, 1)
        pEnc.setVertexBuffer(pCameraDataBuffer, 0, 2)

        pEnc.setFragmentTexture(texture, 0)

        pEnc.setCullMode(MTLCullModeBack)
        pEnc.setFrontFacingWinding(MTLWindingCounterClockwise)


        pEnc.drawIndexedPrimitives(
            MTLPrimitiveTypeTriangle,
            6uL * 6uL,
            MTLIndexTypeUInt16,
            pIndexBuffer,
            0uL,
            kNumInstances.toULong()
        )


        pEnc.endEncoding()
        commandBuffer.presentDrawable(view.currentDrawable() ?: unreachable())
        commandBuffer.commit()
    }

    private fun buildBuffers() = memScoped {
        println("buildBuffers")

        val s = 0.5f

        val verts = listOf(
            //                                         Texture
            //   Positions           Normals         Coordinates
            listOf(listOf(-s, -s, +s), listOf(0f, 0f, 1.0f), listOf(0f, 1.0f)),
            listOf(listOf(+s, -s, +s), listOf(0f, 0f, 1.0f), listOf(1.0f, 1.0f)),
            listOf(listOf(+s, +s, +s), listOf(0f, 0f, 1.0f), listOf(1.0f, 0f)),
            listOf(listOf(-s, +s, +s), listOf(0f, 0f, 1.0f), listOf(0f, 0f)),

            listOf(listOf(+s, -s, +s), listOf(1.0f, 0f, 0f), listOf(0f, 1.0f)),
            listOf(listOf(+s, -s, -s), listOf(1.0f, 0f, 0f), listOf(1.0f, 1.0f)),
            listOf(listOf(+s, +s, -s), listOf(1.0f, 0f, 0f), listOf(1.0f, 0f)),
            listOf(listOf(+s, +s, +s), listOf(1.0f, 0f, 0f), listOf(0f, 0f)),

            listOf(listOf(+s, -s, -s), listOf(0f, 0f, -1.0f), listOf(0f, 1.0f)),
            listOf(listOf(-s, -s, -s), listOf(0f, 0f, -1.0f), listOf(1.0f, 1.0f)),
            listOf(listOf(-s, +s, -s), listOf(0f, 0f, -1.0f), listOf(1.0f, 0f)),
            listOf(listOf(+s, +s, -s), listOf(0f, 0f, -1.0f), listOf(0f, 0f)),

            listOf(listOf(-s, -s, -s), listOf(-1.0f, 0f, 0f), listOf(0f, 1.0f)),
            listOf(listOf(-s, -s, +s), listOf(-1.0f, 0f, 0f), listOf(1.0f, 1.0f)),
            listOf(listOf(-s, +s, +s), listOf(-1.0f, 0f, 0f), listOf(1.0f, 0f)),
            listOf(listOf(-s, +s, -s), listOf(-1.0f, 0f, 0f), listOf(0f, 0f)),

            listOf(listOf(-s, +s, +s), listOf(0f, 1.0f, 0f), listOf(0f, 1.0f)),
            listOf(listOf(+s, +s, +s), listOf(0f, 1.0f, 0f), listOf(1.0f, 1.0f)),
            listOf(listOf(+s, +s, -s), listOf(0f, 1.0f, 0f), listOf(1.0f, 0f)),
            listOf(listOf(-s, +s, -s), listOf(0f, 1.0f, 0f), listOf(0f, 0f)),

            listOf(listOf(-s, -s, -s), listOf(0f, -1.0f, 0f), listOf(0f, 1.0f)),
            listOf(listOf(+s, -s, -s), listOf(0f, -1.0f, 0f), listOf(1.0f, 1.0f)),
            listOf(listOf(+s, -s, +s), listOf(0f, -1.0f, 0f), listOf(1.0f, 0f)),
            listOf(listOf(-s, -s, +s), listOf(0f, -1.0f, 0f), listOf(0f, 0f))
        ).flatten().flatten()

        val indices = listOf(
            0, 1, 2, 2, 3, 0, /* front */
            4, 5, 6, 6, 7, 4, /* right */
            8, 9, 10, 10, 11, 8, /* back */
            12, 13, 14, 14, 15, 12, /* left */
            16, 17, 18, 18, 19, 16, /* top */
            20, 21, 22, 22, 23, 20, /* bottom */
        )

        val vertexDataSize = (sizeOf<CPointerVar<FloatVar>>() * verts.size).toULong()
        val indexDataSize = (sizeOf<CPointerVar<IntVar>>() * indices.size).toULong()

        pVertexBuffer =
            device.newBufferWithLength(vertexDataSize, MTLResourceStorageModeManaged) ?: error("fail to create buffer")
        pIndexBuffer =
            device.newBufferWithLength(indexDataSize, MTLResourceStorageModeManaged) ?: error("fail to create buffer")


        memcpy(pVertexBuffer.contents(), allocArrayOf(verts), vertexDataSize)
        memcpy(pIndexBuffer.contents(), allocArrayOf(indices), indexDataSize)

        pVertexBuffer.didModifyRange(NSMakeRange(0, pVertexBuffer.length()))
        pIndexBuffer.didModifyRange(NSMakeRange(0, pIndexBuffer.length()))

        val instanceDataSize = (kMaxFramesInFlight * kNumInstances * sizeOf<InstanceData>()).toULong()
        println("instanceDataSize $instanceDataSize")
        instanceDataBuffer = (0 until kMaxFramesInFlight).map {
            device.newBufferWithLength(instanceDataSize, MTLResourceStorageModeManaged) ?: unreachable()
        }

        val cameraDataSize = (kMaxFramesInFlight * sizeOf<CameraData>()).toULong()
        println("cameraDataSize $cameraDataSize")
        cameraDataBuffer = (0 until kMaxFramesInFlight).map {
            device.newBufferWithLength(cameraDataSize, MTLResourceStorageModeManaged) ?: unreachable()
        }

    }

    private fun buildTextures() = memScoped {
        println("buildTextures")

        val tw = 128uL
        val th = 128uL

        val mtlTextureDescriptor = MTLTextureDescriptor()
        mtlTextureDescriptor.width = tw
        mtlTextureDescriptor.height = th
        mtlTextureDescriptor.pixelFormat = MTLPixelFormatA8Unorm
        mtlTextureDescriptor.textureType = MTLTextureType2D
        mtlTextureDescriptor.storageMode = MTLStorageModeManaged
        mtlTextureDescriptor.usage = MTLResourceUsageSample or MTLResourceUsageRead

        texture = device.newTextureWithDescriptor(mtlTextureDescriptor) ?: error("fail to create texture")

        val pTextureData = allocArray<IntVar>((tw * th * 4u).toInt())
        for (y in (0 until th.toInt())) {
            for (x in (0 until tw.toInt())) {
                val isWhite = ((x xor y) and 0b1000000) != 0
                val c = if (isWhite) 0xFF else 0xA

                val i = y * tw.toInt() + x

                pTextureData[i * 4 + 0] = c
                pTextureData[i * 4 + 1] = c
                pTextureData[i * 4 + 2] = c
                pTextureData[i * 4 + 3] = 0xFF
            }
        }

        texture.replaceRegion(MTLRegionMake3D(0uL, 0uL, 0uL, tw, th, 1uL), 0, pTextureData, tw * 4uL)

    }

    private fun buildDepthStencilStates() {
        println("buildDepthStencilStates")
        val depthStencilDescriptor = MTLDepthStencilDescriptor()
        depthStencilDescriptor.setDepthCompareFunction(MTLCompareFunctionLess)
        depthStencilDescriptor.setDepthWriteEnabled(true)

        depthStencilState = device.newDepthStencilStateWithDescriptor(depthStencilDescriptor)
            ?: error("fail to create depth state pencil")

    }

    private fun buildShaders() = memScoped {
        println("build shaders")

        val shaderSrc = """
        #include <metal_stdlib>
        using namespace metal;

        struct v2f
                {
                    float4 position [[position]];
                    float3 normal;
                    half3 color;
                    float2 texcoord;
                };

        struct VertexData
                {
                    float3 position;
                    float3 normal;
                    float2 texcoord;
                };

        struct InstanceData
                {
                    float4x4 instanceTransform;
                    float3x3 instanceNormalTransform;
                    float4 instanceColor;
                };

        struct CameraData
                {
                    float4x4 perspectiveTransform;
                    float4x4 worldTransform;
                    float3x3 worldNormalTransform;
                };

        v2f vertex vertexMain( device const VertexData* vertexData [[buffer(0)]],
            device const InstanceData* instanceData [[buffer(1)]],
            device const CameraData& cameraData [[buffer(2)]],
            uint vertexId [[vertex_id]],
            uint instanceId [[instance_id]] )
        {
            v2f o;

            const device VertexData& vd = vertexData[ vertexId ];
            float4 pos = float4( vd.position, 1.0 );
            pos = instanceData[ instanceId ].instanceTransform * pos;
            pos = cameraData.perspectiveTransform * cameraData.worldTransform * pos;
            o.position = pos;

            float3 normal = instanceData[ instanceId ].instanceNormalTransform * vd.normal;
            normal = cameraData.worldNormalTransform * normal;
            o.normal = normal;

            o.texcoord = vd.texcoord.xy;

            o.color = half3( instanceData[ instanceId ].instanceColor.rgb );
            return o;
        }

        half4 fragment fragmentMain( v2f in [[stage_in]], texture2d< half, access::sample > tex [[texture(0)]] )
        {
            constexpr sampler s( address::repeat, filter::linear );
            half3 texel = tex.sample( s, in.texcoord ).rgb;

            // assume light coming from (front-top-right)
            float3 l = normalize(float3( 1.0, 1.0, 0.8 ));
            float3 n = normalize( in.normal );

            half ndotl = half( saturate( dot( n, l ) ) );

            half3 illum = (in.color * texel * 0.1) + (in.color * texel * ndotl);
            return half4( illum, 1.0 );
        }
        """.trimIndent()

        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        library = device.newLibraryWithSource(shaderSrc, null, errorPtr.ptr).let {
            errorPtr.value?.let { error -> error(error.localizedDescription) }
            it ?: error("fail to create library")
        }

        val vertexFunction = library.newFunctionWithName("vertexMain")
        val fragmentFunction = library.newFunctionWithName("fragmentMain")
        val renderPipelineDescriptor = MTLRenderPipelineDescriptor().apply {
            setVertexFunction(vertexFunction)
            setFragmentFunction(fragmentFunction)
            colorAttachments.objectAtIndexedSubscript(0)
                .setPixelFormat(MTLPixelFormatBGRA8Unorm_sRGB)
            setDepthAttachmentPixelFormat(MTLPixelFormatDepth16Unorm)
        }

        renderPipelineStateProtocol =
            device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
                errorPtr.value?.let { error -> error(error.localizedDescription) }
                it ?: error("fail to create render pipeline state")
            }

    }
}

public fun NativePlacement.allocArrayOf(elements: List<Int>): CArrayPointer<IntVar> {
    val res = allocArray<IntVar>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

public fun NativePlacement.allocArrayOf(elements: List<Float>): CArrayPointer<FloatVar> {
    val res = allocArray<FloatVar>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

fun makeTranslate(vector: List<Float>) = vector.let { (x, y, z) ->

    val col0 = mk[1.0f, 0f, 0f, 0f]
    val col1 = mk[0f, 1.0f, 0f, 0f]
    val col2 = mk[0f, 0f, 1.0f, 0f]
    val col3 = mk[x, y, z, 1.0f]
    mk.ndarray(mk[col0, col1, col2, col3])
}

fun makeXRotate(angleRadians: Float): D2Array<Float> {
    return mk.ndarray(
        mk[
            mk[1.0f, 0f, 0f, 0f],
            mk[0f, cos(angleRadians), sin(angleRadians), 0f],
            mk[0f, -sin(angleRadians), cos(angleRadians), 0f],
            mk[0f, 0f, 0f, 1.0f]
        ]
    )
}

fun makeYRotate(angleRadians: Float): D2Array<Float> {
    return mk.ndarray(
        mk[
            mk[cos(angleRadians), 0f, sin(angleRadians), 0f],
            mk[0f, 1.0f, 0f, 0f],
            mk[-sin(angleRadians), 0f, cos(angleRadians), 0f],
            mk[0f, 0f, 0f, 1.0f]
        ]
    )
}

fun makeZRotate(angleRadians: Float): D2Array<Float> {
    return mk.ndarray(
        mk[
            mk[cos(angleRadians), sin(angleRadians), 0f, 0f],
            mk[-sin(angleRadians), cos(angleRadians), 0f, 0f],
            mk[0f, 0f, 1.0f, 0f],
            mk[0f, 0f, 0f, 1.0f]
        ]
    )
}

fun makeScale(vector: List<Float>) = vector.let { (x, y, z) ->
    mk.ndarray(
        mk[
            mk[x, 0f, 0f, 0f],
            mk[0f, y, 0f, 0f],
            mk[0f, 0f, z, 0f],
            mk[0f, 0f, 0f, 1f]
        ]
    )
}

fun add(firstVector: List<Float>, secondVector: List<Float>): List<Float> {
    return listOf(
        firstVector[0] + secondVector[0],
        firstVector[1] + secondVector[1],
        firstVector[2] + secondVector[2]
    )
}

private fun discardTranslation(instanceTransform: D2Array<Float>): D2Array<Float> {
    return mk.ndarray(
        mk[
            instanceTransform[0].data.getFloatArray().toList(),
            instanceTransform[1].data.getFloatArray().toList(),
            instanceTransform[2].data.getFloatArray().toList()
        ]
    )
}

private fun makePerspective(fovRadians: Float, aspect: Float, znear: Float, zfar: Float): D2Array<Float> {
    val ys = 1f / tan(fovRadians * 0.5f)
    val xs = ys / aspect
    val zs = zfar / (znear - zfar)
    return mk.ndarray(
        mk[
            mk[xs, 0f, 0f, 0f],
            mk[0f, ys, 0f, 0f],
            mk[0f, 0f, zs, znear * zs],
            mk[0f, 0f, -1f, 0f]
        ]
    )
}

private fun makeIdentity(): D2Array<Float> {
    return mk.identity(4)
}
