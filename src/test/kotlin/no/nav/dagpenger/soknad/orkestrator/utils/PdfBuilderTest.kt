package no.nav.dagpenger.soknad.orkestrator.utils

import org.junit.jupiter.api.Test

class PdfBuilderTest {
    @Test
    fun `Generate pdf based on html`() {
        val pdf = genererPdfFraHtml(inntektSkjemaHtml)

        assert(pdf.isNotEmpty())
    }

    @Test
    fun `Lukk self closing tags og fjern nbsp fra html-strengen`() {
        // language=HTML
        val forventetHtml =
            """
            <p>Hei, verden<br /></p>
            <img src='nav.no' />
            """.trimIndent()

        val cleanedHtml = vaskHtml(dårligHtml)

        assert(cleanedHtml == forventetHtml)
    }
}

// language=HTML
val dårligHtml =
    """
    <p>Hei,&nbsp;verden<br></p>
    <img src='nav.no'>
    """.trimIndent()

// language=HTML
val inntektSkjemaHtml =
    """
        <!DOCTYPE html>
    <html>
    <head><title>Brukerdialog - Din inntekt</title>
        <style>body {
            font-family: Arial, Helvetica, sans-serif;
            max-width: 760px;
            margin: 2rem auto;
    }

     }

        .mt-4 {
            margin-top: calc(4rem / }


        }

        .mt-14 {
            margin-top: calc(14rem / 3)

        }

        .tag--pdf {
            color: white;
            padding: 0.4rem 0.5rem;
            background-color: #525962;
            border-radius: 4px

        }

        .readmore--pdf > button {
            border: 0;
            padding: 0;
            background-color: transparent;
            color: #0067c5;
            font-size: 1rem;
            margin-top: 1rem;
            margin-bottom: 1rem

        }

        .readmore--pdf > div {
            padding-left: 1rem;
            border-left: 2px solid #071a3636;
            margin-bottom: 1rem

        }

        .navds-error-message {
            display: none

        }

        textarea {
            resize: none;
            width: 97.5%;
            display: block;
            margin-top: 0.5rem;
            padding: 0.4rem;
            font-size: 1rem;
            font-family: Arial, Helvetica, sans-se;
            border-radius: var(--pdf-border-radius-4)

        }

        .card {
            padding: 2rem;
            background-color: #eceef0;
            margin-bottom: 2rem;
            border-radius: 15px

        }

        fieldset {
            border: 0;
            padding: 0.5rem 0

        }

        fieldset > legend,
        .navds-label {
            font-weight: bold

        }

        .sendinn-button--pdf {
            background-color: #0067c5;
            color: #ffffff;
            padding: 0.5rem 1rem;
            border: 0;
            border-radius: 3px;
            font-size: 1.2rem;
            font-weight: bold

        }

        .expansion-card--pdf {
            background-color: #e0d8e9;
            border-radius: 15px;
            padding: 2rem

        }

        .expansion-card--pdf button {
            display: none

        }

        .iframe--container {
            display: none;
        }
        </style>
    </head>
    <body>
    <div class="brukerdialog" id="brukerdialog"><h1 id="header-icon" class="navds-heading navds-heading--large">Din
        inntekt</h1>
        <p class="mt-4 navds-body-long navds-body-long--medium navds-typo--spacing">Vi trenger at du sjekker
            innteksopplysningene vi har hentet om deg.</p>
        <div class="card"><span
                class="navds-tag _tag_1r3cn_1 tag--pdf navds-tag--neutral-filled navds-tag--medium navds-body-short navds-body-short--medium">Hentet fra Skatteetaten</span>
            <h2 class="navds-heading navds-heading--medium navds-typo--spacing">Inntekt</h2>
            <p class="navds-body-long navds-body-long--medium navds-typo--spacing">Inntekt siste 12 måneder fra <!-- -->17.
                desember 2023<!-- --> til <!-- -->17. desember 2024<!-- --> <br><strong>100000<!-- --> kroner</strong></p>
            <p class="navds-body-long navds-body-long--medium navds-typo--spacing">Inntekt siste 36 måneder fra <!-- -->17.
                desember 2021<!-- --> til <!-- -->17. desember 2024<!-- --> <br><strong>200000<!-- --> kroner</strong>.</p>
            <div class="_verticalLine_1r3cn_5" aria-hidden="true"></div>
            <div class="navds-read-more navds-read-more--medium readmore--pdf">
                <button type="button" class="navds-read-more__button navds-body-short" aria-expanded="false"
                        data-state="closed">
                    <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none" viewBox="0 0 24 24"
                         focusable="false" role="img" class="navds-read-more__expand-icon" aria-hidden="true">
                        <path fill="currentColor" fill-rule="evenodd"
                              d="M5.97 9.47a.75.75 0 0 1 1.06 0L12 14.44l4.97-4.97a.75.75 0 1 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06 0l-5.5-5.5a.75.75 0 0 1 0-1.06"
                              clip-rule="evenodd"></path>
                    </svg>
                    <span>Hvilke inntekter gir rett til dagpenger?</span></button>
                <div aria-hidden="true" data-state="closed"
                     class="navds-read-more__content navds-read-more__content--closed navds-body-long navds-body-long--medium">
                    <p>Vi bruker <strong>disse inntektene</strong> for å finne ut om du har rett til dagpenger:</p>
                    <ul>
                        <li>Arbeidsinntekt</li>
                        <li>Foreldrepenger som arbeidstaker</li>
                        <li>Svangerskapspenger som arbeidstaker</li>
                        <li>Svangerskapsrelaterte sykepenger som arbeidstaker</li>
                    </ul>
                    <p><strong>Inntekt som selvstendig næringsdrivende</strong> regnes ikke som arbeidsinntekt.</p>
                    <p>Vi har hentet arbeidsinntekten din fra Skatteetaten de siste 12 månedene og 36 månedene. NAV velger
                        det alternativet som er best for deg når vi vurderer om du har rett til dagpenger.</p></div>
            </div>
        </div>
        <form method="post" action="/arbeid/dagpenger/brukerdialog/13eaa299-ad5c-432c-a207-2c796274d309?index" id=":R275:"
              data-discover="true">
            <div>
                <div class="card">
                    <fieldset class="navds-radio-group navds-radio-group--medium navds-fieldset navds-fieldset--medium">
                        <legend class="navds-fieldset__legend navds-label">Stemmer den samlede inntekten?</legend>
                        <div class="navds-radio-buttons">
                            <div class="navds-radio navds-radio--medium"><input id="radio-Rnie75" name="inntektStemmer"
                                                                                type="radio" aria-labelledby="Rnie75H1"
                                                                                class="navds-radio__input"
                                                                                value="true"><label for="radio-Rnie75"
                                                                                                    class="navds-radio__label"><span
                                    class="navds-radio__content"><span id="Rnie75H1" aria-hidden="true"
                                                                       class="navds-body-short navds-body-short--medium">Ja</span></span></label>
                            </div>
                            <div class="navds-radio navds-radio--medium"><input id="radio-R17ie75" name="inntektStemmer"
                                                                                type="radio" aria-labelledby="R17ie75H1"
                                                                                class="navds-radio__input" value="false"
                                                                                checked="checked"><label for="radio-R17ie75"
                                                                                                         class="navds-radio__label"><span
                                    class="navds-radio__content"><span id="R17ie75H1" aria-hidden="true"
                                                                       class="navds-body-short navds-body-short--medium">Nei</span></span></label>
                            </div>
                        </div>
                        <div id="fieldset-error-Rie75" aria-relevant="additions removals" aria-live="polite"
                             class="navds-fieldset__error"></div>
                    </fieldset>
                    <div class="navds-read-more navds-read-more--medium readmore--pdf">
                        <button type="button" class="navds-read-more__button navds-body-short" aria-expanded="false"
                                data-state="closed">
                            <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none" viewBox="0 0 24 24"
                                 focusable="false" role="img" class="navds-read-more__expand-icon" aria-hidden="true">
                                <path fill="currentColor" fill-rule="evenodd"
                                      d="M5.97 9.47a.75.75 0 0 1 1.06 0L12 14.44l4.97-4.97a.75.75 0 1 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06 0l-5.5-5.5a.75.75 0 0 1 0-1.06"
                                      clip-rule="evenodd"></path>
                            </svg>
                            <span>Hva gjør du hvis inntekten din ikke stemmer?</span></button>
                        <div aria-hidden="true" data-state="closed"
                             class="navds-read-more__content navds-read-more__content--closed navds-body-long navds-body-long--medium">
                            Hvis opplysningene om inntekten din ikke stemmer, må du ta kontakt med arbeidsgiveren din.
                            Arbeidsgiver sender hver måned opplysninger om inntekten din til Skatteetaten. Det er bare
                            arbeidsgiver, som har rapportert inntektsopplysningene, som kan gjøre endringer og rette
                            opplysningene.<br> <br>Har du alternativ dokumentasjon som kan bekrefte at du har tjent mer, det
                            kan for eksempel være lønnslipper eller årsoppgaven, last det opp her.
                        </div>
                    </div>
                    <div class="mt-4 navds-form-field navds-form-field--medium"><label for="textarea-r2"
                                                                                       class="navds-form-field__label navds-label">Hva
                        er feil med inntekten?</label>
                        <div id="textarea-description-r2"
                             class="navds-form-field__description navds-body-short navds-body-short--medium">Beskriv hva som
                            er feil med inntekten din.
                        </div>
                        <textarea rows="3" name="begrunnelse" id="textarea-r2" aria-describedby="textarea-description-r2"
                                  class="navds-textarea__input navds-body-short navds-body-short--medium"
                                  style="--__ac-textarea-height: 90px; --__axc-textarea-height: 90px;">for pdf generering
    </textarea><textarea aria-hidden="true" class="navds-textarea__input navds-body-short navds-body-short--medium"
                         readonly="" tabindex="-1"
                         style="visibility: hidden; position: absolute; overflow: hidden; height: 0px; top: 0px; left: 0px; transform: translateZ(0px); width: 672px;">x</textarea>
                        <div class="navds-form-field__error" id="textarea-error-r2" aria-relevant="additions removals"
                             aria-live="polite"></div>
                    </div>
                    <fieldset
                            class="mt-4 navds-radio-group navds-radio-group--medium navds-fieldset navds-fieldset--medium">
                        <legend class="navds-fieldset__legend navds-label">Ønsker du å laste opp dokumentasjon?</legend>
                        <div class="navds-radio-buttons">
                            <div class="navds-radio navds-radio--medium"><input id="radio-rb" name="vilSendeDokumentasjon"
                                                                                type="radio" aria-labelledby="rc"
                                                                                class="navds-radio__input"
                                                                                value="true"><label for="radio-rb"
                                                                                                    class="navds-radio__label"><span
                                    class="navds-radio__content"><span id="rc" aria-hidden="true"
                                                                       class="navds-body-short navds-body-short--medium">Ja</span></span></label>
                            </div>
                            <div class="navds-radio navds-radio--medium"><input id="radio-rh" name="vilSendeDokumentasjon"
                                                                                type="radio" aria-labelledby="ri"
                                                                                class="navds-radio__input"
                                                                                value="false"><label for="radio-rh"
                                                                                                     class="navds-radio__label"><span
                                    class="navds-radio__content"><span id="ri" aria-hidden="true"
                                                                       class="navds-body-short navds-body-short--medium">Nei</span></span></label>
                            </div>
                        </div>
                        <div id="fieldset-error-r7" aria-relevant="additions removals" aria-live="polite"
                             class="navds-fieldset__error"></div>
                    </fieldset>
                </div>
                <section aria-label="Demo med ikon"
                         class="navds-expansioncard _expansionCard_x11jn_1 expansion-card--pdf navds-expansioncard--medium"
                         data-landmark-index="3">
                    <div class="navds-expansioncard__header" data-open="false">
                        <div class="navds-expansioncard__header-content">
                            <div style="--__ac-stack-gap-xs:var(--a-spacing-4);--__ac-stack-direction-xs:row;--__ac-stack-align-xs:center"
                                 class="navds-stack navds-hstack navds-stack-gap navds-stack-align navds-stack-direction">
                                <div aria-hidden="true">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none"
                                         viewBox="0 0 24 24" focusable="false" role="img" aria-hidden="true"
                                         font-size="3rem">
                                        <path fill="currentColor" fill-rule="evenodd"
                                              d="M4 3.25a.75.75 0 0 0-.75.75v16c0 .414.336.75.75.75h16a.75.75 0 0 0 .75-.75V4a.75.75 0 0 0-.75-.75zm.75 16V4.75h14.5v14.5zM11 7.75a1 1 0 1 1 2 0 1 1 0 0 1-2 0M10.5 10a.75.75 0 0 0 0 1.5h.75v4h-.75a.75.75 0 0 0 0 1.5h3a.75.75 0 0 0 0-1.5h-.75v-4.75A.75.75 0 0 0 12 10z"
                                              clip-rule="evenodd"></path>
                                    </svg>
                                </div>
                                <div><h3
                                        class="navds-expansioncard__title navds-expansioncard__title--small navds-heading navds-heading--small">
                                    Krav om inntekt</h3>
                                    <p class="navds-link-panel__description navds-body-long navds-body-long--medium">Dette
                                        er ett av flere krav som må være oppfylt for å få dagpenger.</p></div>
                            </div>
                        </div>
                        <button class="navds-expansioncard__header-button" type="button" aria-expanded="false">
                            <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none" viewBox="0 0 24 24"
                                 focusable="false" role="img" aria-labelledby="title-Rim75"
                                 class="navds-expansioncard__header-chevron"><title id="title-Rim75">Vis mer</title>
                                <path fill="currentColor" fill-rule="evenodd"
                                      d="M5.97 9.47a.75.75 0 0 1 1.06 0L12 14.44l4.97-4.97a.75.75 0 1 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06 0l-5.5-5.5a.75.75 0 0 1 0-1.06"
                                      clip-rule="evenodd"></path>
                            </svg>
                        </button>
                    </div>
                    <div aria-hidden="true" data-open="false"
                         class="navds-expansioncard__content navds-expansioncard__content--closed navds-body-long navds-body-long--medium">
                        <div class="navds-expansioncard__content-inner"><p class="navds-body-long navds-body-long--medium">
                            For å få dagpenger må du ha hatt en inntekt på minst 186 042 kroner de siste 12 månedene, eller
                            minst 372 084 kroner de siste 36 månedene. <br><br> Hvis du ikke har tjent nok vil du antagelig
                            få avslag på søknaden din om dagpenger.</p></div>
                    </div>
                </section>
                <button type="button"
                        class="mt-14 sendinn-button--pdf navds-button navds-button--primary navds-button--medium"><span
                        class="navds-label">Send inn</span><span class="navds-button__icon"><svg
                        xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none" viewBox="0 0 24 24"
                        focusable="false" role="img"><path fill="currentColor" fill-rule="evenodd"
                                                           d="M6.317 4.32a.75.75 0 0 0-1.023.932L7.704 12l-2.41 6.748a.75.75 0 0 0 1.023.932l15-7a.75.75 0 0 0 0-1.36zm2.712 6.93L7.31 6.44 19.227 12 7.31 17.56l1.719-4.81H12.5a.75.75 0 0 0 0-1.5z"
                                                           clip-rule="evenodd"></path></svg></span></button>
            </div>
        </form>
    </div>
    </body>
    </html>
    """.trimIndent()
