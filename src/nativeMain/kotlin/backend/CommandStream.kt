package backend

class CommandStream(
    val driver: Driver,
    val buffer: CircularBuffer,
    val mDispatcher: Dispatcher = driver.dispatcher
) {

}

typealias DriverApi = CommandStream