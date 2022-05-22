package backend

class MRT(color: TargetBufferInfo) {


    companion object {
        const val MIN_SUPPORTED_RENDER_TARGET_COUNT: UShort = 4u;

        // When updating this, make sure to also take care of RenderTarget.java
        const val MAX_SUPPORTED_RENDER_TARGET_COUNT: UShort = 8u;
    }

    private val infos = Array(MAX_SUPPORTED_RENDER_TARGET_COUNT.toInt()) { TargetBufferInfo() }

    // [] operator
    operator fun get(index: Int) = infos[index]

    init {
        infos[0] = color
    }
}