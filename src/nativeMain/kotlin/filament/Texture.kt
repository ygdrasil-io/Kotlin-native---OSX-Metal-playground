package filament

import backend.*

class Texture(

    val target: SamplerType = SamplerType.SAMPLER_2D,
    val width: UInt = 1u,
    val height: UInt = 1u,
    val levelCount: ULong = 1u,
    val format: TextureFormat = TextureFormat.RGBA8,
    val mHandle: HwTexture,
    val mSampleCount: UShort = 1u
/*
FStream* mStream = nullptr;
uint32_t mDepth = 1;
Usage mUsage = Usage.DEFAULT;*/
) {

    /**
     * Generates all the mipmap levels automatically. This requires the texture to have a
     * color-renderable format.
     *
     * @param engine        Engine this texture is associated to.
     *
     * @attention \p engine must be the instance passed to Builder.build()
     * @attention This Texture instance must NOT use Sampler.SAMPLER_CUBEMAP or it has no effect
     */
    fun generateMipmaps(engine: Engine) {
        val driver = engine.driver
        if (target == SamplerType.SAMPLER_EXTERNAL) {
            println("External Textures are not mipmappable.")
            return
        }

        val formatMipmappable = engine.driver.isTextureFormatMipmappable(format);
        if (!formatMipmappable) {
            println("Texture format $format is not mipmappable.")
            return;
        }

        if (levelCount < 2u || (width == 1u && height == 1u)) {
            return;
        }

        if (engine.driver.canGenerateMipmaps()) {
            engine.driver.generateMipmaps(mHandle)
            return
        }

        val generateMipsForLayer : (TargetBufferInfo) -> Unit = { proto ->

            // Wrap miplevel 0 in a render target so that we can use it as a blit source.
            var level = 0
            val srcw = width
            val srch = height
            proto.handle = mHandle
            proto.level = level++
            val srcrth = driver.createRenderTarget(
                    TargetBufferFlags.COLOR, srcw, srch, sampleCount, proto
            )

            // Perform a blit for all miplevels down to 1x1.
            var dstrth: HwRenderTarget
            do {
                uint32_t dstw = std . max (srcw > > 1u, 1u);
                uint32_t dsth = std . max (srch > > 1u, 1u);
                proto.level = level++;
                dstrth = driver.createRenderTarget(
                    TargetBufferFlags.COLOR, dstw, dsth, mSampleCount, proto, {}, {});
                driver.blit(
                    TargetBufferFlags.COLOR,
                    dstrth, { 0, 0, dstw, dsth },
                    srcrth, { 0, 0, srcw, srch },
                    SamplerMagFilter.LINEAR
                );
                //driver.destroyRenderTarget(srcrth);
                srcrth = dstrth;
                srcw = dstw;
                srch = dsth;
            } while ((srcw > 1 || srch > 1) && level < levelCount);
            driver.destroyRenderTarget(dstrth)
        };

        when (target) {
            SamplerType.SAMPLER_2D -> generateMipsForLayer(TargetBufferInfo());

            SamplerType.SAMPLER_2D_ARRAY -> for (uint16_t layer = 0
                , c = mDepth; layer < c; ++layer) {
            generateMipsForLayer({ .layer = layer });
        }
            SamplerType.SAMPLER_CUBEMAP -> for (uint8_t face = 0; face < 6; ++face
                ) {
                generateMipsForLayer({ .layer = face });
            }
            SamplerType.SAMPLER_EXTERNAL -> Unit
            // not mipmapable

            SamplerType.SAMPLER_3D -> println("Texture.generateMipmap does not support SAMPLER_3D yet on this h/w.")
            // TODO: handle SAMPLER_3D -- this can't be done with a 2D blit, this would require
            //       a fragment shader

        }
    }

    class Builder(
        var width: ULong = 1u,
        var height: ULong = 1u,
        var level: ULong = 1u,
        var format: TextureFormat = TextureFormat.RGBA8,
        var sampler: SamplerType = SamplerType.SAMPLER_2D,
        var usage: TextureUsage = TextureUsage.DEFAULT,
        var textureIsSwizzled: Boolean = false,
        var importedId: UInt = 0u
    ) {


        fun build(engine: Engine): Texture? {
            if (!isTextureFormatSupported(engine, format)) {
                println("Texture format $format not supported on this platform")
                return null
            }

            val sampleable = (usage.value and TextureUsage.SAMPLEABLE.value) > 0
            val isSwizzled = textureIsSwizzled
            val imported = importedId != 0u

            /* TODO: "handle this on webgl"
            #if defined(__EMSCRIPTEN__)
            ASSERT_POSTCONDITION_NON_FATAL(!swizzled, "WebGL does not support texture swizzling.");
            #endif*/

            if ((isSwizzled && sampleable) || !isSwizzled) println("Swizzled texture must be SAMPLEABLE")
            if ((imported && sampleable) || !imported || !isSwizzled) println("Imported texture must be SAMPLEABLE")

            return engine.createTexture(this)
        }
    }
}

private fun isTextureFormatSupported(engine: Engine, format: TextureFormat): Boolean {
    return engine.driver.isTextureFormatSupported(format);
}
