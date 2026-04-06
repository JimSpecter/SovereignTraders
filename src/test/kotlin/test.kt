fun main() {
    val placeholder = "%listing%"
    val cleanKey = placeholder.trim('%', '<', '>')

    var body = "<green>SUCCESSFULLY ACQUIRED %listing% FOR %amount%."
    val replacements = arrayOf("%listing%" to "<gold>Excalibur", "%amount%" to "500")

    for ((p, v) in replacements) {
        val ck = p.trim('%', '<', '>')
        body = body.replace("%$ck%", "<$ck>")
    }

}
