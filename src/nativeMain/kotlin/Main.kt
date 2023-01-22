import platform.Metal.MTLDeviceProtocol

val samples = listOf<Pair<String, (MTLDeviceProtocol) -> Renderer>>(
    "00 - Window" to { device -> Renderer00(device) },
    "01 - Primitive" to { device -> Renderer01(device) },
    "02 - Arg buffers" to { device -> Renderer02(device) },
    "03 - Animation" to { device -> Renderer03(device) },
    "04 - TODO" to { _ -> TODO() },
    "05 - TODO" to { _ -> TODO()},
    "06 - TODO" to { _ -> TODO() },
    "07 - Texture Mapping" to { device -> Renderer07(device) },
    "XX - Custom" to { device -> CustomRenderer(device) }
)

fun main() {
    println("starting the app")
    samples[7].let { (title, sample) ->
        MetalApplication(title, sample).run()
    }

}
