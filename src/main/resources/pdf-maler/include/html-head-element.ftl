<head>
    <title>Søknad om dagpenger</title>
    <style>
        body {
            font-family: 'Source Sans Pro';
            font-style: normal;
            width: 600px;
            padding: 0 40px 40px 40px;
            color: rgb(38, 38, 38);
        }

        .spørsmål {
            margin: 1rem 0 1rem 0;
        }

        .modalSpørsmål {
            background: #eee;
            padding: 1rem;
            margin: 1rem 0 1rem 0;
        }

        .pageHeaderRight {
            position: running(pageHeaderRight);
            font-size: 0.6em;
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
