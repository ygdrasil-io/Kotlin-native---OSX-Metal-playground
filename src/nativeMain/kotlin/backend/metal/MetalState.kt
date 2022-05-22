package backend.metal

import backend.*
import platform.Metal.*

const val SAMPLER_GROUP_COUNT = CONFIG_BINDING_COUNT
const val SAMPLER_BINDING_COUNT = MAX_SAMPLER_COUNT
const val VERTEX_BUFFER_START = CONFIG_BINDING_COUNT

// The "zero" buffer is a small buffer for missing attributes that resides in the vertex slot
// immediately following any user-provided vertex buffers.
const val ZERO_VERTEX_BUFFER = MAX_VERTEX_BUFFER_COUNT;

// The total number of vertex buffer "slots" that the Metal backend can bind.
// + 1 to account for the zero buffer.
const val VERTEX_BUFFER_COUNT = MAX_VERTEX_BUFFER_COUNT + 1

class StateCache<StateType, MetalType, StateCreator>(val device: MTLDeviceProtocol) {
}

class StateTracker<StateType> {

}


data class MetalPipelineState(
    val vertexFunction: MTLFunctionProtocol? = null,
    val fragmentFunction: MTLFunctionProtocol? = null,
    val vertexDescription: VertexDescription,
    val colorAttachmentPixelFormat: Array<MTLPixelFormat> = Array(TargetBufferInfo.MAX_SUPPORTED_RENDER_TARGET_COUNT) { MTLPixelFormatInvalid },
    val depthAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatInvalid,
    val sampleCount: Int = 1,
    val blendState: BlendState = BlendState(),
    val colorWrite: Boolean = true,
    val padding: Array<Char> = Array(7) { 0.toChar() }
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if( other == null)  return false
        if (this::class != other::class) return false

        other as MetalPipelineState

        if (vertexFunction != other.vertexFunction) return false
        if (fragmentFunction != other.fragmentFunction) return false
        if (vertexDescription != other.vertexDescription) return false
        if (!colorAttachmentPixelFormat.contentEquals(other.colorAttachmentPixelFormat)) return false
        if (depthAttachmentPixelFormat != other.depthAttachmentPixelFormat) return false
        if (sampleCount != other.sampleCount) return false
        if (blendState != other.blendState) return false
        if (colorWrite != other.colorWrite) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertexFunction?.hashCode() ?: 0
        result = 31 * result + (fragmentFunction?.hashCode() ?: 0)
        result = 31 * result + vertexDescription.hashCode()
        result = 31 * result + colorAttachmentPixelFormat.contentHashCode()
        result = 31 * result + depthAttachmentPixelFormat.hashCode()
        result = 31 * result + sampleCount.hashCode()
        result = 31 * result + blendState.hashCode()
        result = 31 * result + colorWrite.hashCode()
        return result
    }


}

typealias PipelineStateTracker = StateTracker<MetalPipelineState>
typealias PipelineStateCache = StateCache<MetalPipelineState, MTLRenderPipelineStateProtocol, PipelineStateCreator>


data class BlendState(
    val alphaBlendOperation: MTLBlendOperation = MTLBlendOperationAdd,
    val rgbBlendOperation: MTLBlendOperation = MTLBlendOperationAdd,
    val destinationAlphaBlendFactor: MTLBlendFactor = MTLBlendFactorZero,
    val destinationRGBBlendFactor: MTLBlendFactor = MTLBlendFactorZero,
    val sourceAlphaBlendFactor: MTLBlendFactor = MTLBlendFactorOne,
    val sourceRGBBlendFactor: MTLBlendFactor = MTLBlendFactorOne,
    val blendingEnabled: Boolean = false,
    val padding: Array<Char> = Array(7) { 0.toChar() }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if( other == null)  return false
        if (this::class != other::class) return false

        other as BlendState

        if (alphaBlendOperation != other.alphaBlendOperation) return false
        if (rgbBlendOperation != other.rgbBlendOperation) return false
        if (destinationAlphaBlendFactor != other.destinationAlphaBlendFactor) return false
        if (destinationRGBBlendFactor != other.destinationRGBBlendFactor) return false
        if (sourceAlphaBlendFactor != other.sourceAlphaBlendFactor) return false
        if (sourceRGBBlendFactor != other.sourceRGBBlendFactor) return false
        if (blendingEnabled != other.blendingEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alphaBlendOperation.hashCode()
        result = 31 * result + rgbBlendOperation.hashCode()
        result = 31 * result + destinationAlphaBlendFactor.hashCode()
        result = 31 * result + destinationRGBBlendFactor.hashCode()
        result = 31 * result + sourceAlphaBlendFactor.hashCode()
        result = 31 * result + sourceRGBBlendFactor.hashCode()
        result = 31 * result + blendingEnabled.hashCode()
        return result
    }

}

// VertexDescription is part of Metal's pipeline state, and represents how vertex attributes are
// laid out in memory.
// Vertex attributes are "turned on" by setting format to something other than
// MTLVertexFormatInvalid, which is the default.
data class VertexDescription(
    val attributes: Array<Attribute?> = Array(MAX_VERTEX_ATTRIBUTE_COUNT) {null},
    val layouts: Array<Layout?> = Array(VERTEX_BUFFER_COUNT) {null}
) {
    data class Attribute(
        val format: MTLVertexFormat,
        val buffer: Int,
        val offset: Int
    )

    data class Layout(
        val step: MTLVertexStepFunction,
        val stride: Int
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if( other == null)  return false
        if (this::class != other::class) return false

        other as VertexDescription

        if (!attributes.contentEquals(other.attributes)) return false
        if (!layouts.contentEquals(other.layouts)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attributes.contentHashCode()
        result = 31 * result + layouts.contentHashCode()
        return result
    }


}

// Sampler states

data class SamplerState(
    val samplerParams: SamplerParams = SamplerParams(),
    val minLod: Int = 0,
    val maxLod: Int = Int.MAX_VALUE

)

interface PipelineStateCreator {
    fun get(device: MTLDeviceProtocol, state: MetalPipelineState) : MTLRenderPipelineStateProtocol
}


interface SamplerStateCreator {
    fun get(device: MTLDeviceProtocol, state: SamplerState) :MTLSamplerStateProtocol
}

typealias SamplerStateCache = StateCache<SamplerState, MTLSamplerStateProtocol, SamplerStateCreator>




// Depth-stencil State

data class DepthStencilState(
    val compareFunction: MTLCompareFunction = MTLCompareFunctionAlways,
    val depthWriteEnabled: Boolean = false,
    val padding: Array<Char> = Array(7) { 0.toChar() }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if( other == null)  return false
        if (this::class != other::class) return false

        other as DepthStencilState

        if (compareFunction != other.compareFunction) return false
        if (depthWriteEnabled != other.depthWriteEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = compareFunction.hashCode()
        result = 31 * result + depthWriteEnabled.hashCode()
        return result
    }
};

interface DepthStateCreator {
    fun get(device: MTLDeviceProtocol, state: DepthStencilState): DepthStencilState
};

typealias DepthStencilStateTracker = StateTracker<DepthStencilState>

typealias DepthStencilStateCache = StateCache<DepthStencilState, MTLDepthStencilStateProtocol, DepthStateCreator>