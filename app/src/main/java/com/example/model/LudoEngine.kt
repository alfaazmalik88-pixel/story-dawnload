package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class LudoColor(val value: Color, val hex: String) {
    RED(Color(0xFFE53935), "#E53935"),
    GREEN(Color(0xFF4CAF50), "#4CAF50"),
    YELLOW(Color(0xFFFBC02D), "#FBC02D"),
    BLUE(Color(0xFF1E88E5), "#1E88E5")
}

enum class PlayerType {
    HUMAN,
    BOT,
    ABSENT
}

enum class GamePhase {
    SPLASH,
    MODE_SELECT,
    SETUP,
    PLAYING,
    FINISHED
}

enum class LudoGameMode(val title: String, val description: String) {
    CLASSIC("Classic Game", "Traditional 4-player Ludo match with custom players"),
    ONE_VS_ONE("1v1 Match", "Quick 5-Minute Match (1 Human vs 1 AI computer opposite color)"),
    VS_COMPUTER("Computer Game", "Play solo against 3 intelligent AI computer bots"),
    TEAM_UP("Team Up (2v2)", "Team A (Red & Yellow) vs Team B (Green & Blue) - Partner is opposite to you!"),
    HYBRID_ONLINE("Online Game", "Live multiplayer lobby with real-time AI fallback takeover")
}

data class Player(
    val id: Int, // 0: Red, 1: Green, 2: Yellow, 3: Blue
    val name: String,
    val color: LudoColor,
    val type: PlayerType,
    val isCompleted: Boolean = false,
    val level: Int = 1,
    val avatarId: Int = 0
)

data class Token(
    val id: Int, // 0 to 3
    val playerId: Int,
    val position: Int = 0, // 0: Yard, 1..51: Common Path, 52..56: Home Stretch, 57: Reached Home
    val color: LudoColor
)

object LudoCoordinates {
    // Clockwise 52-cell circular path on 15x15 grid
    val commonBoardPath = listOf(
        // Left arm top (going right)
        6 to 0, 6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
        // Top arm left (going up)
        5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6, 0 to 6,
        // Top edge
        0 to 7,
        // Top arm right (going down)
        0 to 8, 1 to 8, 2 to 8, 3 to 8, 4 to 8, 5 to 8,
        // Right arm top (going right)
        6 to 9, 6 to 10, 6 to 11, 6 to 12, 6 to 13, 6 to 14,
        // Right edge
        7 to 14,
        // Right arm bottom (going left)
        8 to 14, 8 to 13, 8 to 12, 8 to 11, 8 to 10, 8 to 9,
        // Bottom arm right (going down)
        9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8,
        // Bottom edge
        14 to 7,
        // Bottom arm left (going up)
        14 to 6, 13 to 6, 12 to 6, 11 to 6, 10 to 6, 9 to 6,
        // Left arm bottom (going left)
        8 to 5, 8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
        // Left edge
        7 to 0
    )

    // Player starting indices in commonBoardPath
    val startIndices = mapOf(
        0 to 1,   // RED starts at (6, 1) - index 1
        1 to 14,  // GREEN starts at (1, 8) - index 14
        2 to 27,  // YELLOW starts at (8, 13) - index 27
        3 to 40   // BLUE starts at (13, 6) - index 40
    )

    // Player home entrance points (the index in commonBoardPath FROM which they turn into home stretch)
    val entranceIndices = mapOf(
        0 to 0,   // RED turns from (6, 0) - index 0
        1 to 12,  // GREEN turns from (0, 7) - index 12
        2 to 26,  // YELLOW turns from (8, 14) - index 26
        3 to 38   // BLUE turns from (14, 7) - index 38
    )

    // 5 Home stretch cells for each player
    val homeStretches = mapOf(
        0 to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5), // RED (row 7, col 1..5)
        1 to listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7), // GREEN (col 7, row 1..5)
        2 to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9), // YELLOW (row 7, col 13..9)
        3 to listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7) // BLUE (col 7, row 13..9)
    )

    // Center Home cell for each player
    val centerHomes = mapOf(
        0 to (7 to 6), // RED
        1 to (6 to 7), // GREEN
        2 to (7 to 8), // YELLOW
        3 to (8 to 7)  // BLUE
    )

    // Safe cells indices (in commonBoardPath) where tokens can't be cut
    // Star markings usually present on these coordinates:
    // Red Start (6,1), Green Start (1,8), Yellow Start (8,13), Blue Start (13,6)
    // and four extra star cells (8,2), (2,6), (6,12), (12,8)
    val safeCellsOnPath = setOf(1, 9, 14, 22, 27, 35, 40, 48)

    // Token default Yard (base) positions (fractional grid coordinates for beautiful centering)
    val yardOffsets = mapOf(
        0 to listOf(1.5f to 1.5f, 1.5f to 3.5f, 3.5f to 1.5f, 3.5f to 3.5f), // RED (top-left)
        1 to listOf(1.5f to 10.5f, 1.5f to 12.5f, 3.5f to 10.5f, 3.5f to 12.5f), // GREEN (top-right)
        2 to listOf(10.5f to 10.5f, 10.5f to 12.5f, 12.5f to 10.5f, 12.5f to 12.5f), // YELLOW (bottom-right)
        3 to listOf(10.5f to 1.5f, 10.5f to 3.5f, 12.5f to 1.5f, 12.5f to 3.5f)  // BLUE (bottom-left)
    )

    /**
     * Get 15x15 grid coordinates for a token.
     * Returns Pair(row, col) as floats so we can slightly offset stacked tokens!
     */
    fun getTokenCoordinates(playerId: Int, tokenId: Int, position: Int): Pair<Float, Float> {
        if (position == 0) {
            // Yard position
            return yardOffsets[playerId]?.get(tokenId) ?: (0f to 0f)
        }

        if (position == 57) {
            // Reached center home. Put them deeply inside their respective home triangles (around absolute center 7.5, 7.5)!
            return when (playerId) {
                0 -> { // RED: Left triangle (centered around row=7.5f, col=6.7f)
                    when (tokenId) {
                        0 -> 6.65f to 6.35f
                        1 -> 7.35f to 6.35f
                        2 -> 7.0f to 6.75f
                        else -> 7.0f to 5.85f
                    }
                }
                1 -> { // GREEN: Top triangle (centered around row=6.7f, col=7.5f)
                    when (tokenId) {
                        0 -> 6.35f to 6.65f
                        1 -> 6.35f to 7.35f
                        2 -> 6.75f to 7.0f
                        else -> 5.85f to 7.0f
                    }
                }
                2 -> { // YELLOW: Right triangle (centered around row=7.5f, col=8.3f)
                    when (tokenId) {
                        0 -> 6.65f to 7.65f
                        1 -> 7.35f to 7.65f
                        2 -> 7.0f to 7.25f
                        else -> 7.0f to 8.15f
                    }
                }
                else -> { // BLUE: Bottom triangle (centered around row=8.3f, col=7.5f)
                    when (tokenId) {
                        0 -> 7.65f to 6.65f
                        1 -> 7.65f to 7.35f
                        2 -> 7.25f to 7.0f
                        else -> 8.15f to 7.0f
                    }
                }
            }
        }

        if (position in 52..56) {
            // Home stretch
            val cell = homeStretches[playerId]?.get(position - 52) ?: (7 to 7)
            return cell.first.toFloat() to cell.second.toFloat()
        }

        // Common Board Path
        val startIdx = startIndices[playerId] ?: 0
        val entranceIdx = entranceIndices[playerId] ?: 0

        // How many steps has it traveled?
        // position starts at 1, which maps to startIdx
        val commonIndex = (startIdx + position - 1) % 52
        val cell = commonBoardPath[commonIndex]
        return cell.first.toFloat() to cell.second.toFloat()
    }

    /**
     * Helper to check if a specific grid cell (row, col) is safe.
     */
    fun isCellSafe(row: Int, col: Int): Boolean {
        val index = commonBoardPath.indexOf(row to col)
        if (index != -1 && safeCellsOnPath.contains(index)) {
            return true
        }
        return false
    }
}
