package fr.m2.pokequiz

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Bonjour depuis ${platform.name} !"
}
