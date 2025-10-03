package no.nav.dagpenger.soknad.orkestrator.utils

val søknadCss =
    //language=CSS
    """
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: 'Source Sans Pro', sans-serif;
      color: #262626;
      background-color: #fff;
      padding: 40px 20px;
      line-height: 1.5;
    }

    .innhold {
      max-width: 800px;
      margin: 0 auto;
    }

    h2 {
      font-size: 1.75rem;
      margin-bottom: 24px;
    }

    .navds-form-summary {
      border: 1px solid #dcdcdc;
      border-radius: 8px;
      background-color: #f9f9f9;
      margin-bottom: 32px;
      overflow: hidden;
    }

    .navds-form-summary__header {
      background-color: #f1f3f5;
      padding: 16px 20px;
      border-bottom: 1px solid #dcdcdc;
    }

    .navds-heading--medium {
      font-size: 1.125rem;
      font-weight: 600;
      color: #262626;
    }

    .navds-form-summary__answers {
      padding: 16px 20px;
    }

    .navds-form-summary__answer {
      margin-bottom: 16px;
      border-bottom: 1px solid #e0e0e0;
      padding-bottom: 12px;
    }

    .navds-form-summary__answer:last-child {
      border-bottom: none;
      margin-bottom: 0;
      padding-bottom: 0;
    }

    .navds-form-summary__answer dt {
      font-weight: 600;
      margin-bottom: 4px;
      color: #262626;
    }

    .navds-form-summary__answer dd {
      margin-left: 0;
      color: #262626;
    }

    .navds-form-summary__footer {
      padding: 12px 20px;
      border-top: 1px solid #dcdcdc;
      background-color: #ffffff;
      text-align: left;
    }

    .navds-link {
      text-decoration: none;
      color: #0067C5;
      font-weight: 500;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .navds-link:hover {
      text-decoration: underline;
    }

    .navds-link svg {
      width: 1em;
      height: 1em;
      stroke-width: 2;
    }
    """.trimIndent()
