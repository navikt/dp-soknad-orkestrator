package no.nav.dagpenger.soknad.orkestrator.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HtmlTest {
    @Test
    fun `Html lukker ufullstendige self-closing tags`() {
        val input = "<div><br><img src='image.png'><input type='text'><hr></div>"
        val forventet = "<div><br /><img src='image.png' /><input type='text' /><hr /></div>"

        val html = Html(input)

        html.verdi shouldBe forventet
    }

    @Test
    fun `Html fjerner non-breaking spaces`() {
        val input = "<div>Hello&nbsp;World&nbsp;!</div>"
        val forventet = "<div>Hello World !</div>"

        val html = Html(input)

        html.verdi shouldBe forventet
    }

    @Test
    fun `leggTilCss legger til CSS i head`() {
        val input = "<html><head><title>Test</title></head><body><h1>Hello World</h1></body></html>"
        val css = "body { background-color: #f0f0f0; }"
        val forventet =
            "<html><head><style>body { background-color: #f0f0f0; }</style><title>Test</title></head><body><h1>Hello World</h1></body></html>"

        val html = Html(input).leggTilCss(css)

        html.verdi shouldBe forventet
    }

    @Test
    fun `leggTilCss legger til head og CSS hvis head mangler`() {
        val input = "<html><body><h1>Hello World</h1></body></html>"
        val css = "body { background-color: #f0f0f0; }"
        val forventet =
            "<html><head><style>body { background-color: #f0f0f0; }</style></head><body><h1>Hello World</h1></body></html>"

        val html = Html(input).leggTilCss(css)

        html.verdi shouldBe forventet
    }

    @Test
    fun `leggTilCss legger til head og CSS hvis html og head mangler`() {
        val input = "<body><h1>Hello World</h1></body>"
        val css = "body { background-color: #f0f0f0; }"
        val forventet =
            "<head><style>body { background-color: #f0f0f0; }</style></head><body><h1>Hello World</h1></body>"

        val html = Html(input).leggTilCss(css)

        html.verdi shouldBe forventet
    }
}
