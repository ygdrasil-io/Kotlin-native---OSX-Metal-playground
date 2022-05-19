package backend.metal

import platform.Metal.MTLFunctionProtocol

class StateCache<StateType, MetalType, StateCreator>() {
}

class StateTracker<StateType> {

}



data class MetalPipelineState(
    val vertexFunction: MTLFunctionProtocol? = null, // 8 bytes
    val fragmentFunction: MTLFunctionProtocol? = null,  // 8 bytes
    val vertexDescription: VertexDescription,   // 528 bytes

){

    MTLPixelFormat colorAttachmentPixelFormat[MRT::MAX_SUPPORTED_RENDER_TARGET_COUNT] = { MTLPixelFormatInvalid };  // 64 bytes
    MTLPixelFormat depthAttachmentPixelFormat = MTLPixelFormatInvalid;         // 8 bytes
    val sampleCount = 1;                                                // 8 bytes
    BlendState blendState;                                                     // 56 bytes
    val colorWrite = true;                                                    // 1 byte
    char padding[7] = { 0 };                                                   // 7 bytes

    fun equals(const MetalPipelineState& rhs) : Boolean {
        return (
                this->vertexFunction == rhs.vertexFunction &&
        this->fragmentFunction == rhs.fragmentFunction &&
        this->vertexDescription == rhs.vertexDescription &&
        std::equal(this->colorAttachmentPixelFormat, this->colorAttachmentPixelFormat + MRT::MAX_SUPPORTED_RENDER_TARGET_COUNT,
        rhs.colorAttachmentPixelFormat) &&
        this->depthAttachmentPixelFormat == rhs.depthAttachmentPixelFormat &&
        this->sampleCount == rhs.sampleCount &&
        this->blendState == rhs.blendState &&
        this->colorWrite == rhs.colorWrite
        );
    }


};

typealias PipelineStateTracker = StateTracker<MetalPipelineState>
typealias PipelineStateCache = StateCache<MetalPipelineState, id<MTLRenderPipelineState>,PipelineStateCreator>



// VertexDescription is part of Metal's pipeline state, and represents how vertex attributes are
// laid out in memory.
// Vertex attributes are "turned on" by setting format to something other than
// MTLVertexFormatInvalid, which is the default.
data class VertexDescription {
    data class  Attribute {
        MTLVertexFormat format;     // 8 bytes
        uint32_t buffer;            // 4 bytes
        uint32_t offset;            // 4 bytes
    };
    data class  Layout {
        MTLVertexStepFunction step; // 8 bytes
        uint64_t stride;            // 8 bytes
    };
    Attribute attributes[MAX_VERTEX_ATTRIBUTE_COUNT] = {};      // 256 bytes
    Layout layouts[VERTEX_BUFFER_COUNT] = {};                   // 272 bytes

    bool operator==(const VertexDescription& rhs) const noexcept {
        bool result = true;
        for (uint32_t i = 0; i < MAX_VERTEX_ATTRIBUTE_COUNT; i++) {
        result &= (
        this->attributes[i].format == rhs.attributes[i].format &&
        this->attributes[i].buffer == rhs.attributes[i].buffer &&
        this->attributes[i].offset == rhs.attributes[i].offset
        );
    }
        for (uint32_t i = 0; i < MAX_VERTEX_ATTRIBUTE_COUNT; i++) {
        result &= this->layouts[i].stride == rhs.layouts[i].stride;
    }
        return result;
    }

    bool operator!=(const VertexDescription& rhs) const noexcept {
        return !operator==(rhs);
    }
};