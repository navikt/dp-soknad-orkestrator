<#import "macros.ftl" as macros/>
<#macro håndterSpørmål spørsmål>
    <#if spørsmål.svar??>
        <p>
            <strong>${spørsmål.label}</strong>
            <br/>
            <@macros.finnSvar spørsmål=spørsmål/>
        </p>
    </#if>
</#macro>
<#assign root = json?eval_json>
<html lang="no">
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

        .modalSpørsmål {
            background: #eee;
            padding: 1rem;
            margin-bottom: 1rem;
        }

        @page {
            size: A4 portrait;
            @top-right {
                content: counter(page);
                font-family: 'Source Sans Pro';
                padding-right: 15px;
            }
            orphans: 2;
        }
    </style>
</head>
<body>
<h1>Dagpengesøknad</h1>
<p>Innsendt av ${root.navn} (${root.ident}) den ${root.innsendtTidspunkt}</p>
<#list root.seksjoner as seksjon>
    <h2>${seksjon.navn}</h2>
    <#list seksjon.spørsmål as spørsmål>
        <#if spørsmål?is_enumerable>
            <div class="modalSpørsmål">
                <#list spørsmål as modalSpørsmål>
                    <@håndterSpørmål spørsmål=modalSpørsmål/>
                </#list>
            </div>
        <#else>
            <@håndterSpørmål spørsmål=spørsmål/>
        </#if>
    </#list>
</#list>
</body>
</html>