package moe.pure.browser.feature.library.navigation

/** bb.v.w/l8.a: exact URL identity, last destination wins without changing insertion order. */
internal class LibraryOpenQueue {
    private val requests = mutableListOf<LibraryOpenRequest>()

    fun add(request: LibraryOpenRequest): Boolean {
        if (request.url.isEmpty()) return false
        val index = requests.indexOfFirst { it.url == request.url }
        if (index < 0) requests.add(request) else requests[index] = request
        return true
    }

    fun snapshot(): List<LibraryOpenRequest> = requests.toList()
}
