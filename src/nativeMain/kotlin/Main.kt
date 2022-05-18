fun main() {
    println("starting the app")
    /*MetalApplication("00 - Window") { device ->
        Renderer01(device)
    }.run()*/
    MetalApplication("01 - Primitive") { device ->
        Renderer01(device)
    }.run()
}
