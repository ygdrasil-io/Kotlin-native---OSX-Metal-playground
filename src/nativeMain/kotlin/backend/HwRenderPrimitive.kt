package backend

open class HwRenderPrimitive(
    var offset: ULong = 0u,
    var minIndex: Int = 0,
    var maxIndex: Int = 0,
    var count: Int = 0,
    val maxVertexCount: Int = 0,
    var type: PrimitiveType = PrimitiveType.TRIANGLES
) : HwBase()