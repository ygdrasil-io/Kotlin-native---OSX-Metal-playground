package backend

import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.FloatVar

class BufferDescriptor(val sFullScreenTriangleVertices: CArrayPointer<FloatVar>, val sizeof: Long) {
}