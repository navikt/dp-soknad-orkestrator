<#import "include/macros.ftl" as macros/>
<#function mapDokumentasjonskravSvar svar>
    <#switch svar>
        <#case "dokumentkravSvarSendNå">
            <#return "Jeg vil laste opp nå">
        <#case "dokumentkravSvarSenderIkke">
            <#return "Jeg sender det ikke">
        <#case "dokumentkravSvarSenderSenere">
            <#return "Jeg sender det senere">
        <#case "dokumentkravSvarSendtTidligere">
            <#return "Jeg har sendt det i en tidligere søknad om dagpenger">
        <#case "dokumentkravEttersendt">
            <#return "Jeg har ettersendt">
        <#default>
            <#return svar>
    </#switch>
</#function>
<#macro håndterSpørsmål spørsmål>
    <p>
        <#if spørsmål.label??>
            <strong>${spørsmål.label?html}</strong>
        </#if>
        <#if spørsmål.description??>
            <div><i>${spørsmål.description?html}</i></div>
        </#if>
        <#if spørsmål.svar??>
            <br/>
            <@macros.finnSvar spørsmål=spørsmål/>
            <#if spørsmål.options??>
                <div>Svaralternativene
                    var: <#list spørsmål.options as option>${option.label?html}<#sep>, </#sep></#list></div>
            </#if>
        </#if>
    </p>
</#macro>
<#assign root = json?eval_json>

<html lang="no">
<#include "include/html-head-element.ftl"/>
<body>
<#include "include/page-header-right.ftl"/>
<h1>Søknad om dagpenger</h1>
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