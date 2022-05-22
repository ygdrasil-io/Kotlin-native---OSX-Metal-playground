package filament

class ResourceList<T>(val name: String){

    val objects = mutableListOf<T>()
}