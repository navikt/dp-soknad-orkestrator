package no.nav.dagpenger.soknad.orkestrator.utils

class Html(
    verdi: String,
) {
    val verdi: String = vaskHtml(verdi)

    private fun vaskHtml(html: String): String =
        html
            .let { lukkSelfClosingTags(it) }
            .let { fjernNonBreakingSpace(it) }

    private fun lukkSelfClosingTags(html: String): String {
        val selfClosingTags = listOf("br", "img", "input", "hr", "meta", "link")
        val selfClosingTagMønster = Regex("<(${selfClosingTags.joinToString("|")})([^>/]*)(?<!/)>", RegexOption.IGNORE_CASE)

        return html.replace(selfClosingTagMønster) { matchResult ->
            "<${matchResult.groupValues[1]}${matchResult.groupValues[2]} />"
        }
    }

    private fun fjernNonBreakingSpace(html: String): String = html.replace(Regex("&nbsp;"), " ")

    fun leggTilCss(css: String): Html {
        val styleTag = "<style>$css</style>"

        val ferdigHtml =
            when {
                verdi.contains("<head>", ignoreCase = true) ->
                    verdi.replaceFirst("<head>", "<head>$styleTag")
                verdi.contains("<html>", ignoreCase = true) ->
                    verdi.replaceFirst("<html>", "<html><head>$styleTag</head>")
                else ->
                    "<head>$styleTag</head>$verdi"
            }

        return Html(ferdigHtml)
    }
}
