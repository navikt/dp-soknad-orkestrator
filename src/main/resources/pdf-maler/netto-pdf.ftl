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
        <#default>
            <#return svar>
    </#switch>
</#function>
<#macro håndterSpørmål spørsmål>
    <#if spørsmål.svar??>
        <p>
            <#if spørsmål.label??>
                <strong>${spørsmål.label}</strong>
            </#if>
            <br/>
            <@macros.finnSvar spørsmål=spørsmål/>
        </p>
    </#if>
</#macro>
<#assign root = json?eval_json>

<html lang="no">
<#include "include/html-head-element.ftl"/>
<body>
<#include "include/page-header-right.ftl"/>
<#include "include/søknad-om-dagpenger-header.ftl"/>
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
<#if root.dokumentasjonskrav?? && root.dokumentasjonskrav?size gt 0>
    <h2>Dokumentasjonskrav</h2>
    <#list root.dokumentasjonskrav as dokumentkrav>
        <#list dokumentkrav as krav>
            <p>
                <#if krav.tittel??>
                    <strong>${krav.tittel}</strong>
                </#if>
                <br/>
                <#if krav.svar??>
                    Svar: ${mapDokumentasjonskravSvar(krav.svar)}
                </#if>
                <#if krav.begrunnelse??>
                    <br/>
                    Begrunnelse: ${krav.begrunnelse}
                </#if>
            </p>
        </#list>
    </#list>
</#if>
</body>
</html>