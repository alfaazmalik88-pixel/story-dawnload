package com.example.model

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Request and Response Models for Gemini ---
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// --- Retrofit API Interface ---
interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini Service Implementation ---
object GeminiChatService {
    private const val TAG = "GeminiChatService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    // Pre-configured personalities
    private val systemInstructions = """
        You are simulating an active Ludo game chat with real human players. 
        There are up to 4 players, each having their specific color and a distinct human-like personality:
        - Rahul 🔴 (Red): Bold, competitive, bhai/bro vibe, likes to show off, gets frustrated when cut, loves capturing others' tokens.
        - Rajesh 🟢 (Green): Cheeky, extremely funny, sarcastic, loves roasting others with friendly humor, uses "bhai sahab", "arre yaar".
        - Sonia 🟡 (Yellow): Strategic, smart, sassy, clever, congratulates others but stays competitive, uses "aww", "sorry yaara", "easy peasy".
        - Kabir 🔵 (Blue): Cool, chill, relaxed, laid-back, says "chill bro", "no worries", doesn't care much about losing.

        You are the AI playing as these bots. 
        The human user is playing as one of the colors, and the rest of the active players are these bots.
        Your goal is to generate extremely realistic, short, lively chat reactions (1-2 sentences) in a mixture of Hindi and English (Hinglish / friendly Indian slang).
        Make it sound like real friends playing Ludo together in India!
        
        Rules:
        1. Keep the messages short and natural (under 120 characters).
        2. Speak in Hinglish (Hindi written in English script) or simple Hindi with English words mixed.
        3. Do NOT make the bots sound robotic or too polite. They should tease, roast, laugh, or complain!
        4. Match the requested bot personality.
        5. Output ONLY the response formatted exactly like:
           BotName: "The chat message"
           Example: Rajesh 🟢: "Arrey bhai sahab! Kya kismat hai, seedha 6 aa gaya! Ab taiyar ho jao goti katne ke liye! 😂"
    """.trimIndent()

    suspend fun getReaction(
        event: String,
        botName: String,
        gameStateSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or placeholder. Using fallback.")
            return@withContext getLocalFallback(event, botName)
        }

        val prompt = """
            Generate a reaction for $botName.
            Event: $event
            Current Game State: $gameStateSummary
            
            Remember to output ONLY in this format (no other text):
            $botName: "Your reaction here"
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructions))),
            generationConfig = GeminiGenerationConfig(temperature = 0.85f, maxOutputTokens = 100)
        )

        try {
            val response = api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrEmpty()) {
                Log.d(TAG, "Gemini reply: $text")
                return@withContext text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
        }

        return@withContext getLocalFallback(event, botName)
    }

    // Handles user manual chat messages
    suspend fun getReplyToUser(
        userMessage: String,
        userColor: String,
        activeBots: List<String>,
        gameStateSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or placeholder. Using fallback.")
            return@withContext getLocalChatFallback(userMessage, activeBots)
        }

        val prompt = """
            The human user (playing as $userColor) sent a message to the chat: "$userMessage"
            Active computer bots in the game: ${activeBots.joinToString(", ")}
            Game State: $gameStateSummary

            Choose ONE of the active bots that is most relevant (or select randomly among active bots) to reply to the user.
            Keep the reply extremely natural, funny, and matching their persona. Use Hinglish.
            Output ONLY the chosen bot's message formatted exactly as:
            BotName: "Reply text"
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructions))),
            generationConfig = GeminiGenerationConfig(temperature = 0.9f, maxOutputTokens = 120)
        )

        try {
            val response = api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrEmpty()) {
                Log.d(TAG, "Gemini reply to user: $text")
                return@withContext text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error replying to user: ${e.message}", e)
        }

        return@withContext getLocalChatFallback(userMessage, activeBots)
    }

    // Robust offline fallback list
    private fun getLocalFallback(event: String, botName: String): String {
        val cleanName = botName.substringBefore(" ").trim()
        val comment = when (cleanName) {
            "Rahul" -> when {
                event.contains("START") -> "Aaj toh koi mujhe hara nahi sakta! Let's play!"
                event.contains("ROLL_6") -> "Oho, 6! Ab dekhna meri speed! 🚀"
                event.contains("CUT") -> "Boom! Goti saaf! Beta ghar jao aur chai piyo! 😂"
                event.contains("CAPTURED") -> "Arrey yaar! Kisne meri goti kaati? Badla liya jayega! 😡"
                event.contains("WINNER") -> "Jeet gaya bhaisaab! Bolte thhe na king kaun hai! 🏆"
                else -> "Chalo chalo, jaldi khelo sab!"
            }
            "Rajesh" -> when {
                event.contains("START") -> "Bhaiyo aur behno, thoda dhyan se khelna, main bohot tagda khelta hoon! 😉"
                event.contains("ROLL_6") -> "Arrey shabaash, 6 aa gaya! Kismat toh meri hi khuli hai! 🎲"
                event.contains("CUT") -> "Bye bye goti! Rajesh ka nishana kabhi galat nahi hota! 🎯"
                event.contains("CAPTURED") -> "Arrey bhai sahab, yeh kya baat hui? Dhoka hai yeh! 😭"
                event.contains("WINNER") -> "Aur yeh Rajesh ne baazi maar li! Party kab chahiye batao? 🥳"
                else -> "Arrey jaldi chalo, kal subah Panvel nikalna hai! 🏃‍♂️"
            }
            "Sonia" -> when {
                event.contains("START") -> "All the best guys! Par jeetungi toh main hi! 😘"
                event.contains("ROLL_6") -> "Aww, look! Mujhko 6 mil gaya! Touchwood! ✨"
                event.contains("CUT") -> "Oops, so sorry yaara! Safe zone mein rehna chahiye tha na! 🤭"
                event.contains("CAPTURED") -> "Arey, kitne rude ho tum! Meri pyaari goti ko kaat diya! 🥺"
                event.contains("WINNER") -> "Yay! I won! Simple and sweet, no extra effort needed! 👑"
                else -> "So slow! Thoda fast khelo please!"
            }
            "Kabir" -> when {
                event.contains("START") -> "Yo guys! Mast game chalega aaj. Chill maaro!"
                event.contains("ROLL_6") -> "Yo, 6 aa gaya. Sahi hai, nice progress!"
                event.contains("CUT") -> "Arrey chill bro, game hai! Milte hain phir se start pe! 🤙"
                event.contains("CAPTURED") -> "Arey koi nahi bro, life mein ups and downs toh aate rehte hain!"
                event.contains("WINNER") -> "Aur hum jeet gaye! Sahi match thha, mazaa aaya! 🍻"
                else -> "Chill bro, take your time."
            }
            else -> "Arey yaar, jaldi khelo!"
        }
        return "$botName: \"$comment\""
    }

    private fun getLocalChatFallback(userMessage: String, activeBots: List<String>): String {
        if (activeBots.isEmpty()) return "System: No active bots."
        val chosenBot = activeBots.random()
        val cleanName = chosenBot.substringBefore(" ").trim()
        val reply = when (cleanName) {
            "Rahul" -> listOf(
                "Arey bro, focus karo game pe! Baatein baad mein करेंगे!",
                "Sahi bol rahe ho, par jeetunga toh main hi! 😉",
                "Arrey tension mat lo, goti toh main tumhari hi kaatunga!"
            ).random()
            "Rajesh" -> listOf(
                "Arrey bhai sahab! Baaton se Ludo nahi jita jata, chal chalo! 😂",
                "Hahaha! Sahi chal rahe ho, par Rajesh se bach ke rehna!",
                "Kya baat hai, bade majakiya ho yaar tum!"
            ).random()
            "Sonia" -> listOf(
                "Aww, cute! Par mere pass ek mast plan hai jeetne ka! 😉",
                "Haha! Let's see kisme kitna hai dum! 💃",
                "Nice chat, par game mein no mercy!"
            ).random()
            "Kabir" -> listOf(
                "Chill bro, sahi bol rahe ho! Fun game chal raha hai! 🤙",
                "Yo! Mast chal rahe ho. Party toh banti hai!",
                "Chill bro, tension mat lo, maze karo!"
            ).random()
            else -> "Chalo chalo, dhyan do game pe!"
        }
        return "$chosenBot: \"$reply\""
    }

    suspend fun getBotMoveChoice(
        validMoves: List<Token>,
        gameStateSummary: String,
        diceRoll: Int,
        botPlayerName: String
    ): Int = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or placeholder. Falling back.")
            return@withContext -1
        }

        val prompt = """
            You are playing Ludo as $botPlayerName. It is your turn.
            You rolled a $diceRoll.
            
            Current Game State:
            $gameStateSummary
            
            The valid tokens you can move are:
            ${validMoves.map { "Token ID: ${it.id} (Current Position: ${it.position})" }.joinToString("\n")}
            
            Rules & Strategies of Ludo:
            - Position 0 means the token is inside the starting Base/Yard.
            - A token can only release from position 0 to start on the track if the roll is 6.
            - A token goes around the track and enters the safe home path at position 52, reaching home at 57.
            - Think strategically:
              1. Choose a token that can capture/cut an opponent's token if moved (highest priority).
              2. Choose a token that can safely reach the Home (57) or safe zones.
              3. Choose a token that can escape danger (if an opponent is close behind).
              4. Release a token from base (if roll is 6).
              5. Advance the token furthest along the path.
            
            Analyze the game state like a grandmaster Ludo champion and select the SINGLE best Token ID to move.
            You must output ONLY the token ID as a single integer (0, 1, 2, or 3) and absolutely nothing else.
            Output format:
            TokenID
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "You are a Ludo AI grandmaster deciding the best move. Output ONLY the token ID as a single integer."))),
            generationConfig = GeminiGenerationConfig(temperature = 0.2f, maxOutputTokens = 10)
        )

        try {
            val response = api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrEmpty()) {
                Log.d(TAG, "Gemini move choice: $text")
                val digitOnly = text.filter { it.isDigit() }
                if (digitOnly.isNotEmpty()) {
                    return@withContext digitOnly.toInt()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bot move from Gemini: ${e.message}", e)
        }

        return@withContext -1
    }
}
