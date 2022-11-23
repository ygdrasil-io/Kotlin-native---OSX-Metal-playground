import platform.Metal.MTLDeviceProtocol

val samples = listOf<Pair<String, (MTLDeviceProtocol) -> Renderer>>(
    "00 - Window" to { device -> Renderer00(device) },
    "01 - Primitive" to { device -> Renderer01(device) },
    "02 - Arg buffers" to { device -> Renderer02(device) },
    "03 - Animation" to { device -> Renderer03(device) }
)

fun main() {
    println("starting the app")
    samples.last().let { (title, sample) ->
        MetalApplication(title, sample).run()
    }

}
