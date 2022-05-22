package filament

import backend.SamplerType
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Mat3
import kotlin.math.min

const val IBL_INTEGRATION_PREFILTERED_CUBEMAP = 0
const val IBL_INTEGRATION_IMPORTANCE_SAMPLING = 1
const val IBL_INTEGRATION = IBL_INTEGRATION_PREFILTERED_CUBEMAP

const val DEFAULT_INTENSITY = 30000.0f    // lux of the sun

class IndirectLight {

    class Builder(
        var mReflectionsMap: Texture? = null,
        var mIrradianceMap: Texture? = null,
        val mIrradianceCoefs: Array<Float> = Array(9) { 65504.0f }, // magic value (max fp16) to indicate sh are not set
        val mRotation: Mat3 = Mat3(),
        val mIntensity: Float = DEFAULT_INTENSITY
    ) {

        /**
         * Set the reflections cubemap mipmap chain.
         *
         * @param cubemap   A mip-mapped cubemap generated by **cmgen**. Each cubemap level
         *                  encodes a the irradiance for a roughness level.
         *
         * @return This Builder, for chaining calls.
         *
         */
        fun reflections(cubemap: Texture) {
            TODO("not yet implemented")
        }

        /**
         * Sets the irradiance as Spherical Harmonics.
         *
         * The irradiance must be pre-convolved by \f$ \langle n \cdot l \rangle \f$ and
         * pre-multiplied by the Lambertian diffuse BRDF \f$ \frac{1}{\pi} \f$ and
         * specified as Spherical Harmonics coefficients.
         *
         * Additionally, these Spherical Harmonics coefficients must be pre-scaled by the
         * reconstruction factors \f$ A_{l}^{m} \f$ below.
         *
         * The final coefficients can be generated using the `cmgen` tool.
         *
         * The index in the \p sh array is given by:
         *
         *  `index(l, m) = l * (l + 1) + m`
         *
         *  \f$ sh[index(l,m)] = L_{l}^{m} \frac{1}{\pi} A_{l}^{m} \hat{C_{l}} \f$
         *
         *   index |  l  |  m  |  \f$ A_{l}^{m} \f$ |  \f$ \hat{C_{l}} \f$  |  \f$ \frac{1}{\pi} A_{l}^{m}\hat{C_{l}} \f$ |
         *  :-----:|:---:|:---:|:------------------:|:---------------------:|:--------------------------------------------:
         *     0   |  0  |  0  |      0.282095      |       3.1415926       |   0.282095
         *     1   |  1  | -1  |     -0.488602      |       2.0943951       |  -0.325735
         *     2   |  ^  |  0  |      0.488602      |       ^               |   0.325735
         *     3   |  ^  |  1  |     -0.488602      |       ^               |  -0.325735
         *     4   |  2  | -2  |      1.092548      |       0.785398        |   0.273137
         *     5   |  ^  | -1  |     -1.092548      |       ^               |  -0.273137
         *     6   |  ^  |  0  |      0.315392      |       ^               |   0.078848
         *     7   |  ^  |  1  |     -1.092548      |       ^               |  -0.273137
         *     8   |  ^  |  2  |      0.546274      |       ^               |   0.136569
         *
         *
         * Only 1, 2 or 3 bands are allowed.
         *
         * @param bands     Number of spherical harmonics bands. Must be 1, 2 or 3.
         * @param sh        Array containing the spherical harmonics coefficients.
         *                  The size of the array must be \f$ bands^{2} \f$.
         *                  (i.e. 1, 4 or 9 coefficients respectively).
         *
         * @return This Builder, for chaining calls.
         *
         * @note
         * Because the coefficients are pre-scaled, `sh[0]` is the environment's
         * average irradiance.
         */
        fun irradiance(bands: Int, sh: Array<Float>) {
            // clamp to 3 bands for now
            val bands = min(bands, 3)
            val numCoefs = bands * bands
            mIrradianceCoefs.fill(0f, 0, mIrradianceCoefs.lastIndex)
            sh.copyInto(mIrradianceCoefs, 0, 0, numCoefs - 1)
        }


        fun build(engine: Engine): IndirectLight? {
            mReflectionsMap?.let { reflectionsMap ->
                if (reflectionsMap.target != SamplerType.SAMPLER_CUBEMAP) {
                    println("reflection map must a cubemap")
                    return null;
                }

                if (IBL_INTEGRATION == IBL_INTEGRATION_IMPORTANCE_SAMPLING) {
                    reflectionsMap.generateMipmaps(engine);
                }
            }

            mIrradianceMap?.let { irradianceMap ->
                if (irradianceMap.target != SamplerType.SAMPLER_CUBEMAP) {
                    println("irradiance map must a cubemap")
                    return null;
                }
            }

            return engine.createIndirectLight(this);
        }
    }
}