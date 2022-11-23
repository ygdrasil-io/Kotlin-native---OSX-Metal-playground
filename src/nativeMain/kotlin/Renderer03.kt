import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Foundation.NSMakeRange
import platform.Metal.*
import platform.MetalKit.MTKView
import platform.darwin.*
import platform.posix.memmove

const val kMaxFramesInFlight = 3L

class Renderer03(device: MTLDeviceProtocol) : Renderer(device) {

    private var semaphore: dispatch_semaphore_t
    private lateinit var argBuffer: MTLBufferProtocol
    private lateinit var library: MTLLibraryProtocol
    private lateinit var vertexPositionsBuffer: MTLBufferProtocol
    private lateinit var vertexColorsBuffer: MTLBufferProtocol
    private lateinit var renderPipelineStateProtocol: MTLRenderPipelineStateProtocol
    private lateinit var frameData: Array<MTLBufferProtocol>
    private var frame = 0L
    private var angle = 0f

    init {
        buildShaders()
        buildBuffers()
        buildFrameData()

        semaphore = dispatch_semaphore_create( kMaxFramesInFlight ) ?: error("fail to create semaphore")
    }


    override fun drawOnView(view: MTKView) {
        autoreleasepool {

            frame = (frame + 1L) % kMaxFramesInFlight
            val frameDataBuffer = frameData[ frame.toInt() ]

            val commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")
            dispatch_semaphore_wait( semaphore, DISPATCH_TIME_FOREVER )

            commandBuffer.addCompletedHandler {
                dispatch_semaphore_signal(semaphore)
            }

            angle += 0.01f
            frameDataBuffer.contents()!!.reinterpret<FloatVar>()[0] = angle

            frameDataBuffer.didModifyRange(NSMakeRange(0, sizeOf<FloatVar>().toULong()))

            val renderPassDescriptor = view.currentRenderPassDescriptor() ?: error("fail to create render pass descriptor")
            commandBuffer.renderCommandEncoderWithDescriptor( renderPassDescriptor )
                ?.apply {
                    setRenderPipelineState( renderPipelineStateProtocol )
                    setVertexBuffer( argBuffer, 0, 0 )
                    useResource( vertexPositionsBuffer, MTLResourceUsageRead )
                    useResource( vertexColorsBuffer, MTLResourceUsageRead )

                    setVertexBuffer( frameDataBuffer, 0, 1 )
                    drawPrimitives( MTLPrimitiveTypeTriangle, 0, 3 )

                    endEncoding()
                } ?: error("")


            commandBuffer.presentDrawable( view.currentDrawable()!! )
            commandBuffer.commit()



        }
    }

    private fun buildShaders() = memScoped {
        val shaderSrc = """
        #include <metal_stdlib>
        using namespace metal;

        struct v2f
        {
            float4 position [[position]];
            half3 color;
        };

        struct VertexData
        {
            device float3* positions [[id(0)]];
            device float3* colors [[id(1)]];
        };

        struct FrameData
        {
            float angle;
        };

        v2f vertex vertexMain( device const VertexData* vertexData [[buffer(0)]], constant FrameData* frameData [[buffer(1)]], uint vertexId [[vertex_id]] )
        {
            float a = frameData->angle;
            float3x3 rotationMatrix = float3x3( sin(a), cos(a), 0.0, cos(a), -sin(a), 0.0, 0.0, 0.0, 1.0 );
            v2f o;
            o.position = float4( rotationMatrix * vertexData->positions[ vertexId ], 1.0 );
            o.color = half3(vertexData->colors[ vertexId ]);
            return o;
        }

        half4 fragment fragmentMain( v2f in [[stage_in]] )
        {
            return half4( in.color, 1.0 );
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
        }

        renderPipelineStateProtocol =
            device.newRenderPipelineStateWithDescriptor(renderPipelineDescriptor, errorPtr.ptr).let {
                errorPtr.value?.let { error -> error(error.localizedDescription) }
                it ?: error("fail to create render pipeline state")
            }
    }

    private fun buildBuffers() = memScoped  {
        val numVertices = 3

        val position = allocArrayOf(
            -0.8f, 0.8f, 0.0f, 0.0f,
            0.0f, -0.8f, 0.0f, 0.0f,
            +0.8f, 0.8f, 0.0f, 0.0f
        )

        val colors = allocArrayOf(
            1f, 0.3f, 0.2f, 0.0f,
            0.8f, 1f, 0.0f, 0.0f,
            0.8f, 0.0f, 1f, 0.0f
        )

        val positionsDataSize = numVertices * sizeOf<FloatVar>() * 4
        val colorDataSize = numVertices * sizeOf<FloatVar>() * 4
        println("positionsDataSize $positionsDataSize colorDataSize $colorDataSize")

        vertexPositionsBuffer = device.newBufferWithLength(positionsDataSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexPositionsBuffer")
        vertexColorsBuffer = device.newBufferWithLength(colorDataSize.toULong(), MTLResourceStorageModeManaged)
            ?: error("fail to create vertexColorsBuffer")

        memmove(vertexPositionsBuffer.contents(), position.reinterpret<CPointed>(), positionsDataSize.toULong())
        memmove(vertexColorsBuffer.contents(), colors.reinterpret<CPointed>(), colorDataSize.toULong())

        vertexPositionsBuffer.didModifyRange(NSMakeRange(0, vertexPositionsBuffer.length))
        vertexColorsBuffer.didModifyRange(NSMakeRange(0, vertexColorsBuffer.length))

        val vertexFunction = library.newFunctionWithName( "vertexMain" ) ?: error("")
        val argEncoder = vertexFunction.newArgumentEncoderWithBufferIndex( 0 )

        argBuffer = device.newBufferWithLength( argEncoder.encodedLength, MTLResourceStorageModeManaged )
            ?: error("fail to create buffer")

        argEncoder.setArgumentBuffer( argBuffer, 0 )

        argEncoder.setBuffer( vertexPositionsBuffer, 0, 0 )
        argEncoder.setBuffer( vertexColorsBuffer, 0, 1 )

        argBuffer.didModifyRange( NSMakeRange(0, argBuffer.length) )
    }

    private fun buildFrameData() {
        frameData = Array(kMaxFramesInFlight.toInt()) {
            device.newBufferWithLength(sizeOf<FloatVar>().toULong(), MTLResourceStorageModeManaged)
                ?: error("fail to create buffer")
        }
    }
}