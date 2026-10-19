package fr.m2.pokequiz

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
