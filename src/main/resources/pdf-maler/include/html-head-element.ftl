<head>
    <title>Søknad om dagpenger</title>
    <style>
        body {
            font-family: 'Source Sans Pro';
            font-style: normal;
            width: 600px;
            padding: 2rem;
            color: rgb(38, 38, 38);
        }

        h1, h2 {
            margin-top: 2rem;
        }

        h3 {
            margin-top: 1.5rem;
        }

        .modalSpørsmål {
            background: #eee;
            padding: 0.5rem 1rem;
            margin-bottom: 1rem;
            border-radius: 10px;
        }

        .pageHeaderRight {
            position: running(pageHeaderRight);
            font-size: 1em;
            padding: 4px;
            text-align: right;
        }

        @page {
            size: A4 portrait;
            @top-right {
                content: element(pageHeaderRight);
            }
            @bottom-right {
                content: counter(page);
                font-family: 'Source Sans Pro';
                padding-right: 15px;
            }
            orphans: 2;
        }

    </style>
</head>
