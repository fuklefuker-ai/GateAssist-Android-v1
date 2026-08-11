package com.gateassist.app

data class Flight(
    val airline: String,
    val number: String,
    val destination: String,
    val gate: String,
    val departure: String,
    val close: String
)

enum class AnnouncementType(val label: String) {
    BOARDING("Start boarding"),
    FINAL("Final call"),
    PAGING("Passenger paging"),
    DELAY("Flight delay"),
    GATE_CHANGE("Gate change"),
    VOLUNTEER("Volunteer request")
}

data class AnnouncementOutput(
    val english: String,
    val japanese: String,
    val cantonese: String,
    val warnings: List<String>
)

object AnnouncementEngine {
    // Demo data only. Replace with an authorized flight-data source for operational use.
    val flights = listOf(
        Flight("HK Express", "UO871", "Hong Kong", "92", "17:30", "17:10"),
        Flight("HK Express", "UO849", "Hong Kong", "88", "15:45", "15:25"),
        Flight("Japan Airlines", "JL003", "New York", "61", "18:30", "18:10"),
        Flight("Japan Airlines", "JL729", "Jakarta", "64", "17:50", "17:30")
    )

    fun generate(type: AnnouncementType, flight: Flight, gate: String, time: String, extra: String): AnnouncementOutput {
        val warnings = mutableListOf<String>()
        if (gate.isBlank()) warnings += "Gate is missing."
        if (type == AnnouncementType.FINAL && time.isBlank()) warnings += "Boarding close time is missing."
        if (type == AnnouncementType.PAGING && extra.isBlank()) warnings += "Passenger name is missing."
        if (type == AnnouncementType.DELAY && time.isBlank()) warnings += "Updated departure time is missing."
        if (type == AnnouncementType.GATE_CHANGE && extra.isBlank()) warnings += "New gate is missing."
        if (type == AnnouncementType.VOLUNTEER && extra.isBlank()) warnings += "Alternative flight/details are missing."

        val en = when (type) {
            AnnouncementType.BOARDING -> "${flight.airline} is now boarding flight ${flight.number} to ${flight.destination} at Gate $gate. Please have your boarding pass and travel documents ready."
            AnnouncementType.FINAL -> "This is the final boarding call for ${flight.airline} flight ${flight.number} to ${flight.destination}. All remaining passengers are requested to proceed immediately to Gate $gate. Boarding closes at $time."
            AnnouncementType.PAGING -> "Passenger $extra, travelling on ${flight.airline} flight ${flight.number} to ${flight.destination}, please proceed to Gate $gate and contact the gate staff."
            AnnouncementType.DELAY -> "${flight.airline} advises passengers travelling on flight ${flight.number} to ${flight.destination} that departure is delayed from ${flight.departure} to $time. Please remain near Gate $gate for further information."
            AnnouncementType.GATE_CHANGE -> "${flight.airline} advises passengers travelling on flight ${flight.number} to ${flight.destination} that the boarding gate has changed from Gate $gate to Gate $extra. Please proceed to Gate $extra."
            AnnouncementType.VOLUNTEER -> "${flight.airline} is seeking volunteers travelling on flight ${flight.number} to ${flight.destination} who may be willing to travel on $extra. Please contact the staff at Gate $gate if you are interested."
        }

        val ja = when (type) {
            AnnouncementType.BOARDING -> "${flight.airline} ${flight.number}便、${flight.destination}行きは、ただいま${gate}番ゲートより搭乗を開始しております。搭乗券と渡航書類をご準備ください。"
            AnnouncementType.FINAL -> "${flight.airline} ${flight.number}便、${flight.destination}行きの最終搭乗案内です。まだご搭乗でないお客様は、ただちに${gate}番ゲートまでお越しください。搭乗口は${time}に締め切ります。"
            AnnouncementType.PAGING -> "${flight.airline} ${flight.number}便、${flight.destination}行きにご搭乗の${extra}様、${gate}番ゲートの係員までお越しください。"
            AnnouncementType.DELAY -> "${flight.airline} ${flight.number}便、${flight.destination}行きのお客様へご案内いたします。出発時刻は${flight.departure}から${time}へ変更となりました。${gate}番ゲート付近でお待ちください。"
            AnnouncementType.GATE_CHANGE -> "${flight.airline} ${flight.number}便、${flight.destination}行きのお客様へご案内いたします。搭乗ゲートは${gate}番から${extra}番へ変更となりました。${extra}番ゲートへお進みください。"
            AnnouncementType.VOLUNTEER -> "${flight.airline} ${flight.number}便、${flight.destination}行きのお客様へお願いがございます。${extra}への変更にご協力いただけるお客様は、${gate}番ゲートの係員までお申し出ください。"
        }

        val yue = when (type) {
            AnnouncementType.BOARDING -> "${flight.airline} 航班 ${flight.number} 前往 ${flight.destination} 現正於 ${gate} 號閘口登機。請準備登機證及旅行證件。"
            AnnouncementType.FINAL -> "最後登機廣播。乘搭 ${flight.airline} 航班 ${flight.number} 前往 ${flight.destination} 的旅客，請立即前往 ${gate} 號閘口。登機閘口將於 ${time} 關閉。"
            AnnouncementType.PAGING -> "乘搭 ${flight.airline} 航班 ${flight.number} 前往 ${flight.destination} 的旅客 ${extra}，請前往 ${gate} 號閘口與職員聯絡。"
            AnnouncementType.DELAY -> "${flight.airline} 航班 ${flight.number} 前往 ${flight.destination} 的旅客請注意，航班起飛時間由 ${flight.departure} 延至 ${time}。請留在 ${gate} 號閘口附近等候進一步消息。"
            AnnouncementType.GATE_CHANGE -> "${flight.airline} 航班 ${flight.number} 前往 ${flight.destination} 的旅客請注意，登機閘口已由 ${gate} 號更改為 ${extra} 號。請前往 ${extra} 號閘口。"
            AnnouncementType.VOLUNTEER -> "${flight.airline} 現正尋找乘搭航班 ${flight.number} 前往 ${flight.destination} 的自願旅客，改乘 ${extra}。如有興趣，請聯絡 ${gate} 號閘口職員。"
        }

        return AnnouncementOutput(en, ja, yue, warnings)
    }

    fun speechSafeEnglish(text: String): String {
        var out = text
        Regex("\\b([A-Z]{2})(\\d{2,4})\\b").findAll(out).toList().asReversed().forEach { m ->
            val letters = m.groupValues[1].toCharArray().joinToString(" ")
            val digits = m.groupValues[2].map { digitWord(it) }.joinToString(" ")
            out = out.replaceRange(m.range, "$letters $digits")
        }
        Regex("Gate (\\d{1,3})").findAll(out).toList().asReversed().forEach { m ->
            out = out.replaceRange(m.range, "Gate ${numberToWords(m.groupValues[1].toInt())}")
        }
        return out
    }

    private fun digitWord(c: Char) = mapOf(
        '0' to "zero", '1' to "one", '2' to "two", '3' to "three", '4' to "four",
        '5' to "five", '6' to "six", '7' to "seven", '8' to "eight", '9' to "nine"
    )[c] ?: c.toString()

    private fun numberToWords(n: Int): String {
        val under20 = listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
        val tens = listOf("","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety")
        return when {
            n < 20 -> under20[n]
            n < 100 -> tens[n / 10] + if (n % 10 == 0) "" else " ${under20[n % 10]}"
            else -> n.toString()
        }
    }
}
