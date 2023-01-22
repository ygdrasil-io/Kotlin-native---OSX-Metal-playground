package interop

import kotlinx.cinterop.*
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import platform.darwin.simd_float3Var
import platform.darwin.simd_float3x3
import platform.darwin.simd_float4x4

fun simd_float3x3.insert(data: D2Array<Float>) {
    objcPtr()
        .insert(data.data)
}

fun simd_float4x4.insert(data: D2Array<Float>) {
    objcPtr()
        .insert(data.data)
}

private fun NativePtr.insert(data: Iterable<Float>) {
    data.forEachIndexed { index, value ->
        val pointer = plus(index * sizeOf<FloatVar>())
            .let(::interpretOpaquePointed)
            .reinterpret<FloatVar>()
        //println("index $index ${this.objcPtr()}")
        pointer.value = value
    }
}

fun simd_float3Var.insert(data: Iterable<Float>) {
    objcPtr()
        .insert(data)
}