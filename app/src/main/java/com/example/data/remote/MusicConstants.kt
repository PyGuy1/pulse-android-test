package com.example.data.remote

import com.example.data.model.MoodGenre
import java.util.Locale
import java.util.TimeZone

object MusicConstants {
    val MOODS = listOf(
        MoodGenre("Chill", "chill", 0xFF2F5F9E),
        MoodGenre("Workout", "workout", 0xFF7A3FB0),
        MoodGenre("Focus", "focus", 0xFF2C4FB8),
        MoodGenre("Party", "party hits", 0xFFA03FA8),
        MoodGenre("Sad", "sad songs", 0xFF3B4A86),
        MoodGenre("Romance", "romantic songs", 0xFF8A3F8F),
        MoodGenre("Throwback", "90s hits", 0xFF4A5AB0),
        MoodGenre("Hip-Hop", "hip hop", 0xFF5A3F9A),
        MoodGenre("Rock", "rock classics", 0xFF33447A),
        MoodGenre("Lo-fi", "lofi", 0xFF2A6A8E),
        MoodGenre("Latin", "latin hits", 0xFF7B4BC0),
        MoodGenre("K-pop", "kpop", 0xFF9A45B8),
        MoodGenre("Acoustic", "acoustic", 0xFF3A6AB0),
        MoodGenre("Jazz", "jazz", 0xFF27508A)
    )

    val FAMOUS_ARTISTS = listOf(
        "Taylor Swift", "Drake", "Bad Bunny", "The Weeknd", "Ed Sheeran",
        "Billie Eilish", "Dua Lipa", "Ariana Grande", "Bruno Mars", "Coldplay",
        "Eminem", "Rihanna", "BTS", "Arijit Singh", "Shakira", "Adele",
        "Post Malone", "Kendrick Lamar", "Justin Bieber", "Beyoncé", "SZA",
        "Olivia Rodrigo", "Harry Styles", "Imagine Dragons"
    )

    val ALL_COUNTRIES = listOf(
        "WW", "US", "GB", "CA", "AU", "IN", "DE", "FR", "ES", "IT", "BR", "MX", "AR", "CO", "CL",
        "PE", "VE", "EC", "UY", "JP", "KR", "CN", "HK", "TW", "NL", "BE", "SE", "NO", "DK", "FI",
        "IS", "PL", "CZ", "SK", "HU", "RO", "BG", "GR", "PT", "IE", "AT", "CH", "UA", "RU", "TR",
        "IL", "SA", "AE", "QA", "KW", "JO", "LB", "EG", "MA", "DZ", "TN", "NG", "GH", "KE", "ET",
        "TZ", "UG", "ZA", "PK", "BD", "LK", "NP", "ID", "MY", "SG", "TH", "VN", "PH", "MM", "KH", "NZ"
    )

    private val LANGS_BY_COUNTRY = mapOf(
        "IN" to listOf("Hindi", "Tamil", "Telugu", "Malayalam", "Kannada", "Punjabi", "Bengali", "Marathi", "Gujarati", "Bhojpuri", "Odia", "Urdu"),
        "PK" to listOf("Urdu", "Punjabi", "Pashto", "Sindhi", "Balochi"),
        "BD" to listOf("Bengali", "Bangla Band"),
        "LK" to listOf("Sinhala", "Tamil"),
        "NP" to listOf("Nepali"),
        "ID" to listOf("Indonesian", "Javanese", "Sundanese"),
        "MY" to listOf("Malay", "Mandarin", "Tamil"),
        "SG" to listOf("English", "Mandarin", "Malay", "Tamil"),
        "PH" to listOf("Tagalog", "OPM", "Cebuano"),
        "TH" to listOf("Thai", "Isan"),
        "VN" to listOf("Vietnamese"),
        "MM" to listOf("Burmese"),
        "KH" to listOf("Khmer"),
        "JP" to listOf("J-Pop", "Anime", "City Pop", "Enka"),
        "KR" to listOf("K-Pop", "K-Hip-Hop", "K-Drama OST", "Trot"),
        "CN" to listOf("Mandarin", "C-Pop", "Cantonese"),
        "TW" to listOf("Mandarin", "Taiwanese", "Hokkien"),
        "HK" to listOf("Cantonese", "Mandarin", "Canto-pop"),
        "US" to listOf("Pop", "Hip-Hop", "Country", "R&B", "Rock", "Latin", "Spanish"),
        "CA" to listOf("English", "French", "Quebec"),
        "GB" to listOf("British Pop", "UK Rap", "Grime", "Indie"),
        "IE" to listOf("Irish", "Pop"),
        "AU" to listOf("Aussie Pop", "Australian Rock"),
        "NZ" to listOf("Kiwi Pop", "Maori"),
        "DE" to listOf("German", "Deutschrap", "Schlager"),
        "AT" to listOf("German", "Austropop"),
        "CH" to listOf("German", "French", "Swiss"),
        "FR" to listOf("French", "Rap Français", "Chanson"),
        "BE" to listOf("Dutch", "French"),
        "NL" to listOf("Dutch", "Nederlandstalig"),
        "ES" to listOf("Spanish", "Catalan", "Flamenco", "Galician"),
        "PT" to listOf("Portuguese", "Fado"),
        "IT" to listOf("Italian", "Italian Rap"),
        "GR" to listOf("Greek", "Laika"),
        "TR" to listOf("Turkish", "Turkish Rap", "Arabesk"),
        "SE" to listOf("Swedish"),
        "NO" to listOf("Norwegian"),
        "DK" to listOf("Danish"),
        "FI" to listOf("Finnish"),
        "IS" to listOf("Icelandic"),
        "PL" to listOf("Polish", "Polski Rap"),
        "CZ" to listOf("Czech"),
        "SK" to listOf("Slovak"),
        "HU" to listOf("Hungarian"),
        "RO" to listOf("Romanian", "Manele"),
        "BG" to listOf("Bulgarian", "Chalga"),
        "UA" to listOf("Ukrainian"),
        "RU" to listOf("Russian", "Russian Rap"),
        "IL" to listOf("Hebrew", "Mizrahi"),
        "SA" to listOf("Arabic", "Khaleeji"),
        "AE" to listOf("Arabic", "Khaleeji"),
        "QA" to listOf("Arabic", "Khaleeji"),
        "KW" to listOf("Arabic", "Khaleeji"),
        "JO" to listOf("Arabic"),
        "LB" to listOf("Arabic", "Lebanese"),
        "EG" to listOf("Arabic", "Egyptian", "Mahraganat"),
        "MA" to listOf("Arabic", "Maghrebi", "French"),
        "DZ" to listOf("Arabic", "Rai", "French"),
        "TN" to listOf("Arabic", "Tunisian"),
        "NG" to listOf("Afrobeats", "Yoruba", "Igbo", "Naija Hip-Hop"),
        "GH" to listOf("Afrobeats", "Highlife", "Twi"),
        "KE" to listOf("Swahili", "Gengetone"),
        "TZ" to listOf("Bongo Flava", "Swahili"),
        "UG" to listOf("Luganda", "Ugandan"),
        "ET" to listOf("Amharic", "Oromo"),
        "ZA" to listOf("Amapiano", "Afrikaans", "Zulu", "Gqom"),
        "MX" to listOf("Spanish", "Regional Mexican", "Corridos"),
        "AR" to listOf("Spanish", "Argentine Rock", "Cumbia", "Trap Argentino"),
        "CO" to listOf("Spanish", "Reggaeton", "Vallenato"),
        "CL" to listOf("Spanish", "Chilean"),
        "PE" to listOf("Spanish", "Peruvian Cumbia"),
        "VE" to listOf("Spanish", "Salsa"),
        "EC" to listOf("Spanish", "Ecuadorian"),
        "UY" to listOf("Spanish", "Candombe"),
        "BR" to listOf("Portuguese", "Sertanejo", "Funk Brasileiro", "Pagode", "MPB"),
        "WW" to listOf("English", "Spanish", "Hindi", "Korean", "Japanese", "French", "Portuguese", "Arabic", "Turkish", "German", "Italian", "Russian")
    )

    private val TIMEZONE_TO_COUNTRY = mapOf(
        "Asia/Kolkata" to "IN", "Asia/Calcutta" to "IN", "Asia/Karachi" to "PK",
        "Asia/Dhaka" to "BD", "Asia/Colombo" to "LK", "Asia/Kathmandu" to "NP",
        "Asia/Dubai" to "AE", "Asia/Riyadh" to "SA", "Asia/Qatar" to "QA",
        "Asia/Kuwait" to "KW", "Asia/Baghdad" to "IQ", "Asia/Tehran" to "IR",
        "Asia/Jerusalem" to "IL", "Asia/Beirut" to "LB", "Asia/Amman" to "JO",
        "Asia/Istanbul" to "TR", "Europe/Istanbul" to "TR", "Asia/Tokyo" to "JP",
        "Asia/Seoul" to "KR", "Asia/Shanghai" to "CN", "Asia/Hong_Kong" to "HK",
        "Asia/Taipei" to "TW", "Asia/Singapore" to "SG", "Asia/Kuala_Lumpur" to "MY",
        "Asia/Jakarta" to "ID", "Asia/Makassar" to "ID", "Asia/Jayapura" to "ID",
        "Asia/Manila" to "PH", "Asia/Bangkok" to "TH", "Asia/Ho_Chi_Minh" to "VN",
        "Asia/Saigon" to "VN", "Asia/Yangon" to "MM", "Asia/Phnom_Penh" to "KH",
        "Asia/Tashkent" to "UZ", "Asia/Almaty" to "KZ", "Asia/Kabul" to "AF",
        "Europe/London" to "GB", "Europe/Dublin" to "IE", "Europe/Paris" to "FR",
        "Europe/Berlin" to "DE", "Europe/Madrid" to "ES", "Europe/Rome" to "IT",
        "Europe/Amsterdam" to "NL", "Europe/Brussels" to "BE", "Europe/Zurich" to "CH",
        "Europe/Vienna" to "AT", "Europe/Stockholm" to "SE", "Europe/Oslo" to "NO",
        "Europe/Copenhagen" to "DK", "Europe/Helsinki" to "FI", "Europe/Warsaw" to "PL",
        "Europe/Prague" to "CZ", "Europe/Budapest" to "HU", "Europe/Bucharest" to "RO",
        "Europe/Athens" to "GR", "Europe/Lisbon" to "PT", "Europe/Kiev" to "UA",
        "Europe/Kyiv" to "UA", "Europe/Moscow" to "RU", "Africa/Lagos" to "NG",
        "Africa/Cairo" to "EG", "Africa/Johannesburg" to "ZA", "Africa/Nairobi" to "KE",
        "Africa/Accra" to "GH", "Africa/Casablanca" to "MA", "Africa/Algiers" to "DZ",
        "Africa/Addis_Ababa" to "ET", "America/New_York" to "US", "America/Chicago" to "US",
        "America/Denver" to "US", "America/Los_Angeles" to "US", "America/Phoenix" to "US",
        "America/Anchorage" to "US", "Pacific/Honolulu" to "US", "America/Detroit" to "US",
        "America/Toronto" to "CA", "America/Vancouver" to "CA", "America/Edmonton" to "CA",
        "America/Winnipeg" to "CA", "America/Halifax" to "CA", "America/Mexico_City" to "MX",
        "America/Bogota" to "CO", "America/Lima" to "PE", "America/Santiago" to "CL",
        "America/Argentina/Buenos_Aires" to "AR", "America/Buenos_Aires" to "AR",
        "America/Sao_Paulo" to "BR", "America/Caracas" to "VE", "America/Guayaquil" to "EC",
        "America/Montevideo" to "UY", "America/Panama" to "PA", "America/Jamaica" to "JM",
        "Australia/Sydney" to "AU", "Australia/Melbourne" to "AU", "Australia/Brisbane" to "AU",
        "Australia/Perth" to "AU", "Australia/Adelaide" to "AU", "Pacific/Auckland" to "NZ"
    )

    fun languagesFor(code: String): List<String> = LANGS_BY_COUNTRY[code] ?: LANGS_BY_COUNTRY["WW"]!!

    fun countryName(code: String): String {
        if (code == "WW") return "Worldwide"
        return try {
            Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code }
        } catch (_: Exception) {
            code
        }
    }

    fun detectDeviceCountryCode(): String {
        val tz = TimeZone.getDefault().id
        TIMEZONE_TO_COUNTRY[tz]?.let { return it }
        val localeCountry = Locale.getDefault().country.uppercase(Locale.US)
        if (localeCountry.length == 2) return localeCountry
        return "US"
    }
}
