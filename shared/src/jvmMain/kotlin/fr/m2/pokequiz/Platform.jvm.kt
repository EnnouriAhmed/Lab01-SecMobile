package fr.m2.pokequiz

class JVMPlatform : Platform {
    override val name: String =
        "Desktop (${System.getProperty("os.name")}) · Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
