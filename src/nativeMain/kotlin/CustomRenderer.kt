import kotlinx.cinterop.*
import platform.Foundation.NSError
import platform.Metal.*
import platform.MetalKit.MTKView

/**
 * Pixel shader :
 * #version 100 compatibility
 * #ifdef GL_ES
 * 	precision highp float;
 * 	precision highp int;
 * 	precision lowp sampler2D;
 * 	precision lowp samplerCube;
 * #else
 * 	  #define highp
 * 	  #define mediump
 * 	  #define lowp
 * #endif
 * attribute mediump vec2 a_Tex;
 * attribute lowp vec4 a_Col;
 * attribute highp vec2 a_Pos;
 * uniform mat4 u_ProjMat;
 * uniform mat4 u_ViewMat;
 * varying mediump vec2 v_Tex;
 * varying vec4 v_Col;
 * void main() {
 * 	v_Tex = a_Tex;
 * 	v_Col = a_Col;
 * 	gl_Position = ((u_ProjMat * u_ViewMat) * vec4(a_Pos, 0.0, 1.0));
 * }
 */
/**
 * Fragment shader
 * #version 100 compatibility
 * #ifdef GL_ES
 * 	precision highp float;
 * 	precision highp int;
 * 	precision lowp sampler2D;
 * 	precision lowp samplerCube;
 * #else
 * 	  #define highp
 * 	  #define mediump
 * 	  #define lowp
 * #endif
 * varying vec4 v_Col;
 * void main() {
 * 	gl_FragColor = v_Col;
 * }
 */
const val shader = """
#include <metal_stdlib>
using namespace metal;

struct VertexIn {
    float2 a_Tex;
    float4 a_Col;
    float2 a_Pos;
};

struct VertexOut {
    float2 v_Tex;
    float4 v_Col;
    float4 gl_Position [[position]];
};

struct Uniforms {
    float4x4 u_ProjMat;
    float4x4 u_ViewMat;
};

vertex VertexOut vertexMain(device const VertexIn* in [[buffer(0)]],
                            device const Uniforms& u [[buffer(1)]],
                            uint vertexId [[vertex_id]]) {     
    VertexOut out;
    out.v_Tex = in[vertexId].a_Tex;
    out.v_Col = in[vertexId].a_Col;
    out.gl_Position = (u.u_ProjMat * u.u_ViewMat) * float4(in[vertexId].a_Pos, 0.0, 1.0);
    return out;
}


fragment float4 fragmentMain(VertexOut in [[stage_in]]) {
  return in.v_Col;
}

"""

class CustomRenderer(device: MTLDeviceProtocol) : Renderer(device) {

    private val commandBuffer: MTLCommandBufferProtocol
    private lateinit var library: MTLLibraryProtocol
    private lateinit var renderPipelineStateProtocol: MTLRenderPipelineStateProtocol

    init {
        commandBuffer = commandQueue.commandBuffer() ?: error("fail to get command buffer")
        buildShaders()
    }

    override fun drawOnView(view: MTKView) {
        TODO("Not yet implemented")
    }

    private fun buildShaders() = memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        library = device.newLibraryWithSource(shader, null, errorPtr.ptr).let {
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

}