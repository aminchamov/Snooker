package com.elocho.snooker.ui.match

enum class SnookerScoreAction(
    val label: String,
    val points: Int
) {
    RED(label = "Red", points = 1),
    YELLOW(label = "Yellow", points = 2),
    GREEN(label = "Green", points = 3),
    BROWN(label = "Brown", points = 4),
    BLUE(label = "Blue", points = 5),
    PINK(label = "Pink", points = 6),
    BLACK(label = "Black", points = 7),
    ERROR(label = "Error", points = 4)
}
