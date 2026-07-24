package com.example.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LudoAudioEngine
import com.example.audio.RealtimeVoiceManager
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LudoTheme(val displayName: String, val pawnName: String, val diceName: String) {
    CLASSIC("Classic Wood", "Standard Pin", "Traditional 3D Dice"),
    COSMIC("Cosmic Space", "Space Rocket", "Neon Laser Dice"),
    ROYAL("Royal Gold", "Golden Crown", "Royal Sceptre Dice"),
    FOREST("Wild Forest", "Leafy Token", "Stone Carved Dice"),
    CANDY("Candy Land", "Sweet Candy", "Jelly Dice"),
    OCEAN("Ocean Deep", "Ocean Starfish", "Coral Shell Dice"),
    CYBERPUNK("Cyber Neon", "Neon Circuit", "Synthwave Cube"),
    EGYPT("Pharaoh Gold", "Mini Pyramid", "Hieroglyphic Stone")
}

enum class LudoTokenStyle(val id: String, val displayName: String, val emoji: String, val cost: Int) {
    CLASSIC_PIN("classic_pin", "Standard Pin", "📍", 0),
    GLOSSY_3D("glossy_3d", "Glossy Metallic", "✨", 0),
    CLASSIC_PAWN("classic_pawn", "Classic 3D Pawn", "♟️", 0),
    SPACE_ROCKET("space_rocket", "Space Rocket", "🚀", 3000),
    GOLDEN_CROWN("golden_crown", "Golden Crown", "👑", 10000),
    SWEET_CANDY("sweet_candy", "Sweet Candy", "🍬", 20000),
    MAGIC_STAR("magic_star", "Magic Star", "⭐", 35000),
    FIRE_SKULL("fire_skull", "Fire Skull", "💀", 50000)
}

enum class LudoDiceStyle(val id: String, val displayName: String, val emoji: String, val cost: Int) {
    CLASSIC_DOTS("classic_dots", "Classic Dots", "🎲", 0),
    NEON_LASER("neon_laser", "Neon Laser", "⚡", 3000),
    ROYAL_SCEPTRE("royal_sceptre", "Royal Sceptre", "⚔️", 10000),
    OCEAN_SHELL("ocean_shell", "Ocean Shell", "🐚", 20000),
    ANCIENT_HIEROGLYPH("ancient_hieroglyph", "Ancient Stone", "📜", 35000),
    COSMIC_SINGULARITY("cosmic_singularity", "Cosmic Singularity", "🌀", 50000)
}

enum class LudoLanguage(
    val code: String,
    val countryName: String,
    val flag: String,
    val label: String
) {
    EN("en", "English", "🇺🇸", "English"),
    IN("hi_IN", "India", "🇮🇳", "हिन्दी / English"),
    SA("ar_SA", "Saudi Arabia", "🇸🇦", "العربية"),
    AE("ar_AE", "UAE", "🇦🇪", "العربية / English"),
    KW("ar_KW", "Kuwait", "🇰🇼", "العربية"),
    QA("ar_QA", "Qatar", "🇶🇦", "العربية"),
    OM("ar_OM", "Oman", "🇴🇲", "العربية"),
    BH("ar_BH", "Bahrain", "🇧🇭", "العربية"),
    BR("pt_BR", "Brazil", "🇧🇷", "Português"),
    MX("es_MX", "Mexico", "🇲🇽", "Español"),
    ID("in_ID", "Indonesia", "🇮🇩", "Bahasa Indonesia"),
    ES("es_ES", "Spain", "🇪🇸", "Español"),
    TR("tr_TR", "Turkey", "🇹🇷", "Türkçe"),
    RU("ru_RU", "Russia", "🇷🇺", "Русский")
}

object LudoTranslations {
    fun getTranslation(key: String, lang: LudoLanguage): String {
        return when (lang) {
            LudoLanguage.SA, LudoLanguage.KW, LudoLanguage.QA, LudoLanguage.OM, LudoLanguage.BH -> {
                when (key) {
                    "title" -> "لودو ستار"
                    "players_count" -> "👥 عدد اللاعبين"
                    "your_color" -> "🎨 لونك"
                    "choose_theme" -> "🎭 اختر المظهر والقطع"
                    "choose_language" -> "🌐 اختر اللغة"
                    "start_game" -> "ابدأ اللعبة"
                    "developed_by" -> "👑 تم التطوير بواسطة قمر بهان"
                    "rules_title" -> "قواعد لعبة اللودو"
                    "got_it" -> "حسناً"
                    "rules_1" -> "كل لاعب لديه 4 قطع في قاعدته. يجب الحصول على رقم 6 لإخراج القطعة إلى المسار."
                    "rules_2" -> "اختر أي قطعة مميزة بلونك لتحريكها باتجاه عقارب الساعة."
                    "rules_3" -> "الحصول على رقم 6 يمنحك دوراً إضافياً! الحصول على 6 ثلاث مرات متتالية يلغي دورك."
                    "rules_4" -> "الهبوط على قطعة الخصم يعيدها إلى الساحة ويمنحك لفة مجانية إضافية!"
                    "rules_5" -> "الزملاء في وضع الفريق لا يمكنهم أسر قطع بعضهم البعض ويتعايشون بأمان."
                    "rules_6" -> "ادخل مسار لونك وادهس المربع المركزي للفوز! الفائز من يوصل قطعه الأربعة أولاً."
                    "classic_title" -> "لودو كلاسيك"
                    "classic_desc" -> "مباراة لودو تقليدية لأربعة لاعبين"
                    "one_vs_one_title" -> "مواجهة 1 ضد 1"
                    "one_vs_one_desc" -> "مواجهة سريعة ومثيرة ضد خصم واحد"
                    "computer_title" -> "لعبة الكمبيوتر"
                    "computer_desc" -> "العب بمفردك ضد 3 من الروبوتات الذكية"
                    "team_up_title" -> "العمل الجماعي (2 ضد 2)"
                    "team_up_desc" -> "فريق الأحمر والأصفر ضد الأخضر والأزرق"
                    "back_confirm_title" -> "هل تريد الخروج من اللعبة؟"
                    "back_confirm_desc" -> "إذا عدت، ستفقد تقدمك الحالي والعملات التي راهنت بها! هل أنت متأكد أنك تريد العودة؟"
                    "back_confirm_desc_no_wager" -> "إذا عدت، ستفقد تقدمك في المباراة الحالية! هل أنت متأكد أنك تريد العودة؟"
                    "not_enough_coins" -> "⚠️ ليس لديك عملات كافية! تحتاج %d 🪙 للعب."
                    "select_mode_msg" -> "يرجى تحديد وضع اللعبة للبدء!"
                    "internet_required_title" -> "⚠️ الإنترنت الخاص بك مغلق!"
                    "internet_required_desc" -> "⚠️ الإنترنت الخاص بك مغلق! يرجى تشغيل الاتصال بالإنترنت للعب مباراة 1 ضد 1."
                    "reset_confirm_title" -> "هل تريد إعادة تعيين اللعبة؟"
                    "reset_confirm_desc" -> "سيؤدي هذا إلى إعادة تشغيل المباراة الحالية وإعادة جميع القطع إلى الساحة. هل أنت متأكد أنك تريد إعادة التعيين؟"
                    "yes" -> "نعم"
                    "no" -> "لا"
                    "get_six" -> "احصل على 6"
                    "six_active" -> "تم تفعيل 6"
                    "watching_ad" -> "مشاهدة إعلان كفيل 🎲"
                    "ad_guaranteed_six" -> "مشاهدة إعلان قصير للمطالبة بـ 6 مضمونة في لفتك القادمة!"
                    "ad_extend_time" -> "مشاهدة إعلان قصير لتمديد مؤقت المباراة بمقدار +5 دقائق!"
                    "ad_game_finish" -> "الخروج من اللعبة بعد إعلان قصير..."
                    "ad_reset" -> "إعادة تعيين المباراة بعد إعلان قصير..."
                    "ad_watching" -> "جاري مشاهدة الإعلان..."
                    "reward_claims" -> "مطالبة المكافأة في %ds..."
                    else -> key
                }
            }
            LudoLanguage.AE -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 PLAYERS COUNT / عدد اللاعبين"
                    "your_color" -> "🎨 YOUR COLOR / لونك"
                    "choose_theme" -> "🎭 CHOOSE THEME & GOTI / اختر المظهر والقطع"
                    "choose_language" -> "🌐 CHOOSE LANGUAGE / اختر اللغة"
                    "start_game" -> "START GAME / ابدأ"
                    "developed_by" -> "👑 Developed by Kamar Pathan"
                    "rules_title" -> "Ludo Rules / قواعد اللودو"
                    "got_it" -> "GOT IT / حسناً"
                    "rules_1" -> "Each player has 4 tokens. Roll a 6 to release a token."
                    "rules_2" -> "Select any highlighted token of your color to move."
                    "rules_3" -> "Rolling a 6 grants an extra turn! Three 6s cancels your turn."
                    "rules_4" -> "Land on an opponent's token to capture it and get an extra roll!"
                    "rules_5" -> "Teammates in Team Up mode cannot capture each other."
                    "rules_6" -> "Bring all 4 tokens to the home center to win!"
                    "classic_title" -> "Classic Ludo / لودو كلاسيك"
                    "classic_desc" -> "Traditional 4-player Ludo match / مباراة لودو تقليدية"
                    "one_vs_one_title" -> "1 vs 1 Match / مواجهة 1 ضد 1"
                    "one_vs_one_desc" -> "Fast-paced match against one opponent / مواجهة ضد خصم واحد"
                    "computer_title" -> "Vs Computer / ضد الكمبيوتر"
                    "computer_desc" -> "Play solo against smart AI bots / العب ضد الروبوتات"
                    "team_up_title" -> "Team Up 2v2 / وضع الفريق"
                    "team_up_desc" -> "Red & Yellow vs Green & Blue / فريق الأحمر والأصفر ضد الأخضر والأزرق"
                    "hybrid_online_title" -> "Online Game / لعبة أونلاين"
                    "hybrid_online_desc" -> "Live multiplayer lobby / غرفة انتظار متعددة اللاعبين"
                    "back_confirm_title" -> "Do you want to leave the game? / هل تريد الخروج؟"
                    "back_confirm_desc" -> "If you go back, your current game progress and wager will be lost! / ستفقد تقدمك الحالي والرهان إذا عدت."
                    "back_confirm_desc_no_wager" -> "If you go back, your current game progress will be lost! / ستفقد تقدمك الحالي إذا عدت."
                    "not_enough_coins" -> "⚠️ Not enough coins! You need %d 🪙 / ليس لديك عملات كافية!"
                    "select_mode_msg" -> "Select a Ludo Game Mode to play! / اختر وضع اللعبة للبدء!"
                    "internet_required_title" -> "⚠️ Internet Connection Off! / اتصال الإنترنت مغلق!"
                    "internet_required_desc" -> "⚠️ Your internet is turned off! Please turn on your internet connection to play 1v1 match. / الإنترنت مغلق! يرجى تشغيل الإنترنت للعب 1v1."
                    "reset_confirm_title" -> "Do you want to reset the match? / إعادة تعيين المباراة؟"
                    "reset_confirm_desc" -> "This will restart the current match. / سيتم إعادة تشغيل المباراة الحالية."
                    "yes" -> "Yes / نعم"
                    "no" -> "No / لا"
                    "get_six" -> "Get 6 / احصل على 6"
                    "six_active" -> "6 Active / نشط"
                    "watching_ad" -> "Watching Sponsor Video Ad 🎲"
                    "ad_guaranteed_six" -> "Watching a short ad to claim a guaranteed 6 on your next roll!"
                    "ad_extend_time" -> "Watching a short ad to extend your 1v1 match timer by +5 minutes!"
                    "ad_game_finish" -> "Exiting gameplay after a short sponsor ad..."
                    "ad_reset" -> "Resetting match after a short sponsor ad..."
                    "ad_watching" -> "Watching ad..."
                    "reward_claims" -> "Reward claims in %ds..."
                    else -> key
                }
            }
            LudoLanguage.BR -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 NÚMERO DE JOGADORES"
                    "your_color" -> "🎨 SUA COR"
                    "choose_theme" -> "🎭 ESCOLHER TEMA E PEÇAS"
                    "choose_language" -> "🌐 ESCOLHER IDIOMA"
                    "start_game" -> "INICIAR JOGO"
                    "developed_by" -> "👑 Desenvolvido por Kamar Pathan"
                    "rules_title" -> "Regras do Ludo"
                    "got_it" -> "ENTENDI"
                    "rules_1" -> "Cada jogador tem 4 peões. Tire um 6 para liberar um peão para a pista."
                    "rules_2" -> "Selecione qualquer peão destacado de sua cor para movê-lo."
                    "rules_3" -> "Tirar um 6 garante uma jogada extra! Três 6 seguidos anulam a jogada."
                    "rules_4" -> "Caia no peão de um oponente para capturá-lo e ganhar uma jogada grátis!"
                    "rules_5" -> "Parceiros no modo Duplas não podem se capturar e coexistem com segurança."
                    "rules_6" -> "Leve todos os 4 peões ao centro para vencer o jogo!"
                    "classic_title" -> "Ludo Clássico"
                    "classic_desc" -> "Partida tradicional de Ludo para 4 jogadores"
                    "one_vs_one_title" -> "Duelo 1 contra 1"
                    "one_vs_one_desc" -> "Duelo rápido contra um único oponente"
                    "computer_title" -> "Contra Computador"
                    "computer_desc" -> "Jogue sozinho contra 3 robôs inteligentes"
                    "team_up_title" -> "Modo Duplas (2v2)"
                    "team_up_desc" -> "Time Vermelho e Amarelo contra Verde e Azul"
                    "hybrid_online_title" -> "Jogo Online"
                    "hybrid_online_desc" -> "Lobby multiplayer ao vivo"
                    "back_confirm_title" -> "Deseja sair do jogo?"
                    "back_confirm_desc" -> "Se você voltar, o progresso do jogo atual e a sua aposta serão perdidos! Tem certeza de que deseja voltar?"
                    "back_confirm_desc_no_wager" -> "Se você voltar, o progresso do jogo atual será perdido! Tem certeza de que deseja voltar?"
                    "not_enough_coins" -> "⚠️ Moedas insuficientes! Você precisa de %d 🪙 para jogar."
                    "select_mode_msg" -> "Selecione um modo de jogo para começar!"
                    "internet_required_title" -> "⚠️ Internet Desconectada!"
                    "internet_required_desc" -> "⚠️ Sua internet está desligada! Por favor, ligue sua conexão com a internet para jogar o modo 1 contra 1."
                    "reset_confirm_title" -> "Deseja reiniciar a partida?"
                    "reset_confirm_desc" -> "Isso reiniciará a partida atual e retornará todas as peças para o pátio. Tem certeza de que deseja reiniciar?"
                    "yes" -> "Sim"
                    "no" -> "Não"
                    "get_six" -> "Obter 6"
                    "six_active" -> "6 Ativo"
                    "watching_ad" -> "Assistindo ao anúncio patrocinado 🎲"
                    "ad_guaranteed_six" -> "Assistindo a um anúncio curto para garantir um 6 na sua próxima jogada!"
                    "ad_extend_time" -> "Assistindo a um anúncio curto para estender o cronômetro em +5 minutos!"
                    "ad_game_finish" -> "Saindo do jogo após um anúncio curto..."
                    "ad_reset" -> "Reiniciando a partida após um anúncio curto..."
                    "ad_watching" -> "Assistindo ao anúncio..."
                    "reward_claims" -> "Reivindicação de recompensa em %ds..."
                    else -> key
                }
            }
            LudoLanguage.IN -> {
                when (key) {
                    "title" -> "क्राउन लूडो"
                    "players_count" -> "👥 खिलाड़ियों की संख्या"
                    "your_color" -> "🎨 आपका रंग"
                    "choose_theme" -> "🎭 थीम और गोटी चुनें"
                    "choose_language" -> "🌐 भाषा चुनें (Language)"
                    "start_game" -> "खेल शुरू करें"
                    "developed_by" -> "👑 Developed by Kamar Pathan"
                    "rules_title" -> "लूडो खेल के नियम"
                    "got_it" -> "समझ गए"
                    "rules_1" -> "प्रत्येक खिलाड़ी के पास 4 गोटियां होती हैं। गोटी बाहर निकालने के लिए 6 लाएं।"
                    "rules_2" -> "अपनी गोटी को चलाने के लिए हाइलाइट की गई गोटी पर टैप करें।"
                    "rules_3" -> "6 आने पर एक और बारी मिलती है! लगातार तीन बार 6 आने पर बारी रद्द हो जाती है।"
                    "rules_4" -> "विरोधी की गोटी पर कदम रखने से गोटी कट जाती है और आपको एक अतिरिक्त बारी मिलती है!"
                    "rules_5" -> "टीम अप मोड में साथी खिलाड़ी एक-दूसरे की गोटी नहीं काट सकते।"
                    "rules_6" -> "सभी 4 गोटियों को घर के अंदर (केंद्र में) पहुंचाने वाला खिलाड़ी जीतता है!"
                    "classic_title" -> "क्लासिक लूडो"
                    "classic_desc" -> "पारंपरिक 4-खिलाड़ी लूडो मैच"
                    "one_vs_one_title" -> "1 बनाम 1 मैच 🎯"
                    "one_vs_one_desc" -> "तेज़ गति वाला 1v1 मुकाबला"
                    "computer_title" -> "कंप्यूटर गेम"
                    "computer_desc" -> "3 स्मार्ट एआई बॉट्स के खिलाफ अकेले खेलें"
                    "team_up_title" -> "टीम अप (2v2)"
                    "team_up_desc" -> "लाल और पीला बनाम हरा और नीला टीम"
                    "hybrid_online_title" -> "मल्टीप्लेयर ऑनलाइन 🌐"
                    "hybrid_online_desc" -> "ऑनलाइन बहुत सारे लोगों के साथ खेलें (Bahut Online)"
                    "back_confirm_title" -> "क्या आप गेम छोड़ना चाहते हैं?"
                    "back_confirm_desc" -> "यदि आप वापस जाएंगे, तो चालू गेम का दांव (Wager) जब्त हो जाएगा और वापस नहीं मिलेगा! क्या आप वाकई वापस जाना चाहते हैं?"
                    "back_confirm_desc_no_wager" -> "यदि आप वापस जाएंगे, तो चालू गेम का प्रोग्रेस समाप्त हो जाएगा! क्या आप वाकई बाहर निकलना चाहते हैं?"
                    "not_enough_coins" -> "⚠️ आपके पास पर्याप्त सिक्के नहीं हैं! खेलने के लिए आपको %d 🪙 की आवश्यकता है।"
                    "select_mode_msg" -> "खेलने के लिए लूडो गेम मोड चुनें!"
                    "internet_required_title" -> "⚠️ आपका इंटरनेट बंद है!"
                    "internet_required_desc" -> "⚠️ आपका इंटरनेट बंद है! 1v1 मैच खेलने के लिए कृपया अपना इंटरनेट (Net) ऑन करें और दोबारा प्रयास करें।"
                    "reset_confirm_title" -> "क्या आप गेम रीसेट करना चाहते हैं?"
                    "reset_confirm_desc" -> "इससे आपका चालू मैच रीस्टार्ट हो जाएगा और सारी गोटियाँ वापस घर में चली जाएँगी। क्या आप वाकई रीसेट करना चाहते हैं?"
                    "yes" -> "हाँ"
                    "no" -> "नहीं"
                    "get_six" -> "6 लाएं (Get 6)"
                    "six_active" -> "6 चालू है"
                    "watching_ad" -> "प्रायोजक विज्ञापन देख रहे हैं 🎲"
                    "ad_guaranteed_six" -> "अपनी अगली चाल में निश्चित रूप से 6 पाने के लिए एक छोटा विज्ञापन देख रहे हैं!"
                    "ad_extend_time" -> "अपने 1v1 मैच के समय को +5 मिनट बढ़ाने के लिए विज्ञापन देख रहे हैं!"
                    "ad_game_finish" -> "एक छोटे प्रायोजक विज्ञापन के बाद गेम से बाहर जा रहे हैं..."
                    "ad_reset" -> "एक छोटे प्रायोजक विज्ञापन के बाद मैच को फिर से शुरू कर रहे हैं..."
                    "ad_watching" -> "विज्ञापन देख रहे हैं..."
                    "reward_claims" -> "पुरस्कार का दावा %ds में..."
                    "settings_title" -> "सेटिंग्स (Settings)"
                    "privacy_policy" -> "गोपनीयता नीति (Privacy Policy)"
                    "privacy_policy_desc" -> "हम आपकी गोपनीयता का सम्मान करते हैं। क्राउन लूडो पूरी तरह से ऑफलाइन काम करता है। आपका गेमप्ले, सेटिंग्स और आंकड़े पूरी तरह से आपके डिवाइस पर संग्रहीत होते हैं और कभी भी किसी के साथ साझा नहीं किए जाते हैं।"
                    "contact_us" -> "हमसे संपर्क करें (Contact Us)"
                    "contact_us_desc" -> "किसी भी सहायता, प्रतिक्रिया, प्रश्नों या व्यावसायिक पूछताछ के लिए कृपया हमसे इस पर संपर्क करें:"
                    "about" -> "विवरण (About)"
                    "about_desc" -> "क्राउन लूडो v1.2.0\nसुचारू गेमप्ले, सुंदर एनिमेशन और स्मार्ट एआई विरोधियों के लिए बनाया गया एक बेहतरीन ऑफलाइन लूडो गेम। भव्य थीम के साथ क्लासिक, 1v1 और टीम अप 2v2 मोड का आनंद लें।\n\nDeveloped by Kamar Pathan"
                    "copy_email" -> "ईमेल कॉपी करें"
                    "email_copied" -> "ईमेल क्लिपबोर्ड पर कॉपी हो गया!"
                    "profile_title" -> "खिलाड़ी प्रोफाइल"
                    "edit_username" -> "नाम बदलें"
                    "coins_label" -> "सिक्के (Coins)"
                    "daily_reward" -> "दैनिक पुरस्कार (Daily Reward)"
                    "daily_reward_desc" -> "मुफ़्त सिक्के पाने के लिए रोज़ाना गेम खोलें!"
                    "claim_btn" -> "दावा करें (+250 सिक्के)"
                    "claimed_btn" -> "आज का दावा हो गया"
                    "watch_ad_btn" -> "विज्ञापन देखें (+500)"
                    "watch_ad_desc" -> "मुफ़्त सोने के सिक्के कमाने के लिए एक छोटा विज्ञापन देखें!"
                    "enter_name_title" -> "नाम दर्ज करें"
                    "enter_name_placeholder" -> "अपना नाम लिखें..."
                    "save" -> "सुरक्षित करें"
                    "cancel" -> "रद्द करें"
                    "ad_watch_ad" -> "500 मुफ़्त सिक्के पाने के लिए प्रायोजक विज्ञापन देख रहे हैं!"
                    "shop_title" -> "थीम और गोटियों की दुकान"
                    "buy_btn" -> "%d 🪙 में खरीदें"
                    "use_btn" -> "थीम लागू करें"
                    "unlocked" -> "अनलॉक किया गया"
                    else -> key
                }
            }
            LudoLanguage.MX, LudoLanguage.ES -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 NÚMERO DE JUGADORES"
                    "your_color" -> "🎨 TU COLOR"
                    "choose_theme" -> "🎭 ELEGIR TEMA Y FICHAS"
                    "choose_language" -> "🌐 ELEGIR IDIOMA"
                    "start_game" -> "INICIAR JUEGO"
                    "developed_by" -> "👑 Desarrollado por Kamar Pathan"
                    "rules_title" -> "Reglas de Ludo"
                    "got_it" -> "ENTENDIDO"
                    "rules_1" -> "Cada jugador tiene 4 fichas. Saca un 6 para liberar una ficha a la pista activa."
                    "rules_2" -> "Selecciona cualquier ficha resaltada de tu color para moverla."
                    "rules_3" -> "¡Sacar un 6 otorga un turno extra! Tres 6 seguidos cancelan el turno."
                    "rules_4" -> "¡Cae sobre la ficha de un oponente para capturarla y obtener un tiro extra!"
                    "rules_5" -> "Los compañeros en el modo Equipos no pueden capturarse entre sí."
                    "rules_6" -> "¡Lleva las 4 fichas al centro para ganar el juego!"
                    "classic_title" -> "Ludo Clássico"
                    "classic_desc" -> "Partida tradicional de Ludo para 4 jugadores"
                    "one_vs_one_title" -> "Duelo 1 contra 1"
                    "one_vs_one_desc" -> "Partida rápida local contra un oponente"
                    "computer_title" -> "Contra Computadora"
                    "computer_desc" -> "Juega solo contra 3 bots inteligentes"
                    "team_up_title" -> "En Equipo (2v2)"
                    "team_up_desc" -> "Equipo Rojo y Amarillo contra Verde y Azul"
                    "back_confirm_title" -> "¿Quieres salir del juego?"
                    "back_confirm_desc" -> "Si vuelves atrás, se perderá tu progreso actual del juego y tu apuesta de monedas. ¿Estás seguro de que quieres volver?"
                    "back_confirm_desc_no_wager" -> "Si vuelves atrás, se perderá tu progreso actual del juego. ¿Estás seguro de que quieres volver?"
                    "not_enough_coins" -> "⚠️ ¡No tienes suficientes monedas! Necesitas %d 🪙 para jugar."
                    "select_mode_msg" -> "¡Selecciona un modo de juego para comenzar!"
                    "internet_required_title" -> "⚠️ ¡Tu Internet está apagado!"
                    "internet_required_desc" -> "⚠️ ¡Tu conexión a Internet está desactivada! Por favor, activa tu conexión a Internet para jugar la partida 1v1."
                    "reset_confirm_title" -> "¿Quieres reiniciar la partida?"
                    "reset_confirm_desc" -> "Esto reiniciará la partida actual y devolverá todas las fichas al patio. ¿Estás seguro de que deseas reiniciar?"
                    "yes" -> "Sí"
                    "no" -> "No"
                    "get_six" -> "Obtener 6"
                    "six_active" -> "6 Activo"
                    "watching_ad" -> "Viendo anuncio patrocinado 🎲"
                    "ad_guaranteed_six" -> "¡Viendo un anuncio corto para reclamar un 6 garantizado en tu próximo tiro!"
                    "ad_extend_time" -> "¡Viendo un anuncio corto para extender el tiempo de la partida en +5 minutos!"
                    "ad_game_finish" -> "Saliendo del juego después de un anuncio corto..."
                    "ad_reset" -> "Reiniciando la partida después de un anuncio corto..."
                    "ad_watching" -> "Viendo anuncio..."
                    "reward_claims" -> "Reclamo de recompensa en %ds..."
                    else -> key
                }
            }
            LudoLanguage.ID -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 JUMLAH PEMAIN"
                    "your_color" -> "🎨 WARNA ANDA"
                    "choose_theme" -> "🎭 PILIH TEMA & BIDAK"
                    "choose_language" -> "🌐 PILIH BAHASA"
                    "start_game" -> "MULAI PERMAINAN"
                    "developed_by" -> "👑 Dikembangkan oleh Kamar Pathan"
                    "rules_title" -> "Aturan Permainan Ludo"
                    "got_it" -> "MENGERTI"
                    "rules_1" -> "Setiap pemain memiliki 4 bidak. Lempar angka 6 untuk mengeluarkan bidak."
                    "rules_2" -> "Pilih bidak berwarna Anda yang disorot untuk memindahkannya."
                    "rules_3" -> "Mendapatkan angka 6 memberikan giliran tambahan! Tiga kali angka 6 membatalkan giliran."
                    "rules_4" -> "Mendarat di bidak lawan untuk menangkapnya dan dapatkan lemparan gratis!"
                    "rules_5" -> "Teman satu tim dalam mode Team Up tidak dapat menangkap satu sama lain."
                    "rules_6" -> "Bawa keempat bidak ke bagian tengah (rumah) untuk memenangkan permainan!"
                    "classic_title" -> "Ludo Klasik"
                    "classic_desc" -> "Pertandingan Ludo 4 pemain tradisional"
                    "one_vs_one_title" -> "Tanding 1 Lawan 1"
                    "one_vs_one_desc" -> "Pertandingan cepat melawan satu lawan"
                    "computer_title" -> "Lawan Komputer"
                    "computer_desc" -> "Main sendiri melawan 3 AI bot pintar"
                    "team_up_title" -> "Mode Tim (2v2)"
                    "team_up_desc" -> "Tim Merah & Kuning lawan Hijau & Biru"
                    "hybrid_online_title" -> "Game Online"
                    "hybrid_online_desc" -> "Lobby multipemain langsung"
                    "back_confirm_title" -> "Apakah Anda ingin keluar dari permainan?"
                    "back_confirm_desc" -> "Jika Anda kembali, kemajuan permainan saat ini dan taruhan koin Anda akan hilang! Apakah Anda yakin ingin kembali?"
                    "back_confirm_desc_no_wager" -> "Jika Anda kembali, kemajuan permainan saat ini akan hilang! Apakah Anda yakin ingin kembali?"
                    "not_enough_coins" -> "⚠️ Koin tidak cukup! Anda membutuhkan %d 🪙 untuk bermain."
                    "select_mode_msg" -> "Pilih mode permainan Ludo untuk mulai bermain!"
                    "internet_required_title" -> "⚠️ Koneksi Internet Mati!"
                    "internet_required_desc" -> "⚠️ Internet Anda mati! Silakan aktifkan koneksi internet Anda untuk memainkan pertandingan 1 Lawan 1."
                    "reset_confirm_title" -> "Apakah Anda ingin mengatur ulang permainan?"
                    "reset_confirm_desc" -> "Ini akan memulai ulang pertandingan saat ini dan mengembalikan semua bidak ke halaman. Apakah Anda yakin ingin mengatur ulang?"
                    "yes" -> "Ya"
                    "no" -> "Tidak"
                    "get_six" -> "Dapatkan 6"
                    "six_active" -> "6 Aktif"
                    "watching_ad" -> "Menonton Iklan Sponsor 🎲"
                    "ad_guaranteed_six" -> "Menonton iklan singkat untuk mendapatkan angka 6 yang dijamin pada lemparan berikutnya!"
                    "ad_extend_time" -> "Menonton iklan singkat untuk menambah waktu pertandingan 1v1 sebanyak +5 menit!"
                    "ad_game_finish" -> "Keluar dari permainan setelah iklan sponsor singkat..."
                    "ad_reset" -> "Mengatur ulang pertandingan setelah iklan sponsor singkat..."
                    "ad_watching" -> "Menonton iklan..."
                    "reward_claims" -> "Klaim hadiah dalam %ds..."
                    else -> key
                }
            }
            LudoLanguage.TR -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 OYUNCU SAYISI"
                    "your_color" -> "🎨 RENK SEÇİMİNİZ"
                    "choose_theme" -> "🎭 TEMA VE PULLAR"
                    "choose_language" -> "🌐 DİL SEÇİN"
                    "start_game" -> "OYUNU BAŞLAT"
                    "developed_by" -> "👑 Kamar Pathan tarafından geliştirilmiştir"
                    "rules_title" -> "Ludo Oyun Kuralları"
                    "got_it" -> "ANLADIM"
                    "rules_1" -> "Her oyuncunun 4 pulu vardır. Pulu oyuna sokmak için 6 atın."
                    "rules_2" -> "Hareket ettirmek için renginizdeki vurgulanan pullardan birini seçin."
                    "rules_3" -> "6 atmak ekstra bir hamle hakkı kazandırır! Üst üste üç kez 6 atmak sıranızı iptal eder."
                    "rules_4" -> "Rakibin pulunun üzerine gelerek onu kırın ve hanesine geri gönderin. Ekstra bir zar atma hakkı kazanırsınız!"
                    "rules_5" -> "Takım modunda takım arkadaşları birbirlerinin pullarını kıramazlar."
                    "rules_6" -> "Kendi renk yolunuza girin ve oyunu kazanmak için tam merkez hücreye (57) ulaşın! 4 pulu da eve getiren kazanır!"
                    "classic_title" -> "Klasik Ludo"
                    "classic_desc" -> "Geleneksel 4 oyunculu Ludo maçı"
                    "one_vs_one_title" -> "1'e 1 Maç"
                    "one_vs_one_desc" -> "Tek bir rakibe karşı hızlı yerel maç"
                    "computer_title" -> "Bilgisayara Karşı"
                    "computer_desc" -> "3 akıllı yapay zeka botuna karşı tek başınıza oynayın"
                    "team_up_title" -> "Takım Ol (2v2)"
                    "team_up_desc" -> "Kırmızı ve Sarı takım, Yeşil ve Mavi takıma karşı"
                    "hybrid_online_title" -> "Çevrimiçi Oyun"
                    "hybrid_online_desc" -> "Canlı çok oyunculu lobi"
                    "back_confirm_title" -> "Oyundan çıkmak istiyor musunuz?"
                    "back_confirm_desc" -> "Geri dönerseniz mevcut oyun ilerlemeniz ve jeton bahsiniz kaybolacaktır! Geri dönmek istediğinizden emin misiniz?"
                    "back_confirm_desc_no_wager" -> "Geri dönerseniz mevcut oyun ilerlemeniz kaybolacaktır! Geri dönmek istediğinizden emin misiniz?"
                    "not_enough_coins" -> "⚠️ Yetersiz bakiye! Oynamak için %d 🪙 jetona ihtiyacınız var."
                    "select_mode_msg" -> "Oynamak için bir Ludo Oyun Modu seçin!"
                    "internet_required_title" -> "⚠️ İnternet Bağlantınız Kapalı!"
                    "internet_required_desc" -> "⚠️ İnternetiniz kapalı! 1'e 1 maç oynamak için lütfen internet bağlantınızı açın."
                    "reset_confirm_title" -> "Maçı sıfırlamak istiyor musunuz?"
                    "reset_confirm_desc" -> "Bu işlem mevcut maçı yeniden başlatacak ve tüm pulları başlangıç hanesine geri döndürecektir. Sıfırlamak istediğinizden emin misiniz?"
                    "yes" -> "Evet"
                    "no" -> "Hayır"
                    "get_six" -> "6 Al"
                    "six_active" -> "6 Aktif"
                    "watching_ad" -> "Sponsor Reklamı İzleniyor 🎲"
                    "ad_guaranteed_six" -> "Bir sonraki zarınızda garantili bir 6 elde etmek için kısa bir reklam izleniyor!"
                    "ad_extend_time" -> "1v1 maç sürenizi +5 dakika uzatmak için kısa bir reklam izleniyor!"
                    "ad_game_finish" -> "Kısa bir sponsor reklamının ardından oyundan çıkılıyor..."
                    "ad_reset" -> "Kısa bir sponsor reklamının ardından maç sıfırlanıyor..."
                    "ad_watching" -> "Reklam izleniyor..."
                    "reward_claims" -> "Ödül talebi %ds içinde..."
                    "settings_title" -> "Ayarlar"
                    "privacy_policy" -> "Gizlilik Politikası"
                    "privacy_policy_desc" -> "Gizliliğinize değer veriyoruz. Crown Ludo tamamen çevrimdışı çalışır. Oyun verileriniz, ayarlarınız ve istatistikleriniz tamamen cihazınızda saklanır ve asla kimseyle paylaşılmaz."
                    "contact_us" -> "Bize Ulaşın"
                    "contact_us_desc" -> "Her türlü destek, geri bildirim, soru veya iş sorguları için lütfen bizimle iletişime geçin:"
                    "about" -> "Hakkında"
                    "about_desc" -> "Crown Ludo v1.2.0\nPürüzsüz oynanış, güzel animasyonlar ve akıllı yapay zeka rakipleri için tasarlanmış birinci sınıf, yüksek performanslı bir çevrimdışı Ludo oyunu. Muhteşem dinamik temalarla Klasik, 1v1 ve Takım Ol (2v2) modlarının tadını çıkarın.\n\nKamar Pathan tarafından geliştirilmiştir"
                    "copy_email" -> "E-postayı Kopyala"
                    "email_copied" -> "E-posta panoya kopyalandı!"
                    else -> key
                }
            }
            LudoLanguage.RU -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 КОЛИЧЕСТВО ИГРОКОВ"
                    "your_color" -> "🎨 ВАШ ЦВЕТ"
                    "choose_theme" -> "🎭 ВЫБРАТЬ ТЕМУ И ФИШКИ"
                    "choose_language" -> "🌐 ВЫБРАТЬ ЯЗЫК"
                    "start_game" -> "НАЧАТЬ ИГРУ"
                    "developed_by" -> "👑 Разработано Камаром Патханом"
                    "rules_title" -> "Правила игры в Лудо"
                    "got_it" -> "ПОНЯТНО"
                    "rules_1" -> "У каждого игрока есть 4 фишки. Выбросьте 6, чтобы вывести фишку на активную дорожку."
                    "rules_2" -> "Выберите любую подсвеченную фишку вашего цвета для перемещения."
                    "rules_3" -> "Выпадение 6 дает дополнительный ход! Три шестерки подряд отменяют ваш текущий ход."
                    "rules_4" -> "Приземлитесь на фишку соперника, чтобы срубить её и вернуть на базу. Вы получите дополнительный бесплатный бросок!"
                    "rules_5" -> "В командном режиме товарищи по команде не могут рубить фишки друг друга."
                    "rules_6" -> "Войдите на дорожку своего цвета и доберитесь до центральной клетки (57), чтобы завершить путь фишки. Приведите все 4 фишки домой для победы!"
                    "classic_title" -> "Классическое Лудо"
                    "classic_desc" -> "Традиционный матч в Лудо для 4 игроков"
                    "one_vs_one_title" -> "Матч 1 на 1"
                    "one_vs_one_desc" -> "Быстрый локальный матч против одного соперника"
                    "computer_title" -> "Против компьютера"
                    "computer_desc" -> "Играйте в Одиночку против 3 умных ИИ-ботов"
                    "team_up_title" -> "Командная игра (2v2)"
                    "team_up_desc" -> "Красная и Желтая команды против Зеленой и Синей"
                    "hybrid_online_title" -> "Онлайн Игра"
                    "hybrid_online_desc" -> "Живое онлайн-лобби"
                    "back_confirm_title" -> "Вы действительно хотите выйти из игры?"
                    "back_confirm_desc" -> "Если вы выйдете, текущий прогресс игры и ваша ставка монет будут потеряны! Вы уверены, что хотите вернуться?"
                    "back_confirm_desc_no_wager" -> "Если вы выйдете, текущий прогресс игры будет потерян! Вы уверены, что хотите вернуться?"
                    "not_enough_coins" -> "⚠️ Недостаточно монет! Для игры вам нужно %d 🪙."
                    "select_mode_msg" -> "Выберите режим игры Лудо для начала!"
                    "internet_required_title" -> "⚠️ Интернет отключен!"
                    "internet_required_desc" -> "⚠️ Ваш интернет отключен! Пожалуйста, включите интернет-соединение для игры в режиме 1 на 1."
                    "reset_confirm_title" -> "Вы действительно хотите перезапустить матч?"
                    "reset_confirm_desc" -> "Это перезапустит текущий матч и вернет все фишки на базу. Вы уверены, что хотите сбросить прогресс?"
                    "yes" -> "Да"
                    "no" -> "Нет"
                    "get_six" -> "Получить 6"
                    "six_active" -> "6 Активно"
                    "watching_ad" -> "Просмотр спонсорской рекламы 🎲"
                    "ad_guaranteed_six" -> "Просмотр короткой рекламы, чтобы получить гарантированную 6 при следующем броске!"
                    "ad_extend_time" -> "Просмотр короткой рекламы, чтобы продлить таймер матча 1 на 1 на +5 минут!"
                    "ad_game_finish" -> "Выход из игры после короткой спонсорской рекламы..."
                    "ad_reset" -> "Сброс матча после короткой спонсорской рекламы..."
                    "ad_watching" -> "Просмотр рекламы..."
                    "reward_claims" -> "Получение награды через %d сек..."
                    "settings_title" -> "Настройки"
                    "privacy_policy" -> "Политика конфиденциальности"
                    "privacy_policy_desc" -> "Мы ценим вашу конфиденциальность. Crown Ludo работает полностью в автономном режиме. Ваши игровые данные, настройки и статистика хранятся исключительно на вашем устройстве и никогда не передаются третьим лицам."
                    "contact_us" -> "Связаться с нами"
                    "contact_us_desc" -> "По вопросам поддержки, отзывов, запросов или деловых предложений, пожалуйста, свяжитесь с нами по адресу:"
                    "about" -> "О программе"
                    "about_desc" -> "Crown Ludo v1.2.0\nВысококачественная автономная игра в Лудо, созданная для плавного игрового процесса, красивой анимации и умных ИИ-соперников. Наслаждайтесь классическим режимом, матчем 1 на 1 и командной игрой 2 на 2 с великолепными динамическими темами.\n\nРазработано Камаром Патханом"
                    "copy_email" -> "Копировать почту"
                    "email_copied" -> "Почта скопирована в буфер обмена!"
                    else -> key
                }
            }
            else -> {
                when (key) {
                    "title" -> "Crown Ludo"
                    "players_count" -> "👥 PLAYERS COUNT"
                    "your_color" -> "🎨 YOUR COLOR"
                    "choose_theme" -> "🎭 CHOOSE THEME & GOTI"
                    "choose_language" -> "🌐 CHOOSE LANGUAGE"
                    "start_game" -> "START GAME"
                    "developed_by" -> "👑 Developed by Kamar Pathan"
                    "rules_title" -> "Ludo Game Rules"
                    "got_it" -> "GOT IT"
                    "rules_1" -> "Each player has 4 tokens in their base. Roll a 6 to release a token into the active track."
                    "rules_2" -> "Select any highlighted token of your color to move it clockwise."
                    "rules_3" -> "Rolling a 6 grants you an extra turn! Three 6s in a row cancels your current turn."
                    "rules_4" -> "Land on an opponent's token on normal paths to capture it and return it to their yard. You get a free extra roll!"
                    "rules_5" -> "Teammates (Red & Yellow, Green & Blue) in Team Up mode cannot capture each other and coexist safely."
                    "rules_6" -> "Enter your home color track and reach the exact center cell (57) to complete the token. Bring all 4 home to win!"
                    "classic_title" -> "Classic Ludo"
                    "classic_desc" -> "Traditional 4-player Ludo match"
                    "one_vs_one_title" -> "1 vs 1 Battle 🎯"
                    "one_vs_one_desc" -> "Fast-paced match against one opponent"
                    "computer_title" -> "Computer Game"
                    "computer_desc" -> "Play solo against 3 smart AI bots"
                    "team_up_title" -> "Team Up (2v2)"
                    "team_up_desc" -> "Red & Yellow team vs Green & Blue"
                    "hybrid_online_title" -> "Multiplayer Online 🌐"
                    "hybrid_online_desc" -> "Play Ludo with multiple players online"
                    "back_confirm_title" -> "क्या आप गेम छोड़ना चाहते हैं? / Do you want to leave the game?"
                    "back_confirm_desc" -> "यदि आप वापस जाएंगे, तो चालू गेम का दांव (Wager) जब्त हो जाएगा और वापस नहीं मिलेगा! क्या आप वाकई वापस जाना चाहते हैं?\n\n(If you go back, your current game progress and wagered coins will be forfeited! Do you really want to go back?)"
                    "back_confirm_desc_no_wager" -> "यदि आप वापस जाएंगे, तो चालू गेम का प्रोग्रेस समाप्त हो जाएगा! क्या आप वाकई बाहर निकलना चाहते हैं?\n\n(If you go back, your current game progress will be lost! Do you really want to leave?)"
                    "not_enough_coins" -> "⚠️ Not enough coins! You need %d 🪙 to play."
                    "select_mode_msg" -> "Select a Ludo Game Mode to play!"
                    "internet_required_title" -> "⚠️ Internet Connection Off!"
                    "internet_required_desc" -> "⚠️ Your internet is turned off! Please turn on your internet connection to play 1v1 match."
                    "reset_confirm_title" -> "क्या आप गेम रीसेट करना चाहते हैं? / Do you want to reset the match?"
                    "reset_confirm_desc" -> "इससे आपका चालू मैच रीस्टार्ट हो जाएगा और सारी गोटियाँ वापस घर में चली जाएँगी। क्या आप वाकई रीसेट करना चाहते हैं?\n\n(This will restart the current match and return all tokens to the yard. Do you really want to reset?)"
                    "yes" -> "हाँ / Yes"
                    "no" -> "नहीं / No"
                    "get_six" -> "Get 6"
                    "six_active" -> "6 Active"
                    "watching_ad" -> "Watching Sponsor Video Ad 🎲"
                    "ad_guaranteed_six" -> "Watching a short ad to claim a guaranteed 6 on your next roll!"
                    "ad_extend_time" -> "Watching a short ad to extend your 1v1 match timer by +5 minutes!"
                    "ad_game_finish" -> "Exiting gameplay after a short sponsor ad..."
                    "ad_reset" -> "Resetting match after a short sponsor ad..."
                    "ad_watching" -> "Watching ad..."
                    "reward_claims" -> "Reward claims in %ds..."
                    "settings_title" -> "Settings"
                    "privacy_policy" -> "Privacy Policy"
                    "privacy_policy_desc" -> "We value your privacy. Crown Ludo operates completely offline. Your gameplay, settings, and stats are stored entirely on your device and are never collected or shared with anyone."
                    "contact_us" -> "Contact Us"
                    "contact_us_desc" -> "For any support, feedback, queries or business inquiries, please contact us at:"
                    "about" -> "About"
                    "about_desc" -> "Crown Ludo v1.2.0\nA premium, high-performance offline Ludo game crafted for smooth gameplay, beautiful animations, and smart AI opponents. Enjoy Classic, 1v1, and Team Up 2v2 modes with gorgeous dynamic themes.\n\nDeveloped by Kamar Pathan"
                    "copy_email" -> "Copy Email"
                    "email_copied" -> "Email copied to clipboard!"
                    "profile_title" -> "Player Profile"
                    "edit_username" -> "Edit Username"
                    "coins_label" -> "Coins"
                    "daily_reward" -> "Daily Reward"
                    "daily_reward_desc" -> "Check in daily to claim your free reward!"
                    "claim_btn" -> "Claim +250 Coins"
                    "claimed_btn" -> "Claimed Today"
                    "watch_ad_btn" -> "Watch Ad for +500"
                    "watch_ad_desc" -> "Watch a short sponsor video to earn free gold coins!"
                    "enter_name_title" -> "Enter Username"
                    "enter_name_placeholder" -> "Enter your name..."
                    "save" -> "Save"
                    "cancel" -> "Cancel"
                    "ad_watch_ad" -> "Watching sponsor ad to claim +500 free coins!"
                    "shop_title" -> "Theme & Token Shop"
                    "buy_btn" -> "Buy for %d 🪙"
                    "use_btn" -> "Use Theme"
                    "unlocked" -> "Unlocked"
                    else -> key
                }
            }
        }
    }
}

enum class OnlineSubMode {
    CLASSIC,
    QUICK_PLAY
}

enum class AdType {
    GUARANTEED_SIX,
    EXTEND_TIME,
    GAME_FINISH,
    RESET,
    WATCH_AD,
    GAME_START
}

data class ChatMessage(
    val senderName: String,
    val senderColor: LudoColor?, // Null for system messages
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LudoState(
    val gamePhase: GamePhase = GamePhase.SPLASH,
    val isFindingOpponent: Boolean = false,
    val gameMode: LudoGameMode = LudoGameMode.CLASSIC,
    val players: List<Player> = emptyList(),
    val tokens: List<Token> = emptyList(),
    val currentPlayerIdx: Int = 0,
    val diceRoll: Int? = null,
    val hasRolled: Boolean = false,
    val isRolling: Boolean = false,
    val consecutiveSixes: Int = 0,
    val statusMessage: String = "Ludo Game Setup! Select players and tap Start.",
    val winnerPlayerId: Int? = null,
    val isSoundEnabled: Boolean = true,
    val isMicEnabled: Boolean = false,
    val isVoiceEnabled: Boolean = true,
    val micAmplitude: Float = 0f,
    val isSpeaking: Boolean = false,
    val timeLeftSeconds: Int = 300,
    val selectedTheme: LudoTheme = LudoTheme.CLASSIC,
    val selectedLanguage: LudoLanguage = LudoLanguage.EN,
    val adType: AdType? = null,
    val adSecondsLeft: Int = 0,
    val nextRollIsSix: Boolean = false,
    val isTimeUpDialogShowing: Boolean = false,
    val isTimeWarningDialogShowing: Boolean = false,
    val isRealAdShowing: Boolean = false,
    val isMovingToken: Boolean = false,
    val movingTokenId: Int? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val activePlayerBubbles: Map<Int, String> = emptyMap(), // playerId to message text
    val botTypingName: String? = null, // Name of the bot currently typing
    val baseColors: List<LudoColor> = listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE),
    val username: String = "User_ID_" + (100000..999999).random(),
    val coins: Int = 1000,
    val isDailyRewardAvailable: Boolean = false,
    val lastCheckInTime: Long = 0L,
    val unlockedThemes: Set<LudoTheme> = setOf(LudoTheme.CLASSIC),
    val selectedTokenStyle: LudoTokenStyle = LudoTokenStyle.CLASSIC_PAWN,
    val selectedDiceStyle: LudoDiceStyle = LudoDiceStyle.CLASSIC_DOTS,
    val unlockedTokenStyles: Set<LudoTokenStyle> = setOf(LudoTokenStyle.CLASSIC_PIN, LudoTokenStyle.GLOSSY_3D, LudoTokenStyle.CLASSIC_PAWN),
    val unlockedDiceStyles: Set<LudoDiceStyle> = setOf(LudoDiceStyle.CLASSIC_DOTS),
    val selectedWagerAmount: Int = 500,
    val onlinePlayerPings: Map<Int, Int> = emptyMap(),
    val disconnectedPlayers: Set<Int> = emptySet(),
    val onlineSubMode: OnlineSubMode = OnlineSubMode.CLASSIC,
    val coinRedeemAmount: Int = 0,
    val watchAdCount: Int = 0,
    val watchAdCooldownRemaining: Long = 0L,
    val guaranteedSixCount: Int = 0,
    val guaranteedSixCooldownRemaining: Long = 0L,
    val selectedMusicMode: String = "GULF",
    val friendsList: List<String> = emptyList(),
    val addedFriends: Set<Int> = emptySet(),
    val winsComputer: Int = 0,
    val winsOneVsOne: Int = 0,
    val winsOnline: Int = 0,
    val gameplaySpeed: String = "NORMAL",
    val bio: String = "Ludo Champion! 🎲",
    val selectedAvatarId: Int = 0,
    val liveOnlineUsersCount: Int = 1
)

class LudoViewModel : ViewModel() {

    private val realisticPlayerNames = listOf(
        "Rahul_Sharma", "Priya_Ludo99", "Aman_Verma", "Rohan_Pro", "Neha_Singh", "Vikas_Gamer", "Arjun_King", "Aniket_007", "Simran_G", "Pooja_Rani",
        "Aditya_Roy", "Karan_Ludo", "Deepak_S", "Saurabh_786", "Manish_Kumar", "Sanjay_Rathore", "Pankaj_01", "Mohit_Yadav", "Amit_Joshi", "Sumit_G",
        "Aarti_Sharma", "Kavya_Patel", "Sneha_Roy", "Divya_Singh", "Shreya_M", "Megha_P", "Ananya_007", "Ritu_Sharma", "Nisha_K", "Pooja_Verma",
        "Aakash_Pro", "Abhishek_King", "Alok_Gamer", "Anshul_R", "Anurag_Ludo", "Ashish_Singh", "Ayush_Sharma", "Bhavesh_Patel", "Chetan_Kumar", "Dev_Rathore",
        "Gaurav_Sharma", "Harsh_Gupta", "Hemant_Verma", "Jatin_Gamer", "Jitendra_P", "Kapil_Sharma", "Kunal_Roy", "Lokesh_007", "Mayank_King", "Naveen_Kumar",
        "Nitin_Sharma", "Piyush_Pro", "Pradeep_Yadav", "Praveen_Gamer", "Rahul_Verma", "Raj_Rathore", "Rajesh_Kumar", "Rakesh_Sharma", "Ramesh_Singh", "Ravi_K",
        "Rishabh_007", "Rohit_Sharma", "Sachin_Gamer", "Sahil_King", "Sandeep_Verma", "Sanjay_Kumar", "Saurav_Patel", "Shivam_Rathore", "Shubham_G", "Sonu_007",
        "Subhash_Singh", "Sunil_Kumar", "Suresh_Sharma", "Tarun_Gamer", "Tushar_Pro", "Varun_Rathore", "Vicky_Sharma", "Vijay_Kumar", "Vikram_Singh", "Vinay_007",
        "Vineet_Pro", "Vishal_Sharma", "Yash_Gamer", "Yogesh_Verma", "Aakriti_Singh", "Aastha_Sharma", "Aditi_Roy", "Akanksha_G", "Alka_Verma", "Anjali_007",
        "Ankita_Singh", "Anshika_Sharma", "Anupama_Roy", "Archana_Gamer", "Bhavna_Patel", "Deepika_Sharma", "Garima_Singh", "Kajal_007", "Kirti_Pro", "Komal_Sharma",
        "Mamta_Verma", "Manisha_Singh", "Monika_Gamer", "Nandini_Roy", "Neelam_Singh", "Neha_Verma", "Payal_Sharma", "Pragati_G", "Prerna_007", "Priyanka_Singh",
        "Radhika_Sharma", "Rakhi_Gamer", "Renu_Verma", "Riddhi_Roy", "Riya_Singh", "Roshni_Sharma", "Sakshi_Pro", "Sangeeta_Gamer", "Sapna_007", "Seema_Sharma",
        "Shalini_Singh", "Sheetal_Verma", "Shivani_Roy", "Shweta_Sharma", "Sneha_Gamer", "Sona_007", "Sonia_Singh", "Sunita_Verma", "Swati_Sharma", "Tanu_Gamer",
        "Tanya_Singh", "Urvashi_Roy", "Vaishnavi_Sharma", "Vandana_Gamer", "Varsha_007", "Vidya_Singh", "Yashika_Sharma", "Abhi_Gamer_99", "Aman_Bhai_07", "Amit_Ludo_Pro",
        "Ankit_Boss_007", "Arjun_Sniper", "Badshah_Ludo", "Bunny_Rider", "Chetan_Pro_99", "Deepak_King_01", "Dinesh_Rathore", "Gautam_Gamer", "Golu_Ludo_King", "Honey_Singh_Fan",
        "Ishan_Sharma", "Jassi_Gill_Fan", "Kabir_Ludo_Master", "Karan_Aujla_Fan", "Lucky_Boy_007", "Manish_Pro_Player", "Monu_Ludo_King", "Nikhil_Pro_Gamer", "Paras_Sharma", "Prince_Rathore",
        "Rajan_Verma", "Rakesh_Ludo_Master", "Robby_Gamer", "Rocky_Bhai_007", "Rohan_Pro_Player", "Sahil_Khan_007", "Sameer_Khan_99", "Shivam_Ludo_King", "Sonu_Monu_Ludo", "Sunny_Paji_007",
        "Suraj_Kumar_Pro", "Tanmay_Sharma", "Vicky_Pro_Player", "Vikram_Rathore", "Vipin_Kumar", "Yash_Ludo_King", "Yuvraj_Singh_Fan", "Zaid_Khan_007", "Aman_Sheikh_99", "Aarti_Ludo_Queen",
        "Abhay_Singh_007", "Akash_Gamer_Pro", "Alok_Kumar_99", "Aman_King_Ludo", "Anuj_Sharma_007", "Anwar_Khan_Pro", "Arbaaz_Khan_007", "Arpit_Verma_99", "Aryan_Gamer_Pro", "Ashok_Kumar_007",
        "Avinash_Singh", "Ayush_Pro_Gamer", "Balu_Ludo_King", "Bharat_Rathore", "Brijesh_Kumar", "Chandan_Gamer", "Chirag_Sharma", "Deepak_Pro_007", "Devansh_Singh", "Dharmendra_K",
        "Dilip_Sharma", "Firoz_Khan_007", "Ganesh_Pro_Ludo", "Gaurav_Gamer_99", "Gopal_Verma", "Gulshan_Kumar", "Harish_Sharma", "Hemant_Gamer", "Himanshu_Singh", "Imran_Khan_007",
        "Inder_Singh_Pro", "Jagdish_Kumar", "Jaideep_Sharma", "Javed_Khan_99", "Jayesh_Patel", "Jitendra_Gamer", "Jyoti_Sharma_007", "Karan_Pro_Ludo", "Kartik_Singh_99", "Kavita_Rani_Pro",
        "Kishore_Kumar", "Kuldeep_Singh", "Laxman_Pro_Gamer", "Lokesh_Sharma", "Madhav_Singh", "Mahesh_Kumar_007", "Manjit_Singh_Pro", "Manoj_Verma_99", "Mohd_Ali_007", "Mukesh_Kumar"
    )

    private val _uiState = MutableStateFlow(LudoState())
    val uiState: StateFlow<LudoState> = _uiState.asStateFlow()

    // Real Firebase Database connectivity
    private var firebaseDb: FirebaseDatabase? = null
    var isFirebaseAvailable: Boolean = false
        private set
    var activeFirebaseMatchId: String? = null
        private set
    var myFirebasePlayerSlot: Int = 3 // default index 3 (Human slot)
        private set
    private var firebaseMatchListener: ValueEventListener? = null
    private var isUpdatingFromFirebase: Boolean = false

    fun dismissCoinRedeemAnimation() {
        _uiState.update { it.copy(coinRedeemAmount = 0) }
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    private var sharedPrefs: android.content.SharedPreferences? = null
    private var appContext: android.content.Context? = null

    private fun isInternetAvailable(): Boolean {
        val context = appContext ?: return true
        return try {
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    fun initPrefs(context: android.content.Context) {
        appContext = context.applicationContext
        if (sharedPrefs != null) return
        val prefs = context.getSharedPreferences("ludo_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs = prefs
        
        var username = prefs.getString("username", "") ?: ""
        if (username.isEmpty()) {
            username = "User_ID_" + (100000..999999).random()
            prefs.edit().putString("username", username).commit()
        }
        val coins = prefs.getInt("coins", 1000)
        val lastCheckInTime = prefs.getLong("last_check_in_time", 0L)
        
        var watchAdCount = prefs.getInt("watch_ad_count", 0)
        var guaranteedSixCount = prefs.getInt("guaranteed_six_count", 0)
        val watchAdCooldownEndTime = prefs.getLong("watch_ad_cooldown_end_time", 0L)
        val guaranteedSixCooldownEndTime = prefs.getLong("guaranteed_six_cooldown_end_time", 0L)
        
        val currentTime = System.currentTimeMillis()
        val watchAdCooldownRemaining = if (currentTime < watchAdCooldownEndTime) {
            (watchAdCooldownEndTime - currentTime) / 1000
        } else {
            if (watchAdCount >= 2) {
                watchAdCount = 0
                prefs.edit().putInt("watch_ad_count", 0).apply()
            }
            0L
        }
        val guaranteedSixCooldownRemaining = if (currentTime < guaranteedSixCooldownEndTime) {
            (guaranteedSixCooldownEndTime - currentTime) / 1000
        } else {
            if (guaranteedSixCount >= 2) {
                guaranteedSixCount = 0
                prefs.edit().putInt("guaranteed_six_count", 0).apply()
            }
            0L
        }
        
        val isDailyAvailable = checkDailyRewardAvailability(lastCheckInTime)
        
        val unlockedThemesStr = prefs.getStringSet("unlocked_themes", setOf("CLASSIC")) ?: setOf("CLASSIC")
        val unlockedThemesSet = unlockedThemesStr.mapNotNull {
            try { LudoTheme.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
        val currentThemeStr = prefs.getString("selected_theme", "CLASSIC") ?: "CLASSIC"
        val currentTheme = try { LudoTheme.valueOf(currentThemeStr) } catch(e: Exception) { LudoTheme.CLASSIC }

        val unlockedTokensStr = prefs.getStringSet("unlocked_tokens", setOf("CLASSIC_PIN", "GLOSSY_3D", "CLASSIC_PAWN")) ?: setOf("CLASSIC_PIN", "GLOSSY_3D", "CLASSIC_PAWN")
        val unlockedTokensSet = unlockedTokensStr.mapNotNull {
            try { LudoTokenStyle.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
        val currentTokenStr = prefs.getString("selected_token_style", "CLASSIC_PIN") ?: "CLASSIC_PIN"
        val currentToken = try { LudoTokenStyle.valueOf(currentTokenStr) } catch(e: Exception) { LudoTokenStyle.CLASSIC_PIN }

        val unlockedDiceStr = prefs.getStringSet("unlocked_dice", setOf("CLASSIC_DOTS")) ?: setOf("CLASSIC_DOTS")
        val unlockedDiceSet = unlockedDiceStr.mapNotNull {
            try { LudoDiceStyle.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
        val currentDiceStr = prefs.getString("selected_dice_style", "CLASSIC_DOTS") ?: "CLASSIC_DOTS"
        val currentDice = try { LudoDiceStyle.valueOf(currentDiceStr) } catch(e: Exception) { LudoDiceStyle.CLASSIC_DOTS }
        
        val musicMode = prefs.getString("selected_music_mode", "GULF") ?: "GULF"
        LudoAudioEngine.currentMusicMode = musicMode
        
        val isMicEnabled = prefs.getBoolean("is_mic_enabled", false)
        val isVoiceEnabled = prefs.getBoolean("is_voice_enabled", true)

        val savedFriends = prefs.getStringSet("friends_list", emptySet()) ?: emptySet()

        val winsComputer = prefs.getInt("wins_computer", 0)
        val winsOneVsOne = prefs.getInt("wins_one_vs_one", 0)
        val winsOnline = prefs.getInt("wins_online", 0)
        val gameplaySpeed = prefs.getString("gameplay_speed", "NORMAL") ?: "NORMAL"
        val bio = prefs.getString("bio", "Ludo Champion! 🎲") ?: "Ludo Champion! 🎲"
        val selectedAvatarId = prefs.getInt("selected_avatar_id", 0)

        _uiState.update { currentState ->
            currentState.copy(
                username = username,
                coins = coins,
                isDailyRewardAvailable = isDailyAvailable,
                lastCheckInTime = lastCheckInTime,
                unlockedThemes = unlockedThemesSet.ifEmpty { setOf(LudoTheme.CLASSIC) },
                selectedTheme = currentTheme,
                unlockedTokenStyles = unlockedTokensSet.ifEmpty { setOf(LudoTokenStyle.CLASSIC_PIN, LudoTokenStyle.GLOSSY_3D, LudoTokenStyle.CLASSIC_PAWN) },
                selectedTokenStyle = currentToken,
                unlockedDiceStyles = unlockedDiceSet.ifEmpty { setOf(LudoDiceStyle.CLASSIC_DOTS) },
                selectedDiceStyle = currentDice,
                watchAdCount = watchAdCount,
                watchAdCooldownRemaining = watchAdCooldownRemaining,
                guaranteedSixCount = guaranteedSixCount,
                guaranteedSixCooldownRemaining = guaranteedSixCooldownRemaining,
                selectedMusicMode = musicMode,
                isMicEnabled = isMicEnabled,
                isVoiceEnabled = isVoiceEnabled,
                friendsList = savedFriends.toList(),
                winsComputer = winsComputer,
                winsOneVsOne = winsOneVsOne,
                winsOnline = winsOnline,
                gameplaySpeed = gameplaySpeed,
                bio = bio,
                selectedAvatarId = selectedAvatarId
            )
        }

        // Collect real-time microphone amplitude & speaking status
        viewModelScope.launch {
            RealtimeVoiceManager.micAmplitude.collect { amp ->
                _uiState.update { it.copy(micAmplitude = amp) }
            }
        }
        viewModelScope.launch {
            RealtimeVoiceManager.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }

        // Initialize Firebase safely
        try {
            var app: com.google.firebase.FirebaseApp? = try {
                com.google.firebase.FirebaseApp.getInstance()
            } catch (e: Exception) {
                null
            }

            if (app == null) {
                app = try {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    null
                }
            }

            // Fallback to programmatic Firebase Options if BuildConfig or default fallback options are needed
            if (app == null) {
                val dbUrl = try { com.example.BuildConfig.FIREBASE_DATABASE_URL } catch (e: Exception) { "" }
                val apiKey = try { com.example.BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
                val projectId = try { com.example.BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }
                val appId = try { com.example.BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Exception) { "" }

                val finalDbUrl = if (dbUrl.isNotBlank() && !dbUrl.contains("your_firebase_database_url")) dbUrl else "https://ludo-star-realtime-default-rtdb.firebaseio.com"
                val finalApiKey = if (apiKey.isNotBlank() && !apiKey.contains("your_firebase_api_key")) apiKey else "AIzaSyA_DefaultLudoPublicApiKey123456"
                val finalProjectId = if (projectId.isNotBlank() && !projectId.contains("your_firebase_project_id")) projectId else "ludo-star-realtime"
                val finalAppId = if (appId.isNotBlank() && !appId.contains("your_firebase_application_id")) appId else "1:1077812810754:android:defaultludo"

                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setDatabaseUrl(finalDbUrl)
                    .setApiKey(finalApiKey)
                    .setProjectId(finalProjectId)
                    .setApplicationId(finalAppId)
                    .build()
                app = try {
                    com.google.firebase.FirebaseApp.initializeApp(context, options)
                } catch (e: Exception) {
                    null
                }
            }

            if (app != null) {
                val dbUrl = try { com.example.BuildConfig.FIREBASE_DATABASE_URL } catch (e: Exception) { "" }
                val finalDbUrl = if (dbUrl.isNotBlank() && !dbUrl.contains("your_firebase_database_url")) dbUrl else "https://ludo-star-realtime-default-rtdb.firebaseio.com"
                
                firebaseDb = try {
                    FirebaseDatabase.getInstance(finalDbUrl)
                } catch (e: Exception) {
                    try { FirebaseDatabase.getInstance() } catch (e2: Exception) { null }
                }
                isFirebaseAvailable = (firebaseDb != null)
                if (isFirebaseAvailable) {
                    setupRealtimePresenceTracking()
                }
            } else {
                firebaseDb = null
                isFirebaseAvailable = false
            }
        } catch (e: Exception) {
            firebaseDb = null
            isFirebaseAvailable = false
        }
    }

    private fun setupRealtimePresenceTracking() {
        val db = firebaseDb ?: return
        val usernameKey = _uiState.value.username.ifBlank { "User_${System.currentTimeMillis()}" }
        val myPresenceRef = db.getReference("online_presence").child(usernameKey)
        myPresenceRef.setValue(true)
        myPresenceRef.onDisconnect().removeValue()

        val presenceListRef = db.getReference("online_presence")
        presenceListRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val realCount = snapshot.childrenCount.toInt().coerceAtLeast(1)
                _uiState.update { it.copy(liveOnlineUsersCount = realCount) }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun checkDailyRewardAvailability(lastCheckInTime: Long): Boolean {
        if (lastCheckInTime == 0L) return true
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastCheckInTime
        return diff >= 86400000L
    }

    fun getThemeCost(theme: LudoTheme): Int {
        return when (theme) {
            LudoTheme.CLASSIC -> 0
            LudoTheme.COSMIC -> 3000
            LudoTheme.ROYAL -> 10000
            LudoTheme.FOREST -> 25000
            LudoTheme.CANDY -> 50000
            LudoTheme.OCEAN -> 70000
            LudoTheme.CYBERPUNK -> 85000
            LudoTheme.EGYPT -> 100000
        }
    }

    fun unlockTheme(theme: LudoTheme): Boolean {
        val currentState = _uiState.value
        val cost = getThemeCost(theme)
        if (currentState.coins >= cost && !currentState.unlockedThemes.contains(theme)) {
            val newCoins = currentState.coins - cost
            val newUnlocked = currentState.unlockedThemes + theme
            _uiState.update { it.copy(coins = newCoins, unlockedThemes = newUnlocked, selectedTheme = theme) }
            sharedPrefs?.edit()?.apply {
                putInt("coins", newCoins)
                putStringSet("unlocked_themes", newUnlocked.map { it.name }.toSet())
                putString("selected_theme", theme.name)
            }?.apply()
            return true
        }
        return false
    }

    fun selectTokenStyle(style: LudoTokenStyle) {
        val currentState = _uiState.value
        if (currentState.unlockedTokenStyles.contains(style)) {
            _uiState.update { it.copy(selectedTokenStyle = style) }
            sharedPrefs?.edit()?.putString("selected_token_style", style.name)?.apply()
        }
    }

    fun unlockTokenStyle(style: LudoTokenStyle): Boolean {
        val currentState = _uiState.value
        val cost = style.cost
        if (currentState.coins >= cost && !currentState.unlockedTokenStyles.contains(style)) {
            val newCoins = currentState.coins - cost
            val newUnlocked = currentState.unlockedTokenStyles + style
            _uiState.update { it.copy(coins = newCoins, unlockedTokenStyles = newUnlocked, selectedTokenStyle = style) }
            sharedPrefs?.edit()?.apply {
                putInt("coins", newCoins)
                putStringSet("unlocked_tokens", newUnlocked.map { it.name }.toSet())
                putString("selected_token_style", style.name)
            }?.apply()
            return true
        }
        return false
    }

    fun selectDiceStyle(style: LudoDiceStyle) {
        val currentState = _uiState.value
        if (currentState.unlockedDiceStyles.contains(style)) {
            _uiState.update { it.copy(selectedDiceStyle = style) }
            sharedPrefs?.edit()?.putString("selected_dice_style", style.name)?.apply()
        }
    }

    fun unlockDiceStyle(style: LudoDiceStyle): Boolean {
        val currentState = _uiState.value
        val cost = style.cost
        if (currentState.coins >= cost && !currentState.unlockedDiceStyles.contains(style)) {
            val newCoins = currentState.coins - cost
            val newUnlocked = currentState.unlockedDiceStyles + style
            _uiState.update { it.copy(coins = newCoins, unlockedDiceStyles = newUnlocked, selectedDiceStyle = style) }
            sharedPrefs?.edit()?.apply {
                putInt("coins", newCoins)
                putStringSet("unlocked_dice", newUnlocked.map { it.name }.toSet())
                putString("selected_dice_style", style.name)
            }?.apply()
            return true
        }
        return false
    }

    fun updateUsername(newUsername: String) {
        val cleanName = newUsername.trim()
        if (cleanName.isNotEmpty()) {
            _uiState.update { currentState ->
                val updatedPlayers = currentState.players.map { player ->
                    if (player.type == PlayerType.HUMAN || player.id == myFirebasePlayerSlot) {
                        player.copy(name = cleanName)
                    } else {
                        player
                    }
                }
                currentState.copy(username = cleanName, players = updatedPlayers)
            }
            sharedPrefs?.edit()?.putString("username", cleanName)?.commit()

            val matchId = activeFirebaseMatchId
            val slot = myFirebasePlayerSlot
            if (matchId != null && slot >= 0) {
                try {
                    firebaseDb?.getReference("matches")?.child(matchId)?.child("players")?.child(slot.toString())?.child("name")?.setValue(cleanName)
                } catch (e: Exception) {}
            }
        }
    }

    fun updateBio(newBio: String) {
        val cleanBio = newBio.trim()
        _uiState.update { it.copy(bio = cleanBio) }
        sharedPrefs?.edit()?.putString("bio", cleanBio)?.commit()
    }

    fun updateSelectedAvatarId(avatarId: Int) {
        _uiState.update { currentState ->
            val updatedPlayers = currentState.players.map { player ->
                if (player.type == PlayerType.HUMAN || player.id == myFirebasePlayerSlot) {
                    player.copy(avatarId = avatarId)
                } else {
                    player
                }
            }
            currentState.copy(selectedAvatarId = avatarId, players = updatedPlayers)
        }
        sharedPrefs?.edit()?.putInt("selected_avatar_id", avatarId)?.commit()

        val matchId = activeFirebaseMatchId
        val slot = myFirebasePlayerSlot
        if (matchId != null && slot >= 0) {
            try {
                firebaseDb?.getReference("matches")?.child(matchId)?.child("players")?.child(slot.toString())?.child("avatarId")?.setValue(avatarId)
            } catch (e: Exception) {}
        }
    }

    fun addCoins(amount: Int) {
        _uiState.update { currentState ->
            val nextCoins = (currentState.coins + amount).coerceAtLeast(0)
            sharedPrefs?.edit()?.putInt("coins", nextCoins)?.commit()
            currentState.copy(
                coins = nextCoins,
                coinRedeemAmount = amount
            )
        }
    }

    fun claimDailyReward(): Boolean {
        val currentState = _uiState.value
        if (currentState.isDailyRewardAvailable) {
            val currentTime = System.currentTimeMillis()
            _uiState.update { state ->
                val nextCoins = state.coins + 250
                sharedPrefs?.edit()?.apply {
                    putInt("coins", nextCoins)
                    putLong("last_check_in_time", currentTime)
                }?.apply()
                state.copy(
                    coins = nextCoins,
                    isDailyRewardAvailable = false,
                    lastCheckInTime = currentTime,
                    statusMessage = "🎉 Daily check-in successful! +250 Coins added!",
                    coinRedeemAmount = 250
                )
            }
            return true
        }
        return false
    }

    // Setup configuration
    val selectedPlayerCount = MutableStateFlow(2)
    val selectedUserColor = MutableStateFlow(LudoColor.BLUE)
    val selectedOnlineSubMode = MutableStateFlow(OnlineSubMode.CLASSIC)

    fun selectOnlineSubMode(subMode: OnlineSubMode) {
        selectedOnlineSubMode.value = subMode
        _uiState.update { it.copy(onlineSubMode = subMode) }
    }

    // Temporary values for compatibility
    val setupNames = MutableStateFlow(listOf("Player 1", "AI Bot 1", "AI Bot 2", "AI Bot 3"))
    val setupTypes = MutableStateFlow(listOf(PlayerType.HUMAN, PlayerType.BOT, PlayerType.BOT, PlayerType.BOT))

    init {
        // Start background music automatically
        LudoAudioEngine.startBgm()

        // Initialize default setup values
        viewModelScope.launch {
            delay(2000)
            _uiState.update { currentState ->
                if (currentState.gamePhase == GamePhase.SPLASH) {
                    currentState.copy(gamePhase = GamePhase.MODE_SELECT)
                } else {
                    currentState
                }
            }
        }

        // Cooldown ticker loop for Ad Watch and Guaranteed Six
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    val nextAdRemaining = if (state.watchAdCooldownRemaining > 1) {
                        state.watchAdCooldownRemaining - 1
                    } else if (state.watchAdCooldownRemaining == 1L) {
                        // Cooldown finished! Reset watch count to 0 in SharedPreferences
                        sharedPrefs?.edit()?.apply {
                            putInt("watch_ad_count", 0)
                            putLong("watch_ad_cooldown_end_time", 0L)
                            apply()
                        }
                        0L
                    } else {
                        0L
                    }

                    val nextSixRemaining = if (state.guaranteedSixCooldownRemaining > 1) {
                        state.guaranteedSixCooldownRemaining - 1
                    } else if (state.guaranteedSixCooldownRemaining == 1L) {
                        // Cooldown finished!
                        sharedPrefs?.edit()?.apply {
                            putInt("guaranteed_six_count", 0)
                            putLong("guaranteed_six_cooldown_end_time", 0L)
                            apply()
                        }
                        0L
                    } else {
                        0L
                    }

                    val nextAdCount = if (nextAdRemaining == 0L && state.watchAdCooldownRemaining > 0) {
                        0
                    } else {
                        state.watchAdCount
                    }

                    val nextSixCount = if (nextSixRemaining == 0L && state.guaranteedSixCooldownRemaining > 0) {
                        0
                    } else {
                        state.guaranteedSixCount
                    }

                    state.copy(
                        watchAdCooldownRemaining = nextAdRemaining,
                        watchAdCount = nextAdCount,
                        guaranteedSixCooldownRemaining = nextSixRemaining,
                        guaranteedSixCount = nextSixCount
                    )
                }
            }
        }
    }

    fun toggleSound() {
        _uiState.update { 
            val nextEnabled = !it.isSoundEnabled
            LudoAudioEngine.isSoundEnabled = nextEnabled
            LudoAudioEngine.isMusicEnabled = nextEnabled
            it.copy(isSoundEnabled = nextEnabled)
        }
    }

    fun enableMic(context: android.content.Context) {
        RealtimeVoiceManager.startMicRecording(context)
        sharedPrefs?.edit()?.putBoolean("is_mic_enabled", true)?.apply()
        _uiState.update { 
            it.copy(
                isMicEnabled = true,
                statusMessage = "🎙️ Real-time Microphone ACTIVE! Speak into your phone."
            )
        }
    }

    fun disableMic() {
        RealtimeVoiceManager.stopMicRecording()
        sharedPrefs?.edit()?.putBoolean("is_mic_enabled", false)?.apply()
        _uiState.update { 
            it.copy(
                isMicEnabled = false,
                micAmplitude = 0f,
                isSpeaking = false,
                statusMessage = "🎙️ Microphone Muted."
            )
        }
    }

    fun toggleMic(context: android.content.Context? = null) {
        val currentMic = _uiState.value.isMicEnabled
        if (!currentMic) {
            if (context != null) {
                enableMic(context)
            } else if (appContext != null) {
                enableMic(appContext!!)
            } else {
                _uiState.update { it.copy(isMicEnabled = true) }
            }
        } else {
            disableMic()
        }
    }

    fun toggleVoice() {
        _uiState.update { 
            val nextVoice = !it.isVoiceEnabled
            sharedPrefs?.edit()?.putBoolean("is_voice_enabled", nextVoice)?.apply()
            if (nextVoice) {
                RealtimeVoiceManager.speakAnnouncer("Voice speaker active", true)
                it.copy(
                    isVoiceEnabled = true,
                    statusMessage = "🔊 Real-time Speaker Voice Chat ACTIVE!"
                )
            } else {
                RealtimeVoiceManager.stopAnnouncer()
                it.copy(
                    isVoiceEnabled = false,
                    statusMessage = "🔊 Voice Speaker Muted."
                )
            }
        }
    }

    fun addFriend(name: String, playerId: Int) {
        val currentFriends = sharedPrefs?.getStringSet("friends_list", emptySet()) ?: emptySet()
        val cleanedName = name.replace("🔴", "").replace("🟢", "").replace("🟡", "").replace("🔵", "").trim()
        val newFriends = currentFriends + cleanedName
        sharedPrefs?.edit()?.putStringSet("friends_list", newFriends)?.apply()
        
        _uiState.update { state ->
            state.copy(
                friendsList = newFriends.toList(),
                addedFriends = state.addedFriends + playerId
            )
        }
    }

    fun setGameplaySpeed(speed: String) {
        _uiState.update { it.copy(gameplaySpeed = speed) }
        sharedPrefs?.edit()?.putString("gameplay_speed", speed)?.apply()
    }

    fun getGameplaySpeedMultiplier(): Double {
        return when (_uiState.value.gameplaySpeed) {
            "SLOW" -> 1.2
            "FAST" -> 0.3
            "TURBO" -> 0.12
            else -> 0.6 // Make NORMAL much faster and more responsive as well!
        }
    }

    fun recordUserWin(mode: LudoGameMode) {
        _uiState.update { currentState ->
            val nextComputer = if (mode == LudoGameMode.VS_COMPUTER) currentState.winsComputer + 1 else currentState.winsComputer
            val next1v1 = if (mode == LudoGameMode.ONE_VS_ONE) currentState.winsOneVsOne + 1 else currentState.winsOneVsOne
            val nextOnline = if (mode == LudoGameMode.HYBRID_ONLINE) currentState.winsOnline + 1 else currentState.winsOnline
            
            sharedPrefs?.edit()?.apply {
                putInt("wins_computer", nextComputer)
                putInt("wins_one_vs_one", next1v1)
                putInt("wins_online", nextOnline)
                apply()
            }
            
            currentState.copy(
                winsComputer = nextComputer,
                winsOneVsOne = next1v1,
                winsOnline = nextOnline
            )
        }
    }

    fun toggleMusicMode() {
        _uiState.update { state ->
            val nextMode = if (state.selectedMusicMode == "GULF") "CLASSIC" else "GULF"
            sharedPrefs?.edit()?.putString("selected_music_mode", nextMode)?.apply()
            LudoAudioEngine.currentMusicMode = nextMode
            state.copy(selectedMusicMode = nextMode)
        }
    }

    fun updateSetupName(index: Int, name: String) {
        val current = setupNames.value.toMutableList()
        current[index] = name
        setupNames.value = current
    }

    fun updateSetupType(index: Int, type: PlayerType) {
        val current = setupTypes.value.toMutableList()
        current[index] = type
        setupTypes.value = current
    }

    fun selectGameMode(mode: LudoGameMode) {
        _uiState.update { it.copy(gameMode = mode) }
        when (mode) {
            LudoGameMode.CLASSIC -> {
                _uiState.update { 
                    it.copy(
                        gamePhase = GamePhase.SETUP,
                        statusMessage = "Select player count and color!"
                    )
                }
            }
            LudoGameMode.ONE_VS_ONE -> {
                selectedPlayerCount.value = 2
                selectedUserColor.value = LudoColor.BLUE
                _uiState.update { 
                    it.copy(
                        gamePhase = GamePhase.SETUP,
                        statusMessage = "1v1 Match setup! Select your color."
                    )
                }
            }
            LudoGameMode.VS_COMPUTER -> {
                selectedUserColor.value = LudoColor.BLUE
                _uiState.update { 
                    it.copy(
                        gamePhase = GamePhase.SETUP,
                        statusMessage = "VS Computer setup! Select player count and color."
                    )
                }
            }
            LudoGameMode.TEAM_UP -> {
                selectedPlayerCount.value = 4
                _uiState.update { 
                    it.copy(
                        gamePhase = GamePhase.SETUP,
                        statusMessage = "Select your team color (Red/Yellow or Green/Blue)!"
                    )
                }
            }
            LudoGameMode.HYBRID_ONLINE -> {
                // Keep default 4 players and blue color as a starting choice, but allow the user to change them
                _uiState.update { 
                    it.copy(
                        gamePhase = GamePhase.SETUP,
                        statusMessage = "Online Match Setup! Select mode, players and color."
                    )
                }
            }
        }
    }

    fun selectWagerAmount(amount: Int) {
        val lang = uiState.value.selectedLanguage
        if (uiState.value.coins < amount) {
            val msg = LudoTranslations.getTranslation("not_enough_coins", lang)
                .replace("%d", amount.toString())
            _uiState.update { 
                it.copy(
                    selectedWagerAmount = amount,
                    statusMessage = msg
                )
            }
        } else {
            _uiState.update { 
                it.copy(
                    selectedWagerAmount = amount,
                    statusMessage = ""
                )
            }
        }
    }

    fun startGame() {
        val mode = uiState.value.gameMode
        val lang = uiState.value.selectedLanguage
        val isWagerMode = mode == LudoGameMode.ONE_VS_ONE || mode == LudoGameMode.HYBRID_ONLINE
        val wager = uiState.value.selectedWagerAmount
        if (isWagerMode) {
            if (uiState.value.coins < wager) {
                val msg = LudoTranslations.getTranslation("not_enough_coins", lang)
                    .replace("%d", wager.toString())
                _uiState.update { it.copy(statusMessage = msg) }
                return
            }
        }

        // Trigger Ad on Game Start!
        triggerAd(AdType.GAME_START)
    }

    fun proceedWithStartGame() {
        val mode = uiState.value.gameMode
        val wager = uiState.value.selectedWagerAmount
        val isWagerMode = mode == LudoGameMode.ONE_VS_ONE || mode == LudoGameMode.HYBRID_ONLINE
        if (isWagerMode) {
            // Deduct wager
            val nextCoins = uiState.value.coins - wager
            _uiState.update { it.copy(coins = nextCoins) }
            sharedPrefs?.edit()?.putInt("coins", nextCoins)?.commit()
        }

        if (mode == LudoGameMode.HYBRID_ONLINE) {
            searchOrCreateFirebaseMatch(wager)
            return
        }

        val userColor = selectedUserColor.value
        val userIdx = 3 // Always index 3 (bottom-left corner) for Human!
        val playerCount = selectedPlayerCount.value

        val players = mutableListOf<Player>()
        val tokens = mutableListOf<Token>()

        // Dynamically assign unique colors to the other indices so there are no duplicates.
        val availableColors = LudoColor.values().filter { it != userColor }.toMutableList()
        val indexToColor = mutableMapOf<Int, LudoColor>()
        indexToColor[3] = userColor
        indexToColor[0] = availableColors.removeAt(0)
        indexToColor[1] = availableColors.removeAt(0)
        indexToColor[2] = availableColors.removeAt(0)

        // 1. Add User (HUMAN)
        val userLevel = 1 + (uiState.value.winsComputer + uiState.value.winsOneVsOne + uiState.value.winsOnline) / 3
        players.add(
            Player(
                id = userIdx,
                name = uiState.value.username,
                color = userColor,
                type = PlayerType.HUMAN,
                level = userLevel,
                avatarId = uiState.value.selectedAvatarId
            )
        )

        // 2. Add BOTs / other players depending on count
        val otherIndices = when (playerCount) {
            2 -> listOf((userIdx + 2) % 4) // Opposite corner (index 1 / top-right) for 1v1
            3 -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4) // Next two clockwise
            else -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4, (userIdx + 3) % 4) // All other 3
        }

        val shuffledNames = realisticPlayerNames.shuffled().toMutableList()
        for ((idx, otherIdx) in otherIndices.withIndex()) {
            val color = indexToColor[otherIdx] ?: LudoColor.GREEN
            val isClassic = mode == LudoGameMode.CLASSIC
            val isTeamUp = mode == LudoGameMode.TEAM_UP
            val pType = if (isClassic || isTeamUp) PlayerType.HUMAN else PlayerType.BOT
            val pName = if (isClassic) {
                "Player ${idx + 2}"
            } else if (isTeamUp) {
                // If Team Up mode, check who is our partner
                val isTeammate = (userIdx == 0 && otherIdx == 2) || (userIdx == 2 && otherIdx == 0) ||
                                 (userIdx == 1 && otherIdx == 3) || (userIdx == 3 && otherIdx == 1)
                if (isTeammate) "Partner" else "Player ${otherIdx + 1}"
            } else {
                if (shuffledNames.isNotEmpty()) shuffledNames.removeAt(0) else "Player_${otherIdx + 1}"
            }

            // Assign unique random levels and random avatarIds every match
            val otherLevel = (1..20).random()
            val otherAvatarId = (0..12).random()

            players.add(Player(id = otherIdx, name = pName, color = color, type = pType, level = otherLevel, avatarId = otherAvatarId))
        }

        // Sync setupTypes and setupNames for backward/visual compatibility
        val currentTypes = MutableList(4) { PlayerType.ABSENT }
        val currentNames = MutableList(4) { "" }
        currentTypes[userIdx] = PlayerType.HUMAN
        currentNames[userIdx] = uiState.value.username
        for (player in players) {
            currentTypes[player.id] = player.type
            currentNames[player.id] = player.name
        }
        setupTypes.value = currentTypes
        setupNames.value = currentNames

        // Clockwise sorting so turns cycle correctly in order
        players.sortBy { it.id }

        // Start background music for gameplay
        LudoAudioEngine.startBgm()

        // 3. Add tokens for each active player
        for (player in players) {
            for (tokenId in 0..3) {
                tokens.add(Token(id = tokenId, playerId = player.id, position = 0, color = player.color))
            }
        }

        val startMsg = when (mode) {
            LudoGameMode.CLASSIC -> "🎯 Classic Game started! ${players.firstOrNull { it.id == userIdx }?.name ?: uiState.value.username}'s turn is ready."
            LudoGameMode.ONE_VS_ONE -> "🎯 1v1 Match started!"
            LudoGameMode.VS_COMPUTER -> "🤖 VS Computer started!"
            LudoGameMode.TEAM_UP -> "🤝 Team Up (2v2) started! Red & Yellow vs Green & Blue!"
            LudoGameMode.HYBRID_ONLINE -> "🌐 Hybrid Online Match started! Finding live opponents..."
        }

        val currentLanguage = uiState.value.selectedLanguage
        val currentTheme = uiState.value.selectedTheme
        val currentSound = uiState.value.isSoundEnabled

        val baseColors = listOf(
            indexToColor[0] ?: LudoColor.RED,
            indexToColor[1] ?: LudoColor.GREEN,
            indexToColor[2] ?: LudoColor.YELLOW,
            indexToColor[3] ?: LudoColor.BLUE
        )

        val pings = if (mode == LudoGameMode.HYBRID_ONLINE) {
            mapOf(
                0 to (20..80).random(),
                1 to (25..90).random(),
                2 to (30..110).random(),
                3 to (10..40).random()
            )
        } else emptyMap()

        _uiState.update { currentState ->
            currentState.copy(
                gamePhase = GamePhase.PLAYING,
                gameMode = mode,
                players = players,
                tokens = tokens,
                currentPlayerIdx = players.first().id,
                diceRoll = null,
                hasRolled = false,
                isRolling = false,
                statusMessage = startMsg,
                winnerPlayerId = null,
                timeLeftSeconds = 300,
                selectedLanguage = currentLanguage,
                selectedTheme = currentTheme,
                isSoundEnabled = currentSound,
                baseColors = baseColors,
                onlinePlayerPings = pings,
                disconnectedPlayers = emptySet(),
                addedFriends = emptySet()
            )
        }

        timerJob?.cancel()
        val isTimerNeeded = mode == LudoGameMode.ONE_VS_ONE || (mode == LudoGameMode.HYBRID_ONLINE && uiState.value.onlineSubMode == OnlineSubMode.QUICK_PLAY)
        if (isTimerNeeded) {
            timerJob = viewModelScope.launch {
                while (uiState.value.timeLeftSeconds > 0 && uiState.value.gamePhase == GamePhase.PLAYING) {
                    delay(1000)
                    var playWarning = false
                    _uiState.update { currentState ->
                        val nextSeconds = currentState.timeLeftSeconds - 1
                        if (nextSeconds == 30 || nextSeconds in 1..5) {
                            playWarning = true
                        }
                        currentState.copy(
                            timeLeftSeconds = nextSeconds,
                            isTimeWarningDialogShowing = if (nextSeconds == 30) true else currentState.isTimeWarningDialogShowing
                        )
                    }
                    if (playWarning) {
                        LudoAudioEngine.playAlert()
                    }
                }
                if (uiState.value.timeLeftSeconds <= 0 && uiState.value.gamePhase == GamePhase.PLAYING) {
                    _uiState.update { it.copy(isTimeUpDialogShowing = true, isTimeWarningDialogShowing = false) }
                }
            }
        }

        if (mode == LudoGameMode.VS_COMPUTER) {
            viewModelScope.launch {
                delay(800)
                val randomBot = players.filter { it.type == PlayerType.BOT }.randomOrNull()
                if (randomBot != null) {
                    triggerBotChat("START", randomBot.id)
                }
            }
        }

        triggerBotIfNeeded()
    }

    private fun startTurnTimer(mode: LudoGameMode) {
        timerJob?.cancel()
        val isTimerNeeded = mode == LudoGameMode.ONE_VS_ONE || (mode == LudoGameMode.HYBRID_ONLINE && uiState.value.onlineSubMode == OnlineSubMode.QUICK_PLAY)
        if (isTimerNeeded) {
            timerJob = viewModelScope.launch {
                while (uiState.value.timeLeftSeconds > 0 && uiState.value.gamePhase == GamePhase.PLAYING) {
                    delay(1000)
                    var playWarning = false
                    _uiState.update { currentState ->
                        val nextSeconds = currentState.timeLeftSeconds - 1
                        if (nextSeconds == 30 || nextSeconds in 1..5) {
                            playWarning = true
                        }
                        currentState.copy(
                            timeLeftSeconds = nextSeconds,
                            isTimeWarningDialogShowing = if (nextSeconds == 30) true else currentState.isTimeWarningDialogShowing
                        )
                    }
                    if (playWarning) {
                        LudoAudioEngine.playAlert()
                    }
                }
                if (uiState.value.timeLeftSeconds <= 0 && uiState.value.gamePhase == GamePhase.PLAYING) {
                    _uiState.update { currentState ->
                        currentState.copy(isTimeUpDialogShowing = true, isTimeWarningDialogShowing = false)
                    }
                }
            }
        }
    }

    private fun searchOrCreateFirebaseMatch(wager: Int) {
        val currentMode = _uiState.value.gameMode
        val playerCount = selectedPlayerCount.value
        val userColor = selectedUserColor.value
        val userIdx = 3 // Always index 3 for human user

        // Pre-populate lobby player list so Matchmaking Screen displays User + Searching Slots immediately
        val lobbyPlayers = mutableListOf<Player>()
        val availableColors = LudoColor.values().filter { it != userColor }.toMutableList()
        val indexToColor = mutableMapOf<Int, LudoColor>()
        indexToColor[3] = userColor
        indexToColor[0] = availableColors.removeAt(0)
        indexToColor[1] = availableColors.removeAt(0)
        indexToColor[2] = availableColors.removeAt(0)

        val userLevel = 1 + (uiState.value.winsComputer + uiState.value.winsOneVsOne + uiState.value.winsOnline) / 3
        lobbyPlayers.add(Player(id = userIdx, name = uiState.value.username, color = userColor, type = PlayerType.HUMAN, level = userLevel, avatarId = uiState.value.selectedAvatarId))

        val otherIndices = when (playerCount) {
            2 -> listOf((userIdx + 2) % 4)
            3 -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4)
            else -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4, (userIdx + 3) % 4)
        }

        for ((slotIdx, otherIdx) in otherIndices.withIndex()) {
            val color = indexToColor[otherIdx] ?: LudoColor.GREEN
            lobbyPlayers.add(Player(id = otherIdx, name = "Searching Slot ${slotIdx + 1}...", color = color, type = PlayerType.HUMAN))
        }
        lobbyPlayers.sortBy { it.id }

        _uiState.update { 
            it.copy(
                statusMessage = "📶 Searching for Live Online Opponents... ⏳",
                gameMode = currentMode,
                isFindingOpponent = true,
                players = lobbyPlayers
            )
        }

        val matchesRef = try { firebaseDb?.getReference("matches") } catch (e: Exception) { null }

        // Fast timeout safeguard: if real Firebase database doesn't connect/respond in 1.8 seconds, complete lobby matchmaking seamlessly!
        val timeoutJob = viewModelScope.launch {
            delay(1800)
            if (_uiState.value.isFindingOpponent && _uiState.value.gamePhase != GamePhase.PLAYING) {
                _uiState.update { it.copy(statusMessage = "🤝 Opponents found! Syncing online game state... 🚀") }
                delay(400)
                if (_uiState.value.isFindingOpponent && _uiState.value.gamePhase != GamePhase.PLAYING) {
                    startSimulatedOnlineMatch(wager)
                }
            }
        }

        if (matchesRef == null) {
            return
        }

        try {
            val targetModeStr = if (currentMode == LudoGameMode.ONE_VS_ONE) "1v1" else "multiplayer"
            matchesRef.orderByChild("status").equalTo("waiting").limitToLast(10).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    timeoutJob.cancel()
                    var foundMatch = false
                    for (matchSnapshot in snapshot.children) {
                        val matchWager = matchSnapshot.child("wager").getValue(Int::class.java) ?: 500
                        val matchMode = matchSnapshot.child("mode").getValue(String::class.java) ?: "1v1"
                        val matchPlayerCount = matchSnapshot.child("targetPlayerCount").getValue(Int::class.java) ?: 2
                        if (matchWager == wager && matchMode == targetModeStr && matchPlayerCount == playerCount) {
                            val matchId = matchSnapshot.key ?: continue
                            val playersSnap = matchSnapshot.child("players")
                            val occupiedSlots = playersSnap.children.mapNotNull { it.key?.toIntOrNull() }
                            val neededSlots = when (playerCount) {
                                2 -> listOf(1)
                                3 -> listOf(0, 1)
                                else -> listOf(0, 1, 2)
                            }
                            val mySlot = neededSlots.firstOrNull { !occupiedSlots.contains(it) }
                            if (mySlot != null) {
                                joinFirebaseMatch(matchId, mySlot, playerCount)
                                foundMatch = true
                                break
                            }
                        }
                    }
                    if (!foundMatch) {
                        createFirebaseMatch(wager, playerCount)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    startSimulatedOnlineMatch(wager)
                }
            })
        } catch (e: Exception) {
            // Handled via timeoutJob
        }
    }

    private fun createFirebaseMatch(wager: Int, targetPlayerCount: Int) {
        val matchesRef = firebaseDb?.getReference("matches") ?: return
        val matchId = matchesRef.push().key ?: ("match_" + System.currentTimeMillis())
        activeFirebaseMatchId = matchId
        myFirebasePlayerSlot = 3 // Host starts as player slot 3 (Human corner)
        
        val playerMap = mapOf(
            "name" to uiState.value.username,
            "avatarId" to uiState.value.selectedAvatarId,
            "slot" to 3,
            "color" to selectedUserColor.value.name,
            "isBot" to false
        )
        
        val initialTokens = mutableMapOf<String, Int>()
        for (pId in 0..3) {
            for (tId in 0..3) {
                initialTokens["${pId}_${tId}"] = 0
            }
        }

        val targetModeStr = if (_uiState.value.gameMode == LudoGameMode.ONE_VS_ONE) "1v1" else "multiplayer"

        val matchData = mapOf(
            "matchId" to matchId,
            "status" to "waiting",
            "wager" to wager,
            "mode" to targetModeStr,
            "targetPlayerCount" to targetPlayerCount,
            "creatorName" to uiState.value.username,
            "currentPlayerIdx" to 3,
            "diceRoll" to null,
            "hasRolled" to false,
            "senderId" to 3,
            "statusMessage" to "Room created! Waiting for $targetPlayerCount players to join... ⏳",
            "tokenPositions" to initialTokens,
            "players" to mapOf("3" to playerMap)
        )

        _uiState.update { 
            it.copy(statusMessage = "📶 Created Room: ${matchId.takeLast(6).uppercase()}. Waiting for players... ⏳")
        }

        matchesRef.child(matchId).setValue(matchData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listenToFirebaseMatch(matchId)
            } else {
                startSimulatedOnlineMatch(wager)
            }
        }
    }

    private fun joinFirebaseMatch(matchId: String, slot: Int, targetPlayerCount: Int) {
        val matchRef = firebaseDb?.getReference("matches")?.child(matchId) ?: return
        activeFirebaseMatchId = matchId
        myFirebasePlayerSlot = slot
        
        val colors = when (slot) {
            0 -> LudoColor.YELLOW
            1 -> LudoColor.GREEN
            2 -> LudoColor.RED
            else -> LudoColor.GREEN
        }
        
        val playerMap = mapOf(
            "name" to uiState.value.username,
            "avatarId" to uiState.value.selectedAvatarId,
            "slot" to slot,
            "color" to colors.name,
            "isBot" to false
        )

        _uiState.update { 
            it.copy(statusMessage = "🤝 Game found! Syncing state...")
        }

        matchRef.child("players").child(slot.toString()).setValue(playerMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                matchRef.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(playerSnapshot: DataSnapshot) {
                        val joinedCount = playerSnapshot.childrenCount.toInt()
                        if (joinedCount >= targetPlayerCount) {
                            matchRef.updateChildren(mapOf(
                                "status" to "playing",
                                "statusMessage" to "Match Started! Real-time syncing active!"
                            ))
                        } else {
                            matchRef.updateChildren(mapOf(
                                "statusMessage" to "Player joined! Waiting for remaining players... ⏳"
                            ))
                        }
                        listenToFirebaseMatch(matchId)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        listenToFirebaseMatch(matchId)
                    }
                })
            } else {
                startSimulatedOnlineMatch(uiState.value.selectedWagerAmount)
            }
        }
    }

    private fun listenToFirebaseMatch(matchId: String) {
        val matchRef = firebaseDb?.getReference("matches")?.child(matchId) ?: return
        
        firebaseMatchListener?.let { matchRef.removeEventListener(it) }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                val status = snapshot.child("status").getValue(String::class.java) ?: "waiting"
                val senderId = snapshot.child("senderId").getValue(Int::class.java) ?: -1
                val currentPlayerIdx = snapshot.child("currentPlayerIdx").getValue(Int::class.java) ?: 3
                val diceRoll = snapshot.child("diceRoll").getValue(Int::class.java)
                val hasRolled = snapshot.child("hasRolled").getValue(Boolean::class.java) ?: false
                val statusMessage = snapshot.child("statusMessage").getValue(String::class.java) ?: ""
                
                if (senderId != myFirebasePlayerSlot) {
                    isUpdatingFromFirebase = true
                    
                    val playersSnapshot = snapshot.child("players")
                    val updatedPlayers = mutableListOf<Player>()
                    
                    val targetPlayerCount = snapshot.child("targetPlayerCount").getValue(Int::class.java) ?: 2
                    val expectedSlots = when (targetPlayerCount) {
                        2 -> listOf(3, 1)
                        3 -> listOf(3, 0, 1)
                        else -> listOf(3, 0, 1, 2)
                    }

                    for (slot in expectedSlots) {
                        val pSnap = playersSnapshot.child(slot.toString())
                        if (pSnap.exists()) {
                            val name = pSnap.child("name").getValue(String::class.java) ?: "Player ${slot + 1}"
                            val avatarId = pSnap.child("avatarId").getValue(Int::class.java) ?: 0
                            val colorStr = pSnap.child("color").getValue(String::class.java) ?: "RED"
                            val color = try { LudoColor.valueOf(colorStr) } catch(e: Exception) {
                                when (slot) {
                                    3 -> LudoColor.BLUE
                                    0 -> LudoColor.YELLOW
                                    1 -> LudoColor.GREEN
                                    else -> LudoColor.RED
                                }
                            }
                            updatedPlayers.add(Player(id = slot, name = name, color = color, type = PlayerType.HUMAN, avatarId = avatarId))
                        } else {
                            if (status == "waiting") {
                                val placeholderColor = when (slot) {
                                    0 -> LudoColor.YELLOW
                                    1 -> LudoColor.GREEN
                                    2 -> LudoColor.RED
                                    else -> LudoColor.BLUE
                                }
                                updatedPlayers.add(Player(id = slot, name = "Waiting Opponent... ⏳", color = placeholderColor, type = PlayerType.BOT))
                            }
                        }
                    }

                    updatedPlayers.sortBy { it.id }

                    val currentState = _uiState.value
                    val isNewGameStarting = currentState.gamePhase != GamePhase.PLAYING && status == "playing"
                    val tokensSnapshot = snapshot.child("tokenPositions")

                    val baseTokensList = if (isNewGameStarting || currentState.tokens.isEmpty()) {
                        val newTokens = mutableListOf<Token>()
                        for (player in updatedPlayers) {
                            for (tokenId in 0..3) {
                                newTokens.add(Token(id = tokenId, playerId = player.id, position = 0, color = player.color))
                            }
                        }
                        newTokens
                    } else {
                        currentState.tokens
                    }

                    val updatedTokens = baseTokensList.map { token ->
                        val key = "${token.playerId}_${token.id}"
                        val pos = tokensSnapshot.child(key).getValue(Int::class.java) ?: token.position
                        token.copy(position = pos)
                    }.toMutableList()

                    _uiState.update { currentState ->
                        currentState.copy(
                            gamePhase = if (status == "playing") GamePhase.PLAYING else currentState.gamePhase,
                            isFindingOpponent = if (status == "playing") false else currentState.isFindingOpponent,
                            players = updatedPlayers.ifEmpty { currentState.players },
                            tokens = updatedTokens.ifEmpty { currentState.tokens },
                            currentPlayerIdx = currentPlayerIdx,
                            diceRoll = diceRoll,
                            hasRolled = hasRolled,
                            statusMessage = "📶 [Live] $statusMessage"
                        )
                    }
                    
                    isUpdatingFromFirebase = false
                } else {
                    if (status == "playing" && _uiState.value.gamePhase != GamePhase.PLAYING) {
                        setupLocalPlayersAndStartFirebaseGame(snapshot)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        
        firebaseMatchListener = listener
        matchRef.addValueEventListener(listener)
    }

    private fun setupLocalPlayersAndStartFirebaseGame(snapshot: DataSnapshot) {
        val playersSnapshot = snapshot.child("players")
        val players = mutableListOf<Player>()
        val tokens = mutableListOf<Token>()
        
        for (slotSnap in playersSnapshot.children) {
            val slotKey = slotSnap.key ?: continue
            val slotId = slotKey.toIntOrNull() ?: continue
            val name = slotSnap.child("name").getValue(String::class.java) ?: "Player ${slotId + 1}"
            val avatarId = slotSnap.child("avatarId").getValue(Int::class.java) ?: 0
            val colorStr = slotSnap.child("color").getValue(String::class.java) ?: "RED"
            val color = try { LudoColor.valueOf(colorStr) } catch(e: Exception) {
                when (slotId) {
                    3 -> LudoColor.BLUE
                    0 -> LudoColor.YELLOW
                    1 -> LudoColor.GREEN
                    else -> LudoColor.RED
                }
            }
            players.add(Player(id = slotId, name = name, color = color, type = PlayerType.HUMAN, avatarId = avatarId))
        }

        players.sortBy { it.id }

        for (player in players) {
            for (tokenId in 0..3) {
                tokens.add(Token(id = tokenId, playerId = player.id, position = 0, color = player.color))
            }
        }

        val opponentNames = players.filter { it.id != myFirebasePlayerSlot }.joinToString(", ") { it.name }

        LudoAudioEngine.startBgm()

        _uiState.update { currentState ->
            currentState.copy(
                gamePhase = GamePhase.PLAYING,
                isFindingOpponent = false,
                players = players,
                tokens = tokens,
                currentPlayerIdx = 3, // Host goes first!
                diceRoll = null,
                hasRolled = false,
                isRolling = false,
                statusMessage = "🎯 Firebase Match Started! Opponents: $opponentNames!"
            )
        }
        startTurnTimer(LudoGameMode.HYBRID_ONLINE)
    }

    private fun syncLocalStateToFirebase(actionType: String, actionMsg: String) {
        val matchId = activeFirebaseMatchId ?: return
        if (!isFirebaseAvailable || firebaseDb == null) return
        if (isUpdatingFromFirebase) return
        
        val matchRef = firebaseDb?.getReference("matches")?.child(matchId) ?: return
        val state = _uiState.value
        
        val tokenMap = mutableMapOf<String, Int>()
        state.tokens.forEach { token ->
            tokenMap["${token.playerId}_${token.id}"] = token.position
        }
        
        val updates = mapOf(
            "currentPlayerIdx" to state.currentPlayerIdx,
            "diceRoll" to state.diceRoll,
            "hasRolled" to state.hasRolled,
            "senderId" to myFirebasePlayerSlot,
            "statusMessage" to actionMsg,
            "tokenPositions" to tokenMap
        )
        
        matchRef.updateChildren(updates)
    }

    fun cleanupFirebaseMatch() {
        val matchId = activeFirebaseMatchId
        val listener = firebaseMatchListener
        if (matchId != null && firebaseDb != null) {
            try {
                if (listener != null) {
                    firebaseDb?.getReference("matches")?.child(matchId)?.removeEventListener(listener)
                }
                // Delete match node from database completely to keep server storage, reads, and writes at a absolute minimum
                firebaseDb?.getReference("matches")?.child(matchId)?.removeValue()
            } catch (e: Exception) {}
        }
        activeFirebaseMatchId = null
        firebaseMatchListener = null
    }

    fun cancelMatchmaking() {
        val wager = uiState.value.selectedWagerAmount
        val mode = uiState.value.gameMode
        val isWagerMode = mode == LudoGameMode.ONE_VS_ONE || mode == LudoGameMode.HYBRID_ONLINE
        
        cleanupFirebaseMatch()
        _uiState.update { currentState ->
            val nextCoins = if (isWagerMode) currentState.coins + wager else currentState.coins
            sharedPrefs?.edit()?.putInt("coins", nextCoins)?.commit()
            currentState.copy(
                coins = nextCoins,
                isFindingOpponent = false,
                gamePhase = GamePhase.SETUP,
                statusMessage = "Matchmaking cancelled."
            )
        }
    }

    private fun startSimulatedOnlineMatch(wager: Int) {
        val userColor = selectedUserColor.value
        val userIdx = 3
        val playerCount = selectedPlayerCount.value

        val players = mutableListOf<Player>()
        val tokens = mutableListOf<Token>()

        val availableColors = LudoColor.values().filter { it != userColor }.toMutableList()
        val indexToColor = mutableMapOf<Int, LudoColor>()
        indexToColor[3] = userColor
        indexToColor[0] = availableColors.removeAt(0)
        indexToColor[1] = availableColors.removeAt(0)
        indexToColor[2] = availableColors.removeAt(0)

        val userLevel = 1 + (uiState.value.winsComputer + uiState.value.winsOneVsOne + uiState.value.winsOnline) / 3
        players.add(Player(id = userIdx, name = uiState.value.username, color = userColor, type = PlayerType.HUMAN, level = userLevel, avatarId = uiState.value.selectedAvatarId))

        val otherIndices = when (playerCount) {
            2 -> listOf((userIdx + 2) % 4)
            3 -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4)
            else -> listOf((userIdx + 1) % 4, (userIdx + 2) % 4, (userIdx + 3) % 4)
        }

        val shuffledNames = realisticPlayerNames.shuffled().toMutableList()
        for ((idx, otherIdx) in otherIndices.withIndex()) {
            val color = indexToColor[otherIdx] ?: LudoColor.GREEN
            val pName = if (shuffledNames.isNotEmpty()) shuffledNames.removeAt(0) else "Player_${otherIdx + 1}"
            val otherAvatarId = (0..12).random()
            val otherLevel = (1..20).random()
            players.add(Player(id = otherIdx, name = pName, color = color, type = PlayerType.BOT, level = otherLevel, avatarId = otherAvatarId))
        }

        players.sortBy { it.id }

        for (player in players) {
            for (tokenId in 0..3) {
                tokens.add(Token(id = tokenId, playerId = player.id, position = 0, color = player.color))
            }
        }

        val baseColors = listOf(
            indexToColor[0] ?: LudoColor.RED,
            indexToColor[1] ?: LudoColor.GREEN,
            indexToColor[2] ?: LudoColor.YELLOW,
            indexToColor[3] ?: LudoColor.BLUE
        )

        val pings = mapOf(
            0 to (20..80).random(),
            1 to (25..90).random(),
            2 to (30..110).random(),
            3 to (10..40).random()
        )

        val currentMode = uiState.value.gameMode
        LudoAudioEngine.startBgm()
        _uiState.update { currentState ->
            currentState.copy(
                gamePhase = GamePhase.PLAYING,
                isFindingOpponent = false,
                gameMode = currentMode,
                players = players,
                tokens = tokens,
                currentPlayerIdx = players.first().id,
                diceRoll = null,
                hasRolled = false,
                isRolling = false,
                statusMessage = "🌐 Online Match Started! Connected to Lobby (Ping: 28 ms) 🎯",
                winnerPlayerId = null,
                timeLeftSeconds = 300,
                baseColors = baseColors,
                onlinePlayerPings = pings,
                disconnectedPlayers = emptySet(),
                addedFriends = emptySet()
            )
        }

        startTurnTimer(LudoGameMode.HYBRID_ONLINE)
    }

    private fun endGameOnTimeOut() {
        val scoreMap = uiState.value.players.associateWith { player ->
            uiState.value.tokens.filter { it.playerId == player.id }.sumOf { it.position }
        }
        val winner = scoreMap.maxByOrNull { it.value }?.key
        val isHumanWinner = winner?.type == PlayerType.HUMAN

        val nextCoins = if (isHumanWinner) {
            val winBonus = if (uiState.value.gameMode == LudoGameMode.ONE_VS_ONE) {
                uiState.value.selectedWagerAmount * 2
            } else {
                150
            }
            uiState.value.coins + winBonus
        } else {
            uiState.value.coins
        }

        if (isHumanWinner) {
            recordUserWin(uiState.value.gameMode)
            _uiState.update {
                it.copy(
                    gamePhase = GamePhase.FINISHED,
                    winnerPlayerId = winner?.id,
                    coins = nextCoins,
                    statusMessage = "⏳ Time's up! ${winner?.name ?: "No one"} wins by highest progress and coins are doubled to $nextCoins 🪙!"
                )
            }
            sharedPrefs?.edit()?.putInt("coins", nextCoins)?.commit()
        } else {
            _uiState.update {
                it.copy(
                    gamePhase = GamePhase.FINISHED,
                    winnerPlayerId = winner?.id,
                    statusMessage = "⏳ Time's up! ${winner?.name ?: "No one"} wins by highest progress!"
                )
            }
        }
    }

    fun resetToSetup() {
        cleanupFirebaseMatch()
        timerJob?.cancel()
        _uiState.update { currentState ->
            LudoState(
                gamePhase = GamePhase.MODE_SELECT,
                statusMessage = "Select a Ludo Game Mode to play!",
                selectedLanguage = currentState.selectedLanguage,
                selectedTheme = currentState.selectedTheme,
                isSoundEnabled = currentState.isSoundEnabled,
                coins = currentState.coins,
                username = currentState.username,
                unlockedThemes = currentState.unlockedThemes,
                selectedTokenStyle = currentState.selectedTokenStyle,
                unlockedTokenStyles = currentState.unlockedTokenStyles,
                selectedDiceStyle = currentState.selectedDiceStyle,
                unlockedDiceStyles = currentState.unlockedDiceStyles,
                selectedWagerAmount = currentState.selectedWagerAmount,
                isDailyRewardAvailable = currentState.isDailyRewardAvailable,
                lastCheckInTime = currentState.lastCheckInTime,
                friendsList = currentState.friendsList,
                addedFriends = emptySet()
            )
        }
        sharedPrefs?.edit()?.putInt("coins", _uiState.value.coins)?.commit()
    }

    fun rollDice() {
        val state = _uiState.value
        if (state.gamePhase != GamePhase.PLAYING || state.hasRolled || state.isRolling || state.isMovingToken) return

        if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
            if (state.currentPlayerIdx != myFirebasePlayerSlot) {
                // Out of turn roll restricted!
                return
            }
        }

        viewModelScope.launch {
            LudoAudioEngine.playDiceRoll()
            _uiState.update { it.copy(isRolling = true, diceRoll = null) }
            
            // Dice roll animation effect (perfectly balanced speed and beautiful duration)
            for (i in 1..8) {
                _uiState.update { it.copy(diceRoll = Random.nextInt(1, 7)) }
                delay(60)
            }

            val finalRoll = if (_uiState.value.nextRollIsSix) {
                _uiState.update { it.copy(nextRollIsSix = false) }
                6
            } else {
                Random.nextInt(1, 7)
            }
            val currentPlayer = getCurrentPlayer() ?: return@launch

            _uiState.update { 
                it.copy(
                    isRolling = false,
                    diceRoll = finalRoll,
                    hasRolled = true
                )
            }

            if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
                syncLocalStateToFirebase("ROLL", "${currentPlayer.name} rolled a $finalRoll 🎲!")
            }

            handleDiceRollOutcome(finalRoll)
        }
    }

    private suspend fun handleDiceRollOutcome(roll: Int) {
        val state = _uiState.value
        val currentPlayer = getCurrentPlayer() ?: return
        val validMoves = getValidMovesForCurrentPlayer(roll)

        if (roll == 6) {
            val newSixes = state.consecutiveSixes + 1
            _uiState.update { it.copy(consecutiveSixes = newSixes) }
            
            if (newSixes == 3) {
                // Three consecutive sixes - turn cancelled!
                _uiState.update { 
                    it.copy(
                        statusMessage = "🚫 3 consecutive 6s! ${currentPlayer.name}'s turn is cancelled.",
                        consecutiveSixes = 0
                    )
                }
                val limitDelay = ((if (state.gameMode == LudoGameMode.HYBRID_ONLINE) 150L else 200L) * getGameplaySpeedMultiplier()).toLong()
                delay(limitDelay)
                passTurn()
                return
            }
        } else {
            _uiState.update { it.copy(consecutiveSixes = 0) }
        }

        if (validMoves.isEmpty()) {
            _uiState.update { 
                it.copy(
                    statusMessage = "🎲 ${currentPlayer.name} rolled a $roll. No valid moves possible!"
                )
            }
            val noMoveDelay = ((if (state.gameMode == LudoGameMode.HYBRID_ONLINE) 150L else 200L) * getGameplaySpeedMultiplier()).toLong()
            delay(noMoveDelay)
            passTurn()
        } else {
            // If player is Bot, make a decision automatically!
            if (currentPlayer.type == PlayerType.BOT) {
                _uiState.update { 
                    it.copy(
                        statusMessage = "🎲 ${currentPlayer.name} rolled a $roll! Bot thinking..."
                    )
                }
                if (roll == 6) {
                    triggerBotChat("ROLL_6", currentPlayer.id)
                }
                val botDelay = ((if (state.gameMode == LudoGameMode.HYBRID_ONLINE) 100L else 150L) * getGameplaySpeedMultiplier()).toLong()
                delay(botDelay)
                makeBotMove(validMoves)
            } else if (validMoves.size == 1) {
                // Auto-move for human player if only 1 token can move!
                val onlyToken = validMoves.first()
                _uiState.update { 
                    it.copy(
                        statusMessage = "🎲 You rolled a $roll! Auto-moving your only playable piece..."
                    )
                }
                val autoDelay = ((if (state.gameMode == LudoGameMode.HYBRID_ONLINE) 150L else 200L) * getGameplaySpeedMultiplier()).toLong()
                delay(autoDelay) // Clear delay so player sees what they rolled before it moves!
                
                // Double check that state hasn't changed or been reset while waiting
                val currentState = _uiState.value
                if (currentState.gamePhase == GamePhase.PLAYING && 
                    currentState.currentPlayerIdx == currentPlayer.id && 
                    !currentState.isMovingToken) {
                    moveToken(onlyToken, roll)
                }
            } else {
                _uiState.update { 
                    it.copy(
                        statusMessage = "🎲 You rolled a $roll! Select a token to move."
                    )
                }
            }
        }
    }

    private fun getValidMovesForCurrentPlayer(roll: Int): List<Token> {
        val state = _uiState.value
        val currentPlayer = getCurrentPlayer() ?: return emptyList()
        
        return state.tokens.filter { token ->
            token.playerId == currentPlayer.id && isValidMove(token, roll)
        }
    }

    private fun isValidMove(token: Token, roll: Int): Boolean {
        if (token.position == 0) {
            // Can only release from yard if rolled a 6
            return roll == 6
        }
        // Cannot exceed center home (57)
        return token.position + roll <= 57
    }

    fun selectTokenToMove(token: Token) {
        val state = _uiState.value
        if (!state.hasRolled || state.isRolling || state.isMovingToken || state.gamePhase != GamePhase.PLAYING) return
        
        if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
            if (token.playerId != myFirebasePlayerSlot || state.currentPlayerIdx != myFirebasePlayerSlot) {
                return // Not your token or not your turn
            }
        } else {
            val currentPlayer = getCurrentPlayer() ?: return
            if (token.playerId != currentPlayer.id) return // Not your token
        }

        val roll = state.diceRoll ?: return
        if (!isValidMove(token, roll)) return // Invalid token choice

        moveToken(token, roll)
    }

    private fun moveToken(token: Token, steps: Int) {
        val state = _uiState.value
        if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
            if (state.currentPlayerIdx != myFirebasePlayerSlot) {
                // Not my turn to move!
                return
            }
        }
        _uiState.update { it.copy(isMovingToken = true, movingTokenId = token.id) }
        LudoAudioEngine.playTokenMove()
        viewModelScope.launch {
            val startPos = token.position
            val targetPos = if (startPos == 0) 1 else startPos + steps
            val actualSteps = if (startPos == 0) 1 else steps

            val isBot = getCurrentPlayer()?.type == PlayerType.BOT
            val baseHopDelay = if (isBot) 90L else 115L
            val hopDelay = (baseHopDelay * getGameplaySpeedMultiplier()).toLong()

            // Animate token steps (perfectly balanced speed - comfortable to follow)
            var currentPos = startPos
            for (step in 1..actualSteps) {
                currentPos++
                updateTokenPosition(token, currentPos)
                LudoAudioEngine.playTokenHop()
                delay(hopDelay)
            }

            _uiState.update { it.copy(movingTokenId = null) }

            // Post-move check
            handlePostMoveState(token.copy(position = targetPos), steps)
        }
    }

    private fun updateTokenPosition(token: Token, pos: Int) {
        _uiState.update { state ->
            val updatedTokens = state.tokens.map { t ->
                if (t.playerId == token.playerId && t.id == token.id) {
                    t.copy(position = pos)
                } else t
            }
            state.copy(tokens = updatedTokens)
        }
    }

    private suspend fun handlePostMoveState(movedToken: Token, roll: Int) {
        val state = _uiState.value
        val currentPlayer = getCurrentPlayer() ?: return

        var awardExtraTurn = false
        var message = ""

        // 1. Check if token reached Center Home (57)
        if (movedToken.position == 57) {
            awardExtraTurn = true
            message = "🎉 ${currentPlayer.name} reached Home!"
            
            // Check if player completed the game
            val playerTokens = state.tokens.filter { it.playerId == currentPlayer.id }
            val isQuickPlay = state.gameMode == LudoGameMode.HYBRID_ONLINE && state.onlineSubMode == OnlineSubMode.QUICK_PLAY
            val allCompleted = if (isQuickPlay) {
                playerTokens.any { it.position == 57 }
            } else {
                playerTokens.all { it.position == 57 }
            }
            if (allCompleted) {
                // Player completed the board!
                LudoAudioEngine.playVictory()
                val isHumanWinner = currentPlayer.type == PlayerType.HUMAN
                val nextCoins = if (isHumanWinner) {
                    val winBonus = if (state.gameMode == LudoGameMode.ONE_VS_ONE) {
                        state.selectedWagerAmount * 2
                    } else if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
                        state.selectedWagerAmount * state.players.size
                    } else {
                        150
                    }
                    state.coins + winBonus
                } else {
                    state.coins
                }

                if (isHumanWinner) {
                    recordUserWin(state.gameMode)
                    _uiState.update { 
                        it.copy(
                            isMovingToken = false,
                            gamePhase = GamePhase.FINISHED,
                            winnerPlayerId = currentPlayer.id,
                            coins = nextCoins,
                            statusMessage = "🏆🏆 CONGRATULATIONS! ${currentPlayer.name} won and coins are doubled to $nextCoins 🪙!"
                        )
                    }
                    sharedPrefs?.edit()?.putInt("coins", nextCoins)?.commit()
                } else {
                    _uiState.update { 
                        it.copy(
                            isMovingToken = false,
                            gamePhase = GamePhase.FINISHED,
                            winnerPlayerId = currentPlayer.id,
                            statusMessage = "🏆🏆 CONGRATULATIONS! ${currentPlayer.name} has won the Ludo Match!"
                        )
                    }
                }
                triggerBotChat("WINNER", currentPlayer.id)
                return
            }
        } else {
            // 2. Check for Capture (Cut opponent token)
            // Get landed coordinate
            val landedCoord = LudoCoordinates.getTokenCoordinates(movedToken.playerId, movedToken.id, movedToken.position)
            val landedRow = landedCoord.first.toInt()
            val landedCol = landedCoord.second.toInt()

            val isSafeStar = LudoCoordinates.isCellSafe(landedRow, landedCol)
            val isHomeStretch = movedToken.position >= 52

            // In Team Up mode, 2 tokens of teammates on the same cell create a safe block
            val isTeammateBlock = if (state.gameMode == LudoGameMode.TEAM_UP) {
                state.tokens.any { other ->
                    other.id != movedToken.id &&
                    other.position in 1..51 &&
                    movedToken.position in 1..51 &&
                    ((movedToken.playerId == 0 && other.playerId == 2) ||
                     (movedToken.playerId == 2 && other.playerId == 0) ||
                     (movedToken.playerId == 1 && other.playerId == 3) ||
                     (movedToken.playerId == 3 && other.playerId == 1)) &&
                    LudoCoordinates.getTokenCoordinates(other.playerId, other.id, other.position).let { 
                        it.first.toInt() == landedRow && it.second.toInt() == landedCol 
                    }
                }
            } else false

            val isSafe = isSafeStar || isHomeStretch || isTeammateBlock

            if (!isSafe) {
                // Find opponent tokens at the same cell
                val capturedToken = state.tokens.firstOrNull { oppToken ->
                    if (oppToken.playerId != movedToken.playerId && oppToken.position in 1..51) {
                        val isTeammate = state.gameMode == LudoGameMode.TEAM_UP && (
                            (movedToken.playerId == 0 && oppToken.playerId == 2) ||
                            (movedToken.playerId == 2 && oppToken.playerId == 0) ||
                            (movedToken.playerId == 1 && oppToken.playerId == 3) ||
                            (movedToken.playerId == 3 && oppToken.playerId == 1)
                        )
                        if (isTeammate) {
                            false
                        } else {
                            val oppCoord = LudoCoordinates.getTokenCoordinates(oppToken.playerId, oppToken.id, oppToken.position)
                            oppCoord.first.toInt() == landedRow && oppCoord.second.toInt() == landedCol
                        }
                    } else false
                }

                if (capturedToken != null) {
                    LudoAudioEngine.playTokenCaptured()
                    awardExtraTurn = true
                    // Reset opponent token to 0
                    updateTokenPosition(capturedToken, 0)
                    val opponentPlayer = state.players.firstOrNull { it.id == capturedToken.playerId }
                    val opponentName = opponentPlayer?.name ?: "Opponent"
                    message = "💥 BOOM! ${currentPlayer.name} cut ${opponentName}'s token! Extra Roll!"
                    triggerBotChat("CAPTURED", currentPlayer.id, targetPlayerId = capturedToken.playerId)
                }
            }
        }

        // 3. Roll 6 rule
        if (roll == 6 && !awardExtraTurn) {
            awardExtraTurn = true
            message = "🎲 ${currentPlayer.name} rolled a 6 and gets another turn!"
        }

        if (awardExtraTurn) {
            _uiState.update { 
                it.copy(
                    isMovingToken = false,
                    hasRolled = false,
                    statusMessage = if (message.isNotBlank()) message else "🎲 Roll again, ${currentPlayer.name}!"
                )
            }
            if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
                syncLocalStateToFirebase("MOVE", if (message.isNotBlank()) message else "Move finished. ${currentPlayer.name} gets an extra roll!")
            }
            delay(100)
            triggerBotIfNeeded()
        } else {
            passTurn()
        }
    }

    private fun passTurn() {
        val state = _uiState.value
        if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
            if (state.currentPlayerIdx != myFirebasePlayerSlot) {
                // Ignore local pass calls if it is not my turn, since the actual active player handles passing and syncing via Firebase!
                return
            }
        }
        
        // Handle random online player connection updates for HYBRID_ONLINE mode
        if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
            val rand = Random.nextFloat()
            if (rand < 0.04f) {
                // Trigger connection drop for an online player who is not the user (id 3) and not already disconnected
                val candidate = state.players.filter { it.id != 3 && !state.disconnectedPlayers.contains(it.id) }.randomOrNull()
                if (candidate != null) {
                    val updatedDisconnected = state.disconnectedPlayers + candidate.id
                    val disconnectMsg = "⚠️ ${candidate.name} lost connection! Gemini AI fallback takeover active."
                    _uiState.update { currentState ->
                        currentState.copy(
                            disconnectedPlayers = updatedDisconnected,
                            statusMessage = disconnectMsg
                        )
                    }
                    addChatMessage("System", null, disconnectMsg)
                    LudoAudioEngine.playAlert()
                }
            } else if (rand < 0.06f && state.disconnectedPlayers.isNotEmpty()) {
                // Reconnect a disconnected player!
                val candidateId = state.disconnectedPlayers.random()
                val candidate = state.players.firstOrNull { it.id == candidateId }
                if (candidate != null) {
                    val updatedDisconnected = state.disconnectedPlayers - candidateId
                    val reconnectMsg = "📶 ${candidate.name} reconnected successfully!"
                    _uiState.update { currentState ->
                        currentState.copy(
                            disconnectedPlayers = updatedDisconnected,
                            statusMessage = reconnectMsg
                        )
                    }
                    addChatMessage("System", null, reconnectMsg)
                }
            }
        }

        val currentIdx = state.currentPlayerIdx
        
        // Find next active player index
        var nextIdx = (currentIdx + 1) % 4
        // Safe check loop
        var found = false
        for (i in 1..4) {
            val potentialPlayer = state.players.firstOrNull { it.id == nextIdx }
            if (potentialPlayer != null && !potentialPlayer.isCompleted) {
                found = true
                break
            }
            nextIdx = (nextIdx + 1) % 4
        }

        if (found) {
            LudoAudioEngine.playTurnPass()
            val nextPlayer = state.players.first { it.id == nextIdx }
            _uiState.update { 
                it.copy(
                    isMovingToken = false,
                    currentPlayerIdx = nextIdx,
                    hasRolled = false,
                    diceRoll = null,
                    consecutiveSixes = 0,
                    statusMessage = "🎯 Next up: ${nextPlayer.name}'s turn."
                )
            }
            if (state.gameMode == LudoGameMode.HYBRID_ONLINE && activeFirebaseMatchId != null) {
                syncLocalStateToFirebase("PASS", "Turn passed to ${nextPlayer.name}!")
            }
            triggerBotIfNeeded()
        } else {
            // All players completed!
            _uiState.update { 
                it.copy(
                    isMovingToken = false,
                    gamePhase = GamePhase.FINISHED,
                    statusMessage = "Game completed!"
                )
            }
        }
    }

    private fun triggerBotIfNeeded() {
        val nextPlayer = getCurrentPlayer() ?: return
        if (nextPlayer.type == PlayerType.BOT) {
            viewModelScope.launch {
                val baseBotTriggerDelay = if (uiState.value.gameMode == LudoGameMode.HYBRID_ONLINE) 100L else 150L
                val botTriggerDelay = (baseBotTriggerDelay * getGameplaySpeedMultiplier()).toLong()
                delay(botTriggerDelay)
                rollDice()
            }
        }
    }

    private suspend fun makeBotMove(validMoves: List<Token>) {
        if (validMoves.isEmpty()) return
        
        // Bot Move Logic prioritization:
        // 1. Cut an opponent
        // 2. Reach home (57)
        // 3. Save a token in danger or release from base
        // 4. Closest to home (maximize progression)
        
        val state = _uiState.value
        val diceRoll = state.diceRoll ?: return
        val currentPlayer = getCurrentPlayer() ?: return

        // 1. Calculate local smart heuristic first as fallback
        val fallbackToken = validMoves.maxByOrNull { token ->
            val targetPos = if (token.position == 0) 1 else token.position + diceRoll
            var priority = 0

            // Priority 1: Reaching home
            if (targetPos == 57) {
                priority += 1000
            }

            // Priority 2: Cut opponent token
            val targetCoord = LudoCoordinates.getTokenCoordinates(token.playerId, token.id, targetPos)
            val isTargetSafe = LudoCoordinates.isCellSafe(targetCoord.first.toInt(), targetCoord.second.toInt())
            if (!isTargetSafe) {
                val canCut = state.tokens.any { oppToken ->
                    if (oppToken.playerId != token.playerId && oppToken.position in 1..51) {
                        val oppCoord = LudoCoordinates.getTokenCoordinates(oppToken.playerId, oppToken.id, oppToken.position)
                        oppCoord.first.toInt() == targetCoord.first.toInt() && oppCoord.second.toInt() == targetCoord.second.toInt()
                    } else false
                }
                if (canCut) priority += 500
            }

            // Priority 3: Releasing a token
            if (token.position == 0 && diceRoll == 6) {
                priority += 300
            }

            // Priority 4: Advance tokens closer to home
            priority += token.position // Prefer moving advanced tokens

            priority
        } ?: validMoves.random()

        var chosenToken = fallbackToken

        // 2. In VS_COMPUTER mode, let Gemini AI decide! In HYBRID_ONLINE, use high-speed simulated Firebase moves
        if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
            _uiState.update { 
                it.copy(statusMessage = "📶 Online player ${currentPlayer.name} is syncing move over Firebase...")
            }
            val onlineDelay = (300 * getGameplaySpeedMultiplier()).toLong()
            delay(onlineDelay) // realistic network latency simulation!
        } else if (state.gameMode == LudoGameMode.VS_COMPUTER) {
            val prefix = "🤖"
            _uiState.update { 
                it.copy(statusMessage = "$prefix Gemini AI is planning the best move for ${currentPlayer.name}...")
            }
            
            val gameStateSummary = buildGameStateSummary()
            try {
                val geminiChoiceId = GeminiChatService.getBotMoveChoice(
                    validMoves = validMoves,
                    gameStateSummary = gameStateSummary,
                    diceRoll = diceRoll,
                    botPlayerName = currentPlayer.name
                )
                
                val match = validMoves.firstOrNull { it.id == geminiChoiceId }
                if (match != null) {
                    chosenToken = match
                    _uiState.update { 
                        it.copy(statusMessage = "🧠 Gemini AI decided to move Token ${match.id + 1}!")
                    }
                } else {
                    _uiState.update { 
                        it.copy(statusMessage = "$prefix Gemini AI suggests moving Token ${fallbackToken.id + 1}!")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(statusMessage = "$prefix Gemini offline. Moving Token ${fallbackToken.id + 1}...")
                }
            }
            val computerDelay = (800 * getGameplaySpeedMultiplier()).toLong()
            delay(computerDelay)
        }

        moveToken(chosenToken, diceRoll)
    }

    fun getCurrentPlayer(): Player? {
        val state = _uiState.value
        return state.players.firstOrNull { it.id == state.currentPlayerIdx }
    }

    fun selectTheme(theme: LudoTheme) {
        val currentState = _uiState.value
        if (currentState.unlockedThemes.contains(theme)) {
            _uiState.update { it.copy(selectedTheme = theme) }
            sharedPrefs?.edit()?.putString("selected_theme", theme.name)?.apply()
        }
    }

    fun selectLanguage(language: LudoLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
        val state = _uiState.value
        if (state.gameMode == LudoGameMode.ONE_VS_ONE && state.coins < state.selectedWagerAmount) {
            val msg = LudoTranslations.getTranslation("not_enough_coins", language)
                .replace("%d", state.selectedWagerAmount.toString())
            _uiState.update { it.copy(statusMessage = msg) }
        }
    }

    fun triggerAd(type: AdType) {
        if (!isInternetAvailable()) {
            val language = uiState.value.selectedLanguage
            val title = LudoTranslations.getTranslation("internet_required_title", language)
            _uiState.update { it.copy(statusMessage = "❌ $title") }
            return
        }
        // Prepare countdown for backup simulated ad player
        _uiState.update { it.copy(adType = type, adSecondsLeft = 5, isRealAdShowing = false) }
        viewModelScope.launch {
            var seconds = 5
            while (seconds > 0) {
                delay(1000)
                // If the state was cleared or a real ad is showing, stop countdown
                val currentState = _uiState.value
                if (currentState.adType != type || currentState.isRealAdShowing) return@launch
                seconds--
                _uiState.update { it.copy(adSecondsLeft = seconds) }
            }
            // Ad completed successfully via simulated fallback!
            val currentState = _uiState.value
            if (currentState.adType == type && !currentState.isRealAdShowing) {
                onRealAdCompleted(type)
            }
        }
    }

    fun onRealAdStarted() {
        _uiState.update { it.copy(isRealAdShowing = true) }
    }

    fun onRealAdCompleted(type: AdType) {
        val currentType = _uiState.value.adType
        if (currentType != null && currentType != type) return
        viewModelScope.launch {
            _uiState.update { it.copy(adType = null, isRealAdShowing = false) }
            when (type) {
                AdType.GAME_START -> {
                    proceedWithStartGame()
                }
                AdType.GUARANTEED_SIX -> {
                    LudoAudioEngine.playTurnPass()
                    val currentCount = _uiState.value.guaranteedSixCount + 1
                    var cooldownRemaining = 0L
                    var finalCount = currentCount
                    
                    if (currentCount >= 2) {
                        cooldownRemaining = 120L // 2 minutes in seconds
                        finalCount = 2
                        sharedPrefs?.edit()?.apply {
                            putInt("guaranteed_six_count", 2)
                            putLong("guaranteed_six_cooldown_end_time", System.currentTimeMillis() + 120 * 1000L)
                            apply()
                        }
                    } else {
                        sharedPrefs?.edit()?.apply {
                            putInt("guaranteed_six_count", finalCount)
                            apply()
                        }
                    }
                    _uiState.update { currentState ->
                        currentState.copy(
                            nextRollIsSix = true,
                            guaranteedSixCount = finalCount,
                            guaranteedSixCooldownRemaining = cooldownRemaining,
                            statusMessage = "🎉 Next roll is guaranteed to be a 6!"
                        )
                    }
                }
                AdType.EXTEND_TIME -> {
                    LudoAudioEngine.playTurnPass()
                    _uiState.update { currentState ->
                        currentState.copy(
                            timeLeftSeconds = currentState.timeLeftSeconds + 300,
                            isTimeUpDialogShowing = false,
                            isTimeWarningDialogShowing = false,
                            statusMessage = "⏱ Match extended by 5 minutes!"
                        )
                    }
                }
                AdType.GAME_FINISH, AdType.RESET -> {
                    resetToSetup()
                }
                AdType.WATCH_AD -> {
                    LudoAudioEngine.playTurnPass()
                    addCoins(500)
                    val currentCount = _uiState.value.watchAdCount + 1
                    var cooldownRemaining = 0L
                    var finalCount = currentCount
                    
                    if (currentCount >= 2) {
                        cooldownRemaining = 1800L // 30 minutes in seconds
                        finalCount = 2
                        sharedPrefs?.edit()?.apply {
                            putInt("watch_ad_count", 2)
                            putLong("watch_ad_cooldown_end_time", System.currentTimeMillis() + 1800 * 1000L)
                            apply()
                        }
                    } else {
                        sharedPrefs?.edit()?.apply {
                            putInt("watch_ad_count", finalCount)
                            apply()
                        }
                    }
                    _uiState.update { currentState ->
                        currentState.copy(
                            watchAdCount = finalCount,
                            watchAdCooldownRemaining = cooldownRemaining,
                            statusMessage = "🎉 Ad watched! +500 Coins added!"
                        )
                    }
                }
            }
        }
    }

    fun dismissAd() {
        _uiState.update { it.copy(adType = null, isRealAdShowing = false) }
    }

    fun dismissTimeUpDialog() {
        _uiState.update { it.copy(isTimeUpDialogShowing = false) }
        resetToSetup()
    }

    fun dismissTimeWarningDialog() {
        _uiState.update { it.copy(isTimeWarningDialogShowing = false) }
    }

    private fun addChatMessage(senderName: String, senderColor: LudoColor?, text: String) {
        _uiState.update { state ->
            val updatedMessages = state.chatMessages + ChatMessage(senderName, senderColor, text)
            state.copy(chatMessages = updatedMessages)
        }
    }

    private fun buildGameStateSummary(): String {
        val state = _uiState.value
        val summary = StringBuilder()
        summary.append("Game Mode: ${state.gameMode.name}. ")
        summary.append("Current Player: ${state.players.firstOrNull { it.id == state.currentPlayerIdx }?.name ?: "Unknown"}. ")
        state.players.forEach { p ->
            val tokensAtHome = state.tokens.count { it.playerId == p.id && it.position == 57 }
            val tokensInYard = state.tokens.count { it.playerId == p.id && it.position == 0 }
            val activeTokens = state.tokens.filter { it.playerId == p.id && it.position in 1..56 }
            val positions = activeTokens.map { it.position }.joinToString(", ")
            summary.append("${p.name} (Color: ${p.color}, Type: ${p.type}): $tokensAtHome reached home, $tokensInYard in base yard, active token board positions: [$positions]. ")
        }
        return summary.toString()
    }

    fun triggerBotChat(event: String, actingPlayerId: Int, targetPlayerId: Int? = null, extraDetail: String = "") {
        val state = _uiState.value
        if (state.gameMode != LudoGameMode.VS_COMPUTER && state.gameMode != LudoGameMode.HYBRID_ONLINE) return

        val actingPlayer = state.players.firstOrNull { it.id == actingPlayerId } ?: return
        
        viewModelScope.launch {
            val speaker = when (event) {
                "START" -> state.players.filter { it.type == PlayerType.BOT }.randomOrNull()
                "CAPTURED" -> {
                    val capturer = state.players.firstOrNull { it.id == actingPlayerId }
                    val captured = targetPlayerId?.let { id -> state.players.firstOrNull { it.id == id } }
                    if (Random.nextFloat() < 0.6f) capturer else (captured ?: capturer)
                }
                "ROLL_6" -> actingPlayer
                "WINNER" -> actingPlayer
                else -> actingPlayer
            } ?: return@launch

            if (speaker.type != PlayerType.BOT) return@launch

            _uiState.update { it.copy(botTypingName = speaker.name) }
            delay(1000 + Random.nextLong(0, 1000))

            val gameStateSummary = buildGameStateSummary()
            val resultText = GeminiChatService.getReaction(event, speaker.name, gameStateSummary)
            
            val cleanedMessage = if (resultText.contains(":")) {
                resultText.substringAfter(":").replace("\"", "").trim()
            } else {
                resultText.replace("\"", "").trim()
            }

            addChatMessage(speaker.name, speaker.color, cleanedMessage)
            
            _uiState.update { currentState ->
                val newBubbles = currentState.activePlayerBubbles.toMutableMap()
                newBubbles[speaker.id] = cleanedMessage
                currentState.copy(
                    activePlayerBubbles = newBubbles,
                    botTypingName = null
                )
            }

            viewModelScope.launch {
                delay(5000)
                _uiState.update { currentState ->
                    val newBubbles = currentState.activePlayerBubbles.toMutableMap()
                    if (newBubbles[speaker.id] == cleanedMessage) {
                        newBubbles.remove(speaker.id)
                    }
                    currentState.copy(activePlayerBubbles = newBubbles)
                }
            }
        }
    }

    fun sendUserChatMessage(message: String) {
        if (message.isBlank()) return
        val state = _uiState.value
        val userPlayer = state.players.firstOrNull { it.type == PlayerType.HUMAN } ?: return
        
        addChatMessage(userPlayer.name, userPlayer.color, message)

        // Show the user's message as a bubble over the user's avatar
        _uiState.update { currentState ->
            val newBubbles = currentState.activePlayerBubbles.toMutableMap()
            newBubbles[userPlayer.id] = message
            currentState.copy(activePlayerBubbles = newBubbles)
        }

        // Auto-clear user bubble after 5 seconds
        viewModelScope.launch {
            delay(5000)
            _uiState.update { currentState ->
                val newBubbles = currentState.activePlayerBubbles.toMutableMap()
                if (newBubbles[userPlayer.id] == message) {
                    newBubbles.remove(userPlayer.id)
                }
                currentState.copy(activePlayerBubbles = newBubbles)
            }
        }

        val activeBots = state.players.filter { it.type == PlayerType.BOT }.map { it.name }
        if (activeBots.isEmpty()) return

        viewModelScope.launch {
            val typingBot = activeBots.random()
            _uiState.update { it.copy(botTypingName = typingBot) }
            delay(1500 + Random.nextLong(0, 1000))

            val userColorName = userPlayer.color.name
            val summary = buildGameStateSummary()
            val replyText = GeminiChatService.getReplyToUser(message, userColorName, activeBots, summary)

            val senderName = if (replyText.contains(":")) {
                replyText.substringBefore(":").trim()
            } else {
                typingBot
            }
            val cleanedMessage = if (replyText.contains(":")) {
                replyText.substringAfter(":").replace("\"", "").trim()
            } else {
                replyText.replace("\"", "").trim()
            }

            val speaker = state.players.firstOrNull { it.name == senderName } ?: state.players.first { it.name == typingBot }
            addChatMessage(speaker.name, speaker.color, cleanedMessage)

            _uiState.update { currentState ->
                val newBubbles = currentState.activePlayerBubbles.toMutableMap()
                newBubbles[speaker.id] = cleanedMessage
                currentState.copy(
                    activePlayerBubbles = newBubbles,
                    botTypingName = null
                )
            }

            viewModelScope.launch {
                delay(5000)
                _uiState.update { currentState ->
                    val newBubbles = currentState.activePlayerBubbles.toMutableMap()
                    if (newBubbles[speaker.id] == cleanedMessage) {
                        newBubbles.remove(speaker.id)
                    }
                    currentState.copy(activePlayerBubbles = newBubbles)
                }
            }
        }
    }
}
