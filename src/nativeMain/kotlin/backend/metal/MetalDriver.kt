package backend.metal

import backend.*
import kotlinx.cinterop.autoreleasepool
import platform.CoreVideo.CVMetalTextureCacheFlush
import platform.Metal.*
import kotlin.math.min

class MetalDriver : DriverBase() {

    private val metalContext: MetalContext

    init {

        val device = MetalPlatform.createDevice()
        val highestSupportedGpuFamily = initializeSupportedGpuFamilies(device)

        println("Supported GPU families: ")
        if (highestSupportedGpuFamily.common > 0) {
            println("  MTLGPUFamilyCommon${highestSupportedGpuFamily.common}")
        }
        if (highestSupportedGpuFamily.apple > 0) {
            println(" MTLGPUFamilyApple${highestSupportedGpuFamily.apple}")
        }
        if (highestSupportedGpuFamily.mac > 0) {
            println("  MTLGPUFamilyMac${highestSupportedGpuFamily.mac}")
        }
        println("Features:")
        println("  readWriteTextureSupport: ${device.readWriteTextureSupport}")

        // In order to support texture swizzling, the GPU needs to support it and the system be running
        // iOS 13+.
        val supportsTextureSwizzling =
            highestSupportedGpuFamily.apple >= 1 ||  // all Apple GPUs
                    highestSupportedGpuFamily.mac >= 2 // newer macOS GPUs

        val maxColorRenderTargets = if (highestSupportedGpuFamily.apple >= 2 || highestSupportedGpuFamily.mac >= 1) {
            8
        } else 4

        val sampleCountLookup = (1..MAX_SAMPLE_COUNT).map { sampleCount ->
            device.supportsTextureSampleCount(sampleCount.toULong())
        }

        metalContext = MetalContext(
            this,
            device,
            highestSupportedGpuFamily,
            supportsTextureSwizzling,
            maxColorRenderTargets,
            sampleCountLookup
        )
    }

    fun setFrameScheduledCallback(sch: HwSwapChain, callback: FrameScheduledCallback, user: Any) {
        (sch as MetalSwapChain).setFrameScheduledCallback(callback, user)
    }

    fun setFrameCompletedCallback(sch: HwSwapChain, callback: FrameCompletedCallback, user: Any) {
        (sch as MetalSwapChain).setFrameCompletedCallback(callback, user)
    }

    fun execute(fn: () -> Unit) {
        autoreleasepool {
            fn()
        }
    }

    suspend fun endFrame(frameId: Int) {

        // If we haven't committed the command buffer (if the frame was canceled), do it now. There may
        // be commands in it (like fence signaling) that need to execute.
        metalContext.submitPendingCommands()

        metalContext.bufferPool.gc()

        // If we acquired a drawable for this frame, ensure that we release it here.
        metalContext.currentDrawSwapChain?.releaseDrawable()


        CVMetalTextureCacheFlush(metalContext.textureCache, 0)

        // TODO: check to add profiling
        /*#if defined(FILAMENT_METAL_PROFILING)
        os_signpost_interval_end(mContext->log, mContext->signpostId, "Frame encoding");
        #endif*/
    }

    fun flush() {
        assert(!metalContext.isInRenderPass()) {
            error("flush must be called outside of a render pass.")
        }

        metalContext.submitPendingCommands();
    }

    fun finish() {
        assert(!metalContext.isInRenderPass()) {
            error("finish must be called outside of a render pass.")
        }

        // Wait for all frames to finish by submitting and waiting on a dummy command buffer.
        metalContext.submitPendingCommands();
        metalContext.commandQueue.commandBuffer()?.let { oneOffBuffer ->
            oneOffBuffer.commit()
            oneOffBuffer.waitUntilCompleted()
        }
    }


    fun createVertexBufferR(
        bufferCount: Int,
        attributeCount: Int,
        vertexCount: Int,
        attributes: AttributeArray
    ): HwVertexBuffer {
        return MetalVertexBuffer(metalContext, bufferCount, attributeCount, vertexCount, attributes)
    }

    fun createIndexBufferR(elementType: ElementType, indexCount: ULong, usage: BufferUsage): HwIndexBuffer {
        val elementSize = getElementTypeSize(elementType);
        return MetalIndexBuffer(metalContext, usage, elementSize, indexCount);
    }

    override val dispatcher: Dispatcher = Dispatcher()


    override fun createRenderPrimitive(): HwRenderPrimitive {
        return MetalRenderPrimitive()
    }

    // TODO; check if any usage
    /*fun createBufferObjectR(Handle<HwBufferObject> boh, uint32_t byteCount,
    BufferObjectBinding bindingType, BufferUsage usage) {
        construct_handle<MetalBufferObject>(boh, *mContext, usage, byteCount);
    }

    fun createTextureR( th: HwTexture, target: SamplerType, levels: Int,
    format: TextureFormat, samples: Int, width: Int, height: Int,
    depth: Int, usage: TextureUsage) {
        // Clamp sample count to what the device supports.
        auto& sc = mContext->sampleCountLookup;
        samples = sc[std.min(MAX_SAMPLE_COUNT, samples)];

        construct_handle<MetalTexture>(th, *mContext, target, levels, format, samples,
            width, height, depth, usage, TextureSwizzle.CHANNEL_0, TextureSwizzle.CHANNEL_1,
            TextureSwizzle.CHANNEL_2, TextureSwizzle.CHANNEL_3);
    }
    fun createTextureSwizzledR(Handle<HwTexture> th, SamplerType target, uint8_t levels,
    TextureFormat format, uint8_t samples, uint32_t width, uint32_t height,
    uint32_t depth, TextureUsage usage,
    TextureSwizzle r, TextureSwizzle g, TextureSwizzle b, TextureSwizzle a) {
        // Clamp sample count to what the device supports.
        auto& sc = mContext->sampleCountLookup;
        samples = sc[std.min(MAX_SAMPLE_COUNT, samples)];

        construct_handle<MetalTexture>(th, *mContext, target, levels, format, samples,
            width, height, depth, usage, r, g, b, a);
    }

    fun importTextureR(Handle<HwTexture> th, intptr_t i,
    SamplerType target, uint8_t levels,
    TextureFormat format, uint8_t samples, uint32_t width, uint32_t height,
    uint32_t depth, TextureUsage usage) {
        id<MTLTexture> metalTexture = (id<MTLTexture>) CFBridgingRelease((void*) i);
        ASSERT_PRECONDITION(metalTexture.width == width,
            "Imported id<MTLTexture> width (%d) != Filament texture width (%d)",
            metalTexture.width, width);
        ASSERT_PRECONDITION(metalTexture.height == height,
            "Imported id<MTLTexture> height (%d) != Filament texture height (%d)",
            metalTexture.height, height);
        ASSERT_PRECONDITION(metalTexture.mipmapLevelCount == levels,
            "Imported id<MTLTexture> levels (%d) != Filament texture levels (%d)",
            metalTexture.mipmapLevelCount, levels);
        MTLPixelFormat filamentMetalFormat = getMetalFormat(mContext, format);
        ASSERT_PRECONDITION(metalTexture.pixelFormat == filamentMetalFormat,
            "Imported id<MTLTexture> format (%d) != Filament texture format (%d)",
            metalTexture.pixelFormat, filamentMetalFormat);
        MTLTextureType filamentMetalType = getMetalType(target);
        ASSERT_PRECONDITION(metalTexture.textureType == filamentMetalType,
            "Imported id<MTLTexture> type (%d) != Filament texture type (%d)",
            metalTexture.textureType, filamentMetalType);
        construct_handle<MetalTexture>(th, *mContext, target, levels, format, samples,
            width, height, depth, usage, metalTexture);
    }

    fun createSamplerGroupR(Handle<HwSamplerGroup> sbh, uint32_t size) {
        mContext->samplerGroups.insert(construct_handle<MetalSamplerGroup>(sbh, size));
    }

    fun createRenderPrimitiveR(Handle<HwRenderPrimitive> rph, int dummy) {
        construct_handle<MetalRenderPrimitive>(rph);
    }

    fun createProgramR(Handle<HwProgram> rph, Program&& program) {
        construct_handle<MetalProgram>(rph, mContext->device, program);
    }

    fun createDefaultRenderTargetR(Handle<HwRenderTarget> rth, int dummy) {
        construct_handle<MetalRenderTarget>(rth, mContext);
    }*/

    override fun createRenderTarget(
        targetBufferFlags: TargetBufferFlags, width: UInt, height: UInt,
        samples: UShort, color: MRT, depth: TargetBufferInfo, stencil: TargetBufferInfo
    ): HwRenderTarget {
        // Clamp sample count to what the device supports.
        val sc = metalContext.sampleCountLookup;
        val samples = sc[min(MAX_SAMPLE_COUNT.toInt(), samples.toInt())]

        val colorAttachments = Array(MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT.toInt()) { MetalRenderTarget.Attachment() }
        (0 until MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT.toInt()).forEach { i ->
            val buffer = color[i]
            if (buffer.handle == null) {
                if (targetBufferFlags.value and getTargetBufferFlagsAt(i.toUInt()).value == 0u) {
                    error("The COLOR$i flag was specified, but no color texture provided.")
                }
            } else {

                val colorTexture = buffer.handle as MetalTexture
                if (colorTexture.texture == null) error("Color texture passed to render target has no texture allocation")
                colorTexture.updateLodRange(buffer.level.toUInt())
                colorAttachments[i] = MetalRenderTarget.Attachment(colorTexture, color[i].level, color[i].layer)
            }
        }

        var depthAttachment = MetalRenderTarget.Attachment()
        if (depth.handle != null) {
            val depthTexture = depth.handle as MetalTexture
            if (depthTexture.texture == null) error("Depth texture passed to render target has no texture allocation.")
            depthTexture.updateLodRange(depth.level.toUInt())
            depthAttachment = MetalRenderTarget.Attachment(depthTexture, depth.level, depth.layer)
        }
        if ((targetBufferFlags.value and TargetBufferFlags.DEPTH.value) != 0u && depth.handle == null)
            error("The DEPTH flag was specified, but no depth texture provided.")

        if (stencil.handle != null || (targetBufferFlags.value and TargetBufferFlags.STENCIL.value) != 0u) {
            error("Stencil buffer not supported.")
        }

        return MetalRenderTarget(metalContext, width, height, samples, colorAttachments, depthAttachment)
    }

    /*
    fun createFenceR(Handle<HwFence> fh, int dummy) {
        auto* fence = handle_cast<MetalFence>(fh);
        fence->encode();
    }

    fun createSyncR(Handle<HwSync> sh, int) {
        auto* fence = handle_cast<MetalFence>(sh);
        fence->encode();
    }

    fun createSwapChainR(Handle<HwSwapChain> sch, void* nativeWindow, uint64_t flags) {
        if (UTILS_UNLIKELY(flags & SWAP_CHAIN_CONFIG_APPLE_CVPIXELBUFFER)) {
            CVPixelBufferRef pixelBuffer = (CVPixelBufferRef) nativeWindow;
            construct_handle<MetalSwapChain>(sch, *mContext, pixelBuffer, flags);
        } else {
            auto* metalLayer = (__bridge CAMetalLayer*) nativeWindow;
            construct_handle<MetalSwapChain>(sch, *mContext, metalLayer, flags);
        }
    }

    fun createSwapChainHeadlessR(Handle<HwSwapChain> sch,
    uint32_t width, uint32_t height, uint64_t flags) {
        construct_handle<MetalSwapChain>(sch, *mContext, width, height, flags);
    }

    fun createTimerQueryR(Handle<HwTimerQuery> tqh, int) {
        // nothing to do, timer query was constructed in createTimerQueryS
    }

    Handle<HwVertexBuffer> fun createVertexBufferS() noexcept {
        return alloc_handle<MetalVertexBuffer>();
    }

    Handle<HwIndexBuffer> fun createIndexBufferS() noexcept {
        return alloc_handle<MetalIndexBuffer>();
    }

    Handle<HwBufferObject> fun createBufferObjectS() noexcept {
        return alloc_handle<MetalBufferObject>();
    }

    Handle<HwTexture> fun createTextureS() noexcept {
        return alloc_handle<MetalTexture>();
    }

    Handle<HwTexture> fun createTextureSwizzledS() noexcept {
        return alloc_handle<MetalTexture>();
    }

    Handle<HwTexture> fun importTextureS() noexcept {
        return alloc_handle<MetalTexture>();
    }

    Handle<HwSamplerGroup> fun createSamplerGroupS() noexcept {
        return alloc_handle<MetalSamplerGroup>();
    }

    Handle<HwRenderPrimitive> fun createRenderPrimitiveS() noexcept {
        return alloc_handle<MetalRenderPrimitive>();
    }

    Handle<HwProgram> fun createProgramS() noexcept {
        return alloc_handle<MetalProgram>();
    }

    Handle<HwRenderTarget> fun createDefaultRenderTargetS() noexcept {
        return alloc_handle<MetalRenderTarget>();
    }

    Handle<HwRenderTarget> fun createRenderTargetS() noexcept {
        return alloc_handle<MetalRenderTarget>();
    }

    Handle<HwFence> fun createFenceS() noexcept {
        // The handle must be constructed here, as a synchronous call to wait might happen before
        // createFenceR is executed.
        return alloc_and_construct_handle<MetalFence, HwFence>(*mContext);
    }

    Handle<HwSync> fun createSyncS() noexcept {
        // The handle must be constructed here, as a synchronous call to getSyncStatus might happen
        // before createSyncR is executed.
        return alloc_and_construct_handle<MetalFence, HwSync>(*mContext);
    }

    Handle<HwSwapChain> fun createSwapChainS() noexcept {
        return alloc_handle<MetalSwapChain>();
    }

    Handle<HwSwapChain> fun createSwapChainHeadlessS() noexcept {
        return alloc_handle<MetalSwapChain>();
    }

    Handle<HwTimerQuery> fun createTimerQueryS() noexcept {
        // The handle must be constructed here, as a synchronous call to getTimerQueryValue might happen
        // before createTimerQueryR is executed.
        return alloc_and_construct_handle<MetalTimerQuery, HwTimerQuery>();
    }

    fun destroyVertexBuffer(Handle<HwVertexBuffer> vbh) {
        if (vbh) {
            destruct_handle<MetalVertexBuffer>(vbh);
        }
    }

    fun destroyIndexBuffer(Handle<HwIndexBuffer> ibh) {
        if (ibh) {
            destruct_handle<MetalIndexBuffer>(ibh);
        }
    }

    fun destroyBufferObject(Handle<HwBufferObject> boh) {
        if (UTILS_UNLIKELY(!boh)) {
            return;
        }
        auto* bo = handle_cast<MetalBufferObject>(boh);
        // Unbind this buffer object from any uniform slots it's still bound to.
        bo->boundUniformBuffers.forEachSetBit([this](size_t index) {
                mContext->uniformState[index].buffer = nullptr;
            mContext->uniformState[index].bound = false;
        });
        destruct_handle<MetalBufferObject>(boh);
    }

    fun destroyRenderPrimitive(Handle<HwRenderPrimitive> rph) {
        if (rph) {
            destruct_handle<MetalRenderPrimitive>(rph);
        }
    }

    fun destroyProgram(Handle<HwProgram> ph) {
        if (ph) {
            destruct_handle<MetalProgram>(ph);
        }
    }

    fun destroySamplerGroup(Handle<HwSamplerGroup> sbh) {
        if (!sbh) {
            return;
        }
        // Unbind this sampler group from our internal state.
        auto* metalSampler = handle_cast<MetalSamplerGroup>(sbh);
        for (auto& samplerBinding : mContext->samplerBindings) {
            if (samplerBinding == metalSampler) {
                samplerBinding = {};
            }
        }
        mContext->samplerGroups.erase(metalSampler);
        destruct_handle<MetalSamplerGroup>(sbh);
    }

    fun destroyTexture(Handle<HwTexture> th) {
        if (!th) {
            return;
        }

        // Unbind this texture from any sampler groups that currently reference it.
        for (auto* metalSamplerGroup : mContext->samplerGroups) {
            const SamplerGroup.Sampler* samplers = metalSamplerGroup->sb->getSamplers();
            for (size_t i = 0; i < metalSamplerGroup->sb->getSize(); i++) {
            const SamplerGroup.Sampler* sampler = samplers + i;
            if (sampler->t == th) {
                metalSamplerGroup->sb->setSampler(i, {{}, {}});
        }
        }
        }

        destruct_handle<MetalTexture>(th);
    }

    fun destroyRenderTarget(Handle<HwRenderTarget> rth) {
        if (rth) {
            destruct_handle<MetalRenderTarget>(rth);
        }
    }

    fun destroySwapChain(Handle<HwSwapChain> sch) {
        if (sch) {
            destruct_handle<MetalSwapChain>(sch);
        }
    }

    fun destroyStream(Handle<HwStream> sh) {
        // no-op
    }

    fun destroyTimerQuery(Handle<HwTimerQuery> tqh) {
        if (tqh) {
            destruct_handle<MetalTimerQuery>(tqh);
        }
    }

    fun destroySync(Handle<HwSync> sh) {
        if (sh) {
            destruct_handle<MetalFence>(sh);
        }
    }


    fun terminate() {
        // finish() will flush the pending command buffer and will ensure all GPU work has finished.
        // This must be done before calling bufferPool->reset() to ensure no buffers are in flight.
        finish();

        mContext->bufferPool->reset();
        mContext->commandQueue = nil;

        MetalExternalImage.shutdown(*mContext);
        mContext->blitter->shutdown();
    }

    ShaderModel fun getShaderModel() const noexcept {
        #if defined(IOS)
        return ShaderModel.GL_ES_30;
        #else
        return ShaderModel.GL_CORE_41;
        #endif
    }

    Handle<HwStream> fun createStreamNative(void* stream) {
        return {};
    }

    Handle<HwStream> fun createStreamAcquired() {
        return {};
    }

    fun setAcquiredImage(Handle<HwStream> sh, void* image,
    CallbackHandler* handler, StreamCallback cb, void* userData) {
    }

    fun setStreamDimensions(Handle<HwStream> stream, uint32_t width,
    uint32_t height) {

    }

    int64_t fun getStreamTimestamp(Handle<HwStream> stream) {
        return 0;
    }

    fun updateStreams(DriverApi* driver) {

    }

    fun destroyFence(Handle<HwFence> fh) {
        if (fh) {
            destruct_handle<MetalFence>(fh);
        }
    }

    FenceStatus fun wait(Handle<HwFence> fh, uint64_t timeout) {
        auto* fence = handle_cast<MetalFence>(fh);
        if (!fence) {
            return FenceStatus.ERROR;
        }
        return fence->wait(timeout);
    }
*/
    override fun isTextureFormatSupported(format: TextureFormat): Boolean {
        return decidePixelFormat(metalContext, format) != MTLPixelFormatInvalid;
    }
    /*

    bool fun isTextureSwizzleSupported() {
        return mContext->supportsTextureSwizzling;
    }*/

    override fun isTextureFormatMipmappable(format: TextureFormat): Boolean {
        // Derived from the Metal 3.0 Feature Set Tables.
        // In order for a format to be mipmappable, it must be color-renderable and filterable.
        val metalFormat = decidePixelFormat(metalContext, format)
        return when (metalFormat) {
            // Mipmappable across all devices:
            MTLPixelFormatR8Unorm,
            MTLPixelFormatR8Snorm,
            MTLPixelFormatR16Float,
            MTLPixelFormatRG8Unorm,
            MTLPixelFormatRG8Snorm,
            MTLPixelFormatRG16Float,
            MTLPixelFormatRGBA8Unorm,
            MTLPixelFormatRGBA8Unorm_sRGB,
            MTLPixelFormatRGBA8Snorm,
            MTLPixelFormatRGB10A2Unorm,
            MTLPixelFormatRG11B10Float,
            MTLPixelFormatRGBA16Float -> true
            // Mipmappable only on desktop:
            MTLPixelFormatR32Float,
            MTLPixelFormatRG32Float,
            MTLPixelFormatRGBA32Float -> true
            else -> false
        }
    }

    /*
    bool fun isRenderTargetFormatSupported(TextureFormat format) {
        MTLPixelFormat mtlFormat = getMetalFormat(mContext, format);
        // RGB9E5 isn't supported on Mac as a color render target.
        return mtlFormat != MTLPixelFormatInvalid && mtlFormat != MTLPixelFormatRGB9E5Float;
    }

    bool fun isFrameBufferFetchSupported() {
        // FrameBuffer fetch is achievable via "programmable blending" in Metal, and only supported on
        // Apple GPUs with readWriteTextureSupport.
        return mContext->highestSupportedGpuFamily.apple >= 1 &&
        mContext->device.readWriteTextureSupport;
    }

    bool fun isFrameBufferFetchMultiSampleSupported() {
        return isFrameBufferFetchSupported();
    }

    bool fun isFrameTimeSupported() {
        // Frame time is calculated via hard fences, which are only available on iOS 12 and above.
        if (@available(iOS 12, *)) {
            return true;
        }
        return false;
    }

    bool fun isAutoDepthResolveSupported() {
        return true;
    }

    bool fun isWorkaroundNeeded(Workaround workaround) {
        switch (workaround) {
            Workaround.SPLIT_EASU:
            return false;
            Workaround.ALLOW_READ_ONLY_ANCILLARY_FEEDBACK_LOOP:
            return true;
        }
        return false;
    }

    math.float2 fun getClipSpaceParams() {
        // virtual and physical z-coordinate of clip-space is in [-w, 0]
        // Note: this is actually never used (see: main.vs), but it's a backend API so we implement it
        // properly.
        return math.float2{ 1.0f, 0.0f };
    }

    uint8_t fun getMaxDrawBuffers() {
        return std.min(mContext->maxColorRenderTargets, MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT);
    }
    */
    suspend fun updateIndexBuffer(ibh: HwIndexBuffer, data: BufferDescriptor, byteOffset: Int) {
        (ibh as MetalIndexBuffer).let { ib ->
            ib.buffer.copyIntoBuffer(data.buffer, data.size, byteOffset);
            scheduleDestroy(std.move(data));
        }
    }
    /*

    fun updateBufferObject(Handle<HwBufferObject> boh, BufferDescriptor&& data,
    uint32_t byteOffset) {
        auto* bo = handle_cast<MetalBufferObject>(boh);
        bo->updateBuffer(data.buffer, data.size, byteOffset);
        scheduleDestroy(std.move(data));
    }

    fun setVertexBufferObject(Handle<HwVertexBuffer> vbh, uint32_t index,
    Handle<HwBufferObject> boh) {
        auto* vertexBuffer = handle_cast<MetalVertexBuffer>(vbh);
        auto* bufferObject = handle_cast<MetalBufferObject>(boh);
        assert_invariant(index < vertexBuffer->buffers.size());
        vertexBuffer->buffers[index] = bufferObject->getBuffer();
    }

    fun update2DImage(Handle<HwTexture> th, uint32_t level, uint32_t xoffset,
    uint32_t yoffset, uint32_t width, uint32_t height, PixelBufferDescriptor&& data) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "update2DImage must be called outside of a render pass.");
        auto tex = handle_cast<MetalTexture>(th);
        tex->loadImage(level, MTLRegionMake2D(xoffset, yoffset, width, height), data);
        scheduleDestroy(std.move(data));
    }

    fun setMinMaxLevels(Handle<HwTexture> th, uint32_t minLevel, uint32_t maxLevel) {
    }

    fun update3DImage(Handle<HwTexture> th, uint32_t level,
    uint32_t xoffset, uint32_t yoffset, uint32_t zoffset,
    uint32_t width, uint32_t height, uint32_t depth,
    PixelBufferDescriptor&& data) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "update3DImage must be called outside of a render pass.");
        auto tex = handle_cast<MetalTexture>(th);
        tex->loadImage(level, MTLRegionMake3D(xoffset, yoffset, zoffset, width, height, depth), data);
        scheduleDestroy(std.move(data));
    }

    fun updateCubeImage(Handle<HwTexture> th, uint32_t level,
    PixelBufferDescriptor&& data, FaceOffsets faceOffsets) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "updateCubeImage must be called outside of a render pass.");
        auto tex = handle_cast<MetalTexture>(th);
        tex->loadCubeImage(faceOffsets, level, data);
        scheduleDestroy(std.move(data));
    }

    fun setupExternalImage(void* image) {
        // This is called when passing in a CVPixelBuffer as either an external image or swap chain.
        // Here we take ownership of the passed in buffer. It will be released the next time
        // setExternalImage is called, when the texture is destroyed, or when the swap chain is
        // destroyed.
        CVPixelBufferRef pixelBuffer = (CVPixelBufferRef) image;
        CVPixelBufferRetain(pixelBuffer);
    }

    fun cancelExternalImage(void* image) {
        CVPixelBufferRef pixelBuffer = (CVPixelBufferRef) image;
        CVPixelBufferRelease(pixelBuffer);
    }

    fun setExternalImage(Handle<HwTexture> th, void* image) {
        auto texture = handle_cast<MetalTexture>(th);
        texture->externalImage.set((CVPixelBufferRef) image);
    }

    fun setExternalImagePlane(Handle<HwTexture> th, void* image, uint32_t plane) {
        auto texture = handle_cast<MetalTexture>(th);
        texture->externalImage.set((CVPixelBufferRef) image, plane);
    }

    fun setExternalStream(Handle<HwTexture> th, Handle<HwStream> sh) {
    }

    bool fun getTimerQueryValue(Handle<HwTimerQuery> tqh, uint64_t* elapsedTime) {
        auto* tq = handle_cast<MetalTimerQuery>(tqh);
        return mContext->timerQueryImpl->getQueryResult(tq, elapsedTime);
    }

    SyncStatus fun getSyncStatus(Handle<HwSync> sh) {
        auto* fence = handle_cast<MetalFence>(sh);
        FenceStatus status = fence->wait(0);
        if (status == FenceStatus.TIMEOUT_EXPIRED) {
            return SyncStatus.NOT_SIGNALED;
        } else if (status == FenceStatus.CONDITION_SATISFIED) {
            return SyncStatus.SIGNALED;
        }
        return SyncStatus.ERROR;
    }*/

    override fun generateMipmaps(hwTexture: HwTexture) {
        if (metalContext.isInRenderPass()) error("generateMipmaps must be called outside of a render pass.")
        val metalTexture = hwTexture as MetalTexture
        val blitEncoder = metalContext.pendingCommandBuffer?.blitCommandEncoder() ?: error("fail to get blitCommandEncoder")
        metalTexture.texture?.let { texture ->
            blitEncoder.generateMipmapsForTexture(texture)
            blitEncoder.endEncoding()
            metalTexture.minLod = 0u
            metalTexture.maxLod = texture.mipmapLevelCount.toUInt() - 1u
        }
    }

    override fun canGenerateMipmaps(): Boolean {
        return true;
    }
/*
    fun updateSamplerGroup(Handle<HwSamplerGroup> sbh,
    SamplerGroup&& samplerGroup) {
        auto sb = handle_cast<MetalSamplerGroup>(sbh);
        *sb->sb = samplerGroup;
    }

    fun beginRenderPass(Handle<HwRenderTarget> rth,
    const RenderPassParams& params) {
        auto renderTarget = handle_cast<MetalRenderTarget>(rth);
        mContext->currentRenderTarget = renderTarget;
        mContext->currentRenderPassFlags = params.flags;

        MTLRenderPassDescriptor* descriptor = [MTLRenderPassDescriptor renderPassDescriptor];
        renderTarget->setUpRenderPassAttachments(descriptor, params);

        mContext->currentRenderPassEncoder =
        [getPendingCommandBuffer(mContext) renderCommandEncoderWithDescriptor:descriptor];
        if (!mContext->groupMarkers.empty()) {
                mContext->currentRenderPassEncoder.label =
            [NSString stringWithCString:mContext->groupMarkers.top()
            encoding:NSUTF8StringEncoding];
        }

        // Flip the viewport, because Metal's screen space is vertically flipped that of Filament's.
        NSInteger renderTargetHeight =
        mContext->currentRenderTarget->isDefaultRenderTarget() ?
        mContext->currentReadSwapChain->getSurfaceHeight() : mContext->currentRenderTarget->height;
        MTLViewport metalViewport {
            .originX = static_cast<double>(params.viewport.left),
            .originY = renderTargetHeight - static_cast<double>(params.viewport.bottom) -
                static_cast<double>(params.viewport.height),
            .width = static_cast<double>(params.viewport.width),
            .height = static_cast<double>(params.viewport.height),
            .znear = static_cast<double>(params.depthRange.near),
            .zfar = static_cast<double>(params.depthRange.far)
        };
        [mContext->currentRenderPassEncoder setViewport:metalViewport];

        // Metal requires a new command encoder for each render pass, and they cannot be reused.
        // We must bind certain states for each command encoder, so we dirty the states here to force a
        // rebinding at the first the draw call of this pass.
        mContext->pipelineState.invalidate();
        mContext->depthStencilState.invalidate();
        mContext->cullModeState.invalidate();
        mContext->windingState.invalidate();
    }

    fun nextSubpass(int dummy) {}

    fun endRenderPass(int dummy) {
        [mContext->currentRenderPassEncoder endEncoding];

        // Command encoders are one time use. Set it to nil to release the encoder and ensure we don't
        // accidentally use it again.
        mContext->currentRenderPassEncoder = nil;
    }*/

    override fun setRenderPrimitiveBuffer(rph: HwRenderPrimitive, vbh: HwVertexBuffer, ibh: HwIndexBuffer) {
        val primitive = rph as MetalRenderPrimitive
        val vertexBuffer = vbh as MetalVertexBuffer
        val indexBuffer = ibh as MetalIndexBuffer
        primitive.setBuffers(vertexBuffer, indexBuffer);
    }

    override fun setRenderPrimitiveRange(
        rph: HwRenderPrimitive,
        pt: PrimitiveType,
        offset: ULong,
        minIndex: Int,
        maxIndex: Int,
        count: Int
    ) {
        val primitive = rph as MetalRenderPrimitive
        primitive.type = pt
        primitive.offset = offset * (primitive.indexBuffer?.elementSize ?: error(""))
        primitive.count = count
        primitive.minIndex = minIndex
        primitive.maxIndex = if (maxIndex > minIndex) maxIndex else primitive.maxVertexCount - 1
    }
/*
    fun makeCurrent(Handle<HwSwapChain> schDraw, Handle<HwSwapChain> schRead) {
        ASSERT_PRECONDITION_NON_FATAL(schDraw, "A draw SwapChain must be set.")
        auto* drawSwapChain = handle_cast<MetalSwapChain>(schDraw)
        mContext.currentDrawSwapChain = drawSwapChain

        if (schRead) {
            auto* readSwapChain = handle_cast<MetalSwapChain>(schRead)
            mContext.currentReadSwapChain = readSwapChain
        }
    }

    fun commit(Handle<HwSwapChain> sch) {
        auto* swapChain = handle_cast<MetalSwapChain>(sch)
        swapChain.present()
        submitPendingCommands(mContext);
        swapChain.releaseDrawable();
    }

    fun bindUniformBuffer(uint32_t index, Handle<HwBufferObject> boh) {
        auto* bo = handle_cast<MetalBufferObject>(boh);
        auto* currentBo = mContext.uniformState[index].buffer;
        if (currentBo) {
                currentBo.boundUniformBuffers.unset(index);
        }
        bo.boundUniformBuffers.set(index);
        mContext.uniformState[index] = UniformBufferState{
            .buffer = bo,
            .offset = 0,
            .bound = true
        };
    }

    fun bindUniformBufferRange(uint32_t index, Handle<HwBufferObject> boh,
    uint32_t offset, uint32_t size) {
        auto* bo = handle_cast<MetalBufferObject>(boh);
        auto* currentBo = mContext.uniformState[index].buffer;
        if (currentBo) {
                currentBo.boundUniformBuffers.unset(index);
        }
        bo->boundUniformBuffers.set(index);
        mContext->uniformState[index] = UniformBufferState{
            .buffer = bo,
            .offset = offset,
            .bound = true
        };
    }

    fun bindSamplers(uint32_t index, Handle<HwSamplerGroup> sbh) {
        auto sb = handle_cast<MetalSamplerGroup>(sbh);
        mContext->samplerBindings[index] = sb;
    }

    fun insertEventMarker(const char* string, uint32_t len) {

    }

    fun pushGroupMarker(const char* string, uint32_t len) {
        mContext->groupMarkers.push(string);
    }

    fun popGroupMarker(int) {
        assert_invariant(!mContext->groupMarkers.empty());
        mContext->groupMarkers.pop();
    }

    fun startCapture(int) {
        if (@available(iOS 13, *)) {
            MTLCaptureDescriptor* descriptor = [MTLCaptureDescriptor new];
            descriptor.captureObject = mContext->device;
            descriptor.destination = MTLCaptureDestinationGPUTraceDocument;
            descriptor.outputURL = [[NSURL alloc] initFileURLWithPath:@"filament.gputrace"];
            NSError* error = nil;
            [[MTLCaptureManager sharedCaptureManager] startCaptureWithDescriptor:descriptor
                    error:&error];
            if (error) {
                NSLog(@"%@", [error localizedDescription]);
            }
        } else {
            // This compile-time check is used to silence deprecation warnings when compiling for the
            // iOS simulator, which only supports Metal on iOS 13.0+.
            #if (TARGET_OS_IOS && __IPHONE_OS_VERSION_MIN_REQUIRED < __IPHONE_13_0)
                [[MTLCaptureManager sharedCaptureManager] startCaptureWithDevice:mContext->device];
            #endif
        }
    }

    fun stopCapture(int) {
        [[MTLCaptureManager sharedCaptureManager] stopCapture];
    }

    fun readPixels(Handle<HwRenderTarget> src, uint32_t x, uint32_t y, uint32_t width,
    uint32_t height, PixelBufferDescriptor&& data) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "readPixels must be called outside of a render pass.");

        auto srcTarget = handle_cast<MetalRenderTarget>(src);
        // We always readPixels from the COLOR0 attachment.
        MetalRenderTarget.Attachment color = srcTarget->getDrawColorAttachment(0);
        id<MTLTexture> srcTexture = color.getTexture();
        size_t miplevel = color.level;

        // Clamp height and width to actual texture's height and width
        MTLSize srcTextureSize = MTLSizeMake(srcTexture.width >> miplevel, srcTexture.height >> miplevel, 1);
        height = std.min(static_cast<uint32_t>(srcTextureSize.height), height);
        width = std.min(static_cast<uint32_t>(srcTextureSize.width), width);

        const MTLPixelFormat format = getMetalFormat(data.format, data.type);
        ASSERT_PRECONDITION(format != MTLPixelFormatInvalid,
            "The chosen combination of PixelDataFormat (%d) and PixelDataType (%d) is not supported for "
            "readPixels.", (int) data.format, (int) data.type);

        const bool formatConversionNecessary = srcTexture.pixelFormat != format;

        // TODO: MetalBlitter does not currently support format conversions to integer types.
        // The format and type must match the source pixel format exactly.
        ASSERT_PRECONDITION(!formatConversionNecessary || !isMetalFormatInteger(format),
            "readPixels does not support integer format conversions from MTLPixelFormat (%d) to (%d).",
            (int) srcTexture.pixelFormat, (int) format);

        MTLTextureDescriptor* textureDescriptor =
            [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:format
                    width:srcTextureSize.width
        height:srcTextureSize.height
        mipmapped:NO];
        #if defined(IOS)
        textureDescriptor.storageMode = MTLStorageModeShared;
        #else
        textureDescriptor.storageMode = MTLStorageModeManaged;
        #endif
        textureDescriptor.usage = MTLTextureUsageShaderWrite | MTLTextureUsageRenderTarget;
        id<MTLTexture> readPixelsTexture = [mContext->device newTextureWithDescriptor:textureDescriptor];

        MetalBlitter.BlitArgs args;
        args.filter = SamplerMagFilter.NEAREST;
        args.source.level = miplevel;
        args.source.region = MTLRegionMake2D(0, 0, srcTexture.width >> miplevel, srcTexture.height >> miplevel);
        args.destination.level = 0;
        args.destination.region = MTLRegionMake2D(0, 0, readPixelsTexture.width, readPixelsTexture.height);
        args.source.color = srcTexture;
        args.destination.color = readPixelsTexture;

        mContext->blitter->blit(getPendingCommandBuffer(mContext), args);

        #if !defined(IOS)
        // Managed textures on macOS require explicit synchronization between GPU / CPU.
        id <MTLBlitCommandEncoder> blitEncoder = [getPendingCommandBuffer(mContext) blitCommandEncoder];
        [blitEncoder synchronizeResource:readPixelsTexture];
        [blitEncoder endEncoding];
        #endif

        PixelBufferDescriptor* p = new PixelBufferDescriptor(std.move(data));
        [getPendingCommandBuffer(mContext) addCompletedHandler:^(id<MTLCommandBuffer> cb) {
            size_t stride = p->stride ? p->stride : width;
            size_t bpp = PixelBufferDescriptor.computeDataSize(p->format, p->type, 1, 1, 1);
            size_t bpr = PixelBufferDescriptor.computeDataSize(p->format, p->type, stride, 1, p->alignment);
            // Metal's texture coordinates have (0, 0) at the top-left of the texture, but readPixels
            // assumes (0, 0) at bottom-left.
            MTLRegion srcRegion = MTLRegionMake2D(x, readPixelsTexture.height - y - height, width, height);
            const uint8_t* bufferStart = (const uint8_t*) p->buffer + (p->left * bpp) +
            (p->top * bpr);
            [readPixelsTexture getBytes:(void*) bufferStart
                    bytesPerRow:bpr
            fromRegion:srcRegion
            mipmapLevel:0];
            scheduleDestroy(std.move(*p));
        }];
    }

    fun blit(TargetBufferFlags buffers,
    Handle<HwRenderTarget> dst, Viewport dstRect,
    Handle<HwRenderTarget> src, Viewport srcRect,
    SamplerMagFilter filter) {
        // If we're the in middle of a render pass, finish it.
        // This condition should only occur during copyFrame. It's okay to end the render pass because
        // we don't issue any other rendering commands.
        if (mContext->currentRenderPassEncoder) {
            [mContext->currentRenderPassEncoder endEncoding];
            mContext->currentRenderPassEncoder = nil;
        }

        auto srcTarget = handle_cast<MetalRenderTarget>(src);
        auto dstTarget = handle_cast<MetalRenderTarget>(dst);

        ASSERT_PRECONDITION(
            !(buffers & (TargetBufferFlags.COLOR_ALL & ~TargetBufferFlags.COLOR0)),
        "Blitting only supports COLOR0");

        ASSERT_PRECONDITION(srcRect.left >= 0 && srcRect.bottom >= 0 &&
                dstRect.left >= 0 && dstRect.bottom >= 0,
            "Source and destination rects must be positive.");

        // Metal's texture coordinates have (0, 0) at the top-left of the texture, but Filament's
        // coordinates have (0, 0) at bottom-left.
        const NSInteger srcHeight =
            srcTarget->isDefaultRenderTarget() ?
        mContext->currentReadSwapChain->getSurfaceHeight() : srcTarget->height;
        MTLRegion srcRegion = MTLRegionMake2D(
                (NSUInteger) srcRect.left,
        srcHeight - (NSUInteger) srcRect.bottom - srcRect.height,
        srcRect.width, srcRect.height);

        const NSInteger dstHeight =
            dstTarget->isDefaultRenderTarget() ?
        mContext->currentDrawSwapChain->getSurfaceHeight() : dstTarget->height;
        MTLRegion dstRegion = MTLRegionMake2D(
                (NSUInteger) dstRect.left,
        dstHeight - (NSUInteger) dstRect.bottom - dstRect.height,
        dstRect.width, dstRect.height);

        auto isBlitableTextureType = [](MTLTextureType t) {
            return t == MTLTextureType2D || t == MTLTextureType2DMultisample ||
                    t == MTLTextureType2DArray;
        };

        MetalBlitter.BlitArgs args;
        args.filter = filter;
        args.source.region = srcRegion;
        args.destination.region = dstRegion;

        if (any(buffers & TargetBufferFlags.COLOR_ALL)) {
            // We always blit from/to the COLOR0 attachment.
            MetalRenderTarget.Attachment srcColorAttachment = srcTarget->getReadColorAttachment(0);
            MetalRenderTarget.Attachment dstColorAttachment = dstTarget->getDrawColorAttachment(0);

            if (srcColorAttachment && dstColorAttachment) {
                ASSERT_PRECONDITION(isBlitableTextureType(srcColorAttachment.getTexture().textureType) &&
                        isBlitableTextureType(dstColorAttachment.getTexture().textureType),
                    "Metal does not support blitting to/from non-2D textures.");

                args.source.color = srcColorAttachment.getTexture();
                args.destination.color = dstColorAttachment.getTexture();
                args.source.level = srcColorAttachment.level;
                args.destination.level = dstColorAttachment.level;
                args.source.slice = srcColorAttachment.layer;
                args.destination.slice = dstColorAttachment.layer;
            }
        }

        if (any(buffers & TargetBufferFlags.DEPTH)) {
            MetalRenderTarget.Attachment srcDepthAttachment = srcTarget->getDepthAttachment();
            MetalRenderTarget.Attachment dstDepthAttachment = dstTarget->getDepthAttachment();

            if (srcDepthAttachment && dstDepthAttachment) {
                ASSERT_PRECONDITION(isBlitableTextureType(srcDepthAttachment.getTexture().textureType) &&
                        isBlitableTextureType(dstDepthAttachment.getTexture().textureType),
                    "Metal does not support blitting to/from non-2D textures.");

                args.source.depth = srcDepthAttachment.getTexture();
                args.destination.depth = dstDepthAttachment.getTexture();

                if (args.blitColor()) {
                    // If blitting color, we've already set the source and destination levels and slices.
                    // Check that they match the requested depth levels/slices.
                    ASSERT_PRECONDITION(args.source.level == srcDepthAttachment.level,
                        "Color and depth source LOD must match. (%d != %d)",
                        args.source.level, srcDepthAttachment.level);
                    ASSERT_PRECONDITION(args.destination.level == dstDepthAttachment.level,
                        "Color and depth destination LOD must match. (%d != %d)",
                        args.destination.level, dstDepthAttachment.level);
                    ASSERT_PRECONDITION(args.source.slice == srcDepthAttachment.layer,
                        "Color and depth source layer must match. (%d != %d)",
                        args.source.slice, srcDepthAttachment.layer);
                    ASSERT_PRECONDITION(args.destination.slice == dstDepthAttachment.layer,
                        "Color and depth destination layer must match. (%d != %d)",
                        args.destination.slice, dstDepthAttachment.layer);
                }

                args.source.level = srcDepthAttachment.level;
                args.destination.level = dstDepthAttachment.level;
                args.source.slice = srcDepthAttachment.layer;
                args.destination.slice = dstDepthAttachment.layer;
            }
        }

        mContext->blitter->blit(getPendingCommandBuffer(mContext), args);
    }

    fun draw(PipelineState ps, Handle<HwRenderPrimitive> rph, uint32_t instanceCount) {
        ASSERT_PRECONDITION(mContext->currentRenderPassEncoder != nullptr,
        "Attempted to draw without a valid command encoder.");
        auto primitive = handle_cast<MetalRenderPrimitive>(rph);
        auto program = handle_cast<MetalProgram>(ps.program);
        const auto& rs = ps.rasterState;

        // If the material debugger is enabled, avoid fatal (or cascading) errors and that can occur
        // during the draw call when the program is invalid. The shader compile error has already been
        // dumped to the console at this point, so it's fine to simply return early.
        if (FILAMENT_ENABLE_MATDBG && UTILS_UNLIKELY(!program->isValid)) {
            return;
        }

        ASSERT_PRECONDITION(program->isValid, "Attempting to draw with an invalid Metal program.");

        // Pipeline state
        MTLPixelFormat colorPixelFormat[MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT] = { MTLPixelFormatInvalid };
        for (size_t i = 0; i < MRT.MAX_SUPPORTED_RENDER_TARGET_COUNT; i++) {
            const auto& attachment = mContext->currentRenderTarget->getDrawColorAttachment(i);
            if (!attachment) {
                continue;
            }
            colorPixelFormat[i] = attachment.getPixelFormat();
        }
        MTLPixelFormat depthPixelFormat = MTLPixelFormatInvalid;
        const auto& depthAttachment = mContext->currentRenderTarget->getDepthAttachment();
        if (depthAttachment) {
            depthPixelFormat = depthAttachment.getPixelFormat();
        }
        MetalPipelineState pipelineState {
            .vertexFunction = program->vertexFunction,
            .fragmentFunction = program->fragmentFunction,
            .vertexDescription = primitive->vertexDescription,
            .colorAttachmentPixelFormat = {
            colorPixelFormat[0],
            colorPixelFormat[1],
            colorPixelFormat[2],
            colorPixelFormat[3],
            colorPixelFormat[4],
            colorPixelFormat[5],
            colorPixelFormat[6],
            colorPixelFormat[7]
        },
            .depthAttachmentPixelFormat = depthPixelFormat,
            .sampleCount = mContext->currentRenderTarget->getSamples(),
            .blendState = BlendState {
            .blendingEnabled = rs.hasBlending(),
            .rgbBlendOperation = getMetalBlendOperation(rs.blendEquationRGB),
            .alphaBlendOperation = getMetalBlendOperation(rs.blendEquationAlpha),
            .sourceRGBBlendFactor = getMetalBlendFactor(rs.blendFunctionSrcRGB),
            .sourceAlphaBlendFactor = getMetalBlendFactor(rs.blendFunctionSrcAlpha),
            .destinationRGBBlendFactor = getMetalBlendFactor(rs.blendFunctionDstRGB),
            .destinationAlphaBlendFactor = getMetalBlendFactor(rs.blendFunctionDstAlpha)
        },
            .colorWrite = rs.colorWrite
        };
        mContext->pipelineState.updateState(pipelineState);
        if (mContext->pipelineState.stateChanged()) {
            id<MTLRenderPipelineState> pipeline =
            mContext->pipelineStateCache.getOrCreateState(pipelineState);
            assert_invariant(pipeline != nil);
            [mContext->currentRenderPassEncoder setRenderPipelineState:pipeline];
        }

        // Cull mode
        MTLCullMode cullMode = getMetalCullMode(rs.culling);
        mContext->cullModeState.updateState(cullMode);
        if (mContext->cullModeState.stateChanged()) {
            [mContext->currentRenderPassEncoder setCullMode:cullMode];
        }

        // Front face winding
        MTLWinding winding = rs.inverseFrontFaces ? MTLWindingClockwise : MTLWindingCounterClockwise;
        mContext->windingState.updateState(winding);
        if (mContext->windingState.stateChanged()) {
            [mContext->currentRenderPassEncoder setFrontFacingWinding:winding];
        }

        // Set the depth-stencil state, if a state change is needed.
        DepthStencilState depthState;
        if (depthAttachment) {
            depthState.compareFunction = getMetalCompareFunction(rs.depthFunc);
            depthState.depthWriteEnabled = rs.depthWrite;
        }
        mContext->depthStencilState.updateState(depthState);
        if (mContext->depthStencilState.stateChanged()) {
            id<MTLDepthStencilState> state =
            mContext->depthStencilStateCache.getOrCreateState(depthState);
            assert_invariant(state != nil);
            [mContext->currentRenderPassEncoder setDepthStencilState:state];
        }

        if (ps.polygonOffset.constant != 0.0 || ps.polygonOffset.slope != 0.0) {
            [mContext->currentRenderPassEncoder setDepthBias:ps.polygonOffset.constant
            slopeScale:ps.polygonOffset.slope
            clamp:0.0];
        }

        // FIXME: implement take ps.scissor into account
        //  must be intersected with viewport (see OpenGLDriver.cpp for implementation details)

        // Bind uniform buffers.
        MetalBuffer* uniformsToBind[Program.BINDING_COUNT] = { nil };
        NSUInteger offsets[Program.BINDING_COUNT] = { 0 };

        enumerateBoundUniformBuffers([&uniformsToBind, &offsets](const UniformBufferState& state,
        MetalBuffer* buffer, uint32_t index) {
            uniformsToBind[index] = buffer;
            offsets[index] = state.offset;
        });
        MetalBuffer.bindBuffers(getPendingCommandBuffer(mContext), mContext->currentRenderPassEncoder,
        0, MetalBuffer.Stage.VERTEX | MetalBuffer.Stage.FRAGMENT, uniformsToBind, offsets,
        Program.BINDING_COUNT);

        // Enumerate all the sampler buffers for the program and check which textures and samplers need
        // to be bound.

        auto getTextureToBind = [this](const SamplerGroup.Sampler* sampler) {
            const auto metalTexture = handle_const_cast<MetalTexture>(sampler->t);
            id<MTLTexture> textureToBind = metalTexture->swizzledTextureView ? metalTexture->swizzledTextureView
            : metalTexture->texture;
            if (metalTexture->externalImage.isValid()) {
            textureToBind = metalTexture->externalImage.getMetalTextureForDraw();
        }
            return textureToBind;
        };

        auto getSamplerToBind = [this](const SamplerGroup.Sampler* sampler) {
            const auto metalTexture = handle_const_cast<MetalTexture>(sampler->t);
            SamplerState s {
                .samplerParams = sampler->s,
                .minLod = metalTexture->minLod,
                .maxLod = metalTexture->maxLod
            };
            return mContext->samplerStateCache.getOrCreateState(s);
        };

        id<MTLTexture> texturesToBindVertex[MAX_VERTEX_SAMPLER_COUNT] = {};
        id<MTLSamplerState> samplersToBindVertex[MAX_VERTEX_SAMPLER_COUNT] = {};

        enumerateSamplerGroups(program, ShaderType.VERTEX,
            [this, &getTextureToBind, &getSamplerToBind, &texturesToBindVertex, &samplersToBindVertex](
        const SamplerGroup.Sampler* sampler, uint8_t binding) {
            // We currently only support a max of MAX_VERTEX_SAMPLER_COUNT samplers. Ignore any additional
            // samplers that may be bound.
            if (binding >= MAX_VERTEX_SAMPLER_COUNT) {
                return;
            }

            auto& textureToBind = texturesToBindVertex[binding];
            textureToBind = getTextureToBind(sampler);
            if (!textureToBind) {
                utils.slog.w << "Warning: no texture bound at binding point " << (size_t) binding
                << " at the vertex shader." << utils.io.endl;
                textureToBind = getOrCreateEmptyTexture(mContext);
            }

            auto& samplerToBind = samplersToBindVertex[binding];
            samplerToBind = getSamplerToBind(sampler);
        });

        // Assign a default sampler to empty slots, in case Filament hasn't bound all samplers.
        // Metal requires all samplers referenced in shaders to be bound.
        for (auto& sampler : samplersToBindVertex) {
            if (!sampler) {
                sampler = mContext->samplerStateCache.getOrCreateState({});
            }
        }

        NSRange vertexSamplerRange = NSMakeRange(0, MAX_VERTEX_SAMPLER_COUNT);
        [mContext->currentRenderPassEncoder setVertexTextures:texturesToBindVertex
        withRange:vertexSamplerRange];
        [mContext->currentRenderPassEncoder setVertexSamplerStates:samplersToBindVertex
        withRange:vertexSamplerRange];

        id<MTLTexture> texturesToBindFragment[MAX_FRAGMENT_SAMPLER_COUNT] = {};
        id<MTLSamplerState> samplersToBindFragment[MAX_FRAGMENT_SAMPLER_COUNT] = {};

        enumerateSamplerGroups(program, ShaderType.FRAGMENT,
            [this, &getTextureToBind, &getSamplerToBind, &texturesToBindFragment, &samplersToBindFragment](
        const SamplerGroup.Sampler* sampler, uint8_t binding) {
            // We currently only support a max of MAX_FRAGMENT_SAMPLER_COUNT samplers. Ignore any additional
            // samplers that may be bound.
            if (binding >= MAX_FRAGMENT_SAMPLER_COUNT) {
                return;
            }

            auto& textureToBind = texturesToBindFragment[binding];
            textureToBind = getTextureToBind(sampler);
            if (!textureToBind) {
                utils.slog.w << "Warning: no texture bound at binding point " << (size_t) binding
                << " at the fragment shader." << utils.io.endl;
                textureToBind = getOrCreateEmptyTexture(mContext);
            }

            auto& samplerToBind = samplersToBindFragment[binding];
            samplerToBind = getSamplerToBind(sampler);
        });

        // Assign a default sampler to empty slots, in case Filament hasn't bound all samplers.
        // Metal requires all samplers referenced in shaders to be bound.
        for (auto& sampler : samplersToBindFragment) {
            if (!sampler) {
                sampler = mContext->samplerStateCache.getOrCreateState({});
            }
        }

        NSRange fragmentSamplerRange = NSMakeRange(0, MAX_FRAGMENT_SAMPLER_COUNT);
        [mContext->currentRenderPassEncoder setFragmentTextures:texturesToBindFragment
        withRange:fragmentSamplerRange];
        [mContext->currentRenderPassEncoder setFragmentSamplerStates:samplersToBindFragment
        withRange:fragmentSamplerRange];

        // Bind the vertex buffers.

        MetalBuffer* buffers[MAX_VERTEX_BUFFER_COUNT];
        size_t vertexBufferOffsets[MAX_VERTEX_BUFFER_COUNT];
        size_t bufferIndex = 0;

        auto vb = primitive->vertexBuffer;
        for (uint32_t attributeIndex = 0; attributeIndex < vb->attributes.size(); attributeIndex++) {
            const auto& attribute = vb->attributes[attributeIndex];
            if (attribute.buffer == Attribute.BUFFER_UNUSED) {
                continue;
            }

            assert_invariant(vb->buffers[attribute.buffer]);
            buffers[bufferIndex] = vb->buffers[attribute.buffer];
            vertexBufferOffsets[bufferIndex] = attribute.offset;
            bufferIndex++;
        }

        const auto bufferCount = bufferIndex;
        MetalBuffer.bindBuffers(getPendingCommandBuffer(mContext), mContext->currentRenderPassEncoder,
        VERTEX_BUFFER_START, MetalBuffer.Stage.VERTEX, buffers,
        vertexBufferOffsets, bufferCount);

        // Bind the zero buffer, used for missing vertex attributes.
        static const char bytes[16] = { 0 };
        [mContext->currentRenderPassEncoder setVertexBytes:bytes
        length:16
        atIndex:(VERTEX_BUFFER_START + ZERO_VERTEX_BUFFER)];

        MetalIndexBuffer* indexBuffer = primitive->indexBuffer;

        id<MTLCommandBuffer> cmdBuffer = getPendingCommandBuffer(mContext);
        id<MTLBuffer> metalIndexBuffer = indexBuffer->buffer.getGpuBufferForDraw(cmdBuffer);
        size_t offset = indexBuffer->buffer.getGpuBufferStreamOffset();
        [mContext->currentRenderPassEncoder drawIndexedPrimitives:getMetalPrimitiveType(primitive->type)
        indexCount:primitive->count
        indexType:getIndexType(indexBuffer->elementSize)
        indexBuffer:metalIndexBuffer
        indexBufferOffset:primitive->offset + offset
        instanceCount:instanceCount];
    }

    fun beginTimerQuery(Handle<HwTimerQuery> tqh) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "beginTimerQuery must be called outside of a render pass.");
        auto* tq = handle_cast<MetalTimerQuery>(tqh);
        mContext->timerQueryImpl->beginTimeElapsedQuery(tq);
    }

    fun endTimerQuery(Handle<HwTimerQuery> tqh) {
        ASSERT_PRECONDITION(!isInRenderPass(mContext),
            "endTimerQuery must be called outside of a render pass.");
        auto* tq = handle_cast<MetalTimerQuery>(tqh);
        mContext->timerQueryImpl->endTimeElapsedQuery(tq);
    }

    fun enumerateSamplerGroups(
    const MetalProgram* program, ShaderType shaderType,
    const std.function<void(const SamplerGroup.Sampler*, size_t)>& f) {
        auto& samplerBlockInfo = (shaderType == ShaderType.VERTEX) ?
        program->vertexSamplerBlockInfo : program->fragmentSamplerBlockInfo;
        auto maxSamplerCount = (shaderType == ShaderType.VERTEX) ?
        MAX_VERTEX_SAMPLER_COUNT : MAX_FRAGMENT_SAMPLER_COUNT;
        for (size_t bindingIdx = 0; bindingIdx != maxSamplerCount; ++bindingIdx) {
            auto& blockInfo = samplerBlockInfo[bindingIdx];
            if (blockInfo.samplerGroup == UINT8_MAX) {
                continue;
            }

            const auto* metalSamplerGroup = mContext->samplerBindings[blockInfo.samplerGroup];
            if (!metalSamplerGroup) {
                // Do not emit warning here. For example this can arise when skinning is enabled
                // and the morphing texture is unused.
                continue;
            }

            SamplerGroup* sb = metalSamplerGroup->sb.get();
            const SamplerGroup.Sampler* boundSampler = sb->getSamplers() + blockInfo.sampler;

            if (!boundSampler->t) {
            continue;
        }

            f(boundSampler, bindingIdx);
        }
    }

    fun enumerateBoundUniformBuffers(
    const std.function<void(const UniformBufferState&, MetalBuffer*, uint32_t)>& f) {
        for (uint32_t i = 0; i < Program.BINDING_COUNT; i++) {
            auto& thisUniform = mContext->uniformState[i];
            if (!thisUniform.bound) {
                continue;
            }
            f(thisUniform, thisUniform.buffer->getBuffer(), i);
        }
    }
    */

}

