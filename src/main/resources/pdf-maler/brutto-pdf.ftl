<#import "macros.ftl" as macros/>
<#macro håndterSpørsmål spørsmål>
    <p>
    <strong>${spørsmål.label}</strong>
    <#if spørsmål.description??>
        <div><i>${spørsmål.description}</i></div>
    </#if>
    <#if spørsmål.svar??>
        <br/>
        <@macros.finnSvar spørsmål=spørsmål/>
        <#if spørsmål.options??>
            <div>Svaralternativene
                var: <#list spørsmål.options as option>${option.label}<#sep>, </#sep></#list></div>
        </#if>
    </#if>
    </p>
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
                    <@håndterSpørsmål spørsmål=modalSpørsmål/>
                </#list>
            </div>
        <#else>
            <@håndterSpørsmål spørsmål=spørsmål/>
        </#if>
    </#list>
</#list>
</body>
</html>