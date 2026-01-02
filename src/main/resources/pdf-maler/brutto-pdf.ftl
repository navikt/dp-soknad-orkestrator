<#import "include/macros.ftl" as macros/>
<#macro håndterSpørsmål spørsmål>
    <div class="spørsmål">
        <#if spørsmål.label??>
            <div>
                <strong>
                    <#if spørsmål.type == "dokumentasjonskravindikator">
                        Dokumentasjonskrav:
                    </#if>
                    ${spørsmål.label}
                </strong>
            </div>
        </#if>
        <#if spørsmål.description??>
            <div><i>${spørsmål.description}</i></div>
        </#if>
        <#if spørsmål.svar??>
            <div>
                <@macros.finnSvar spørsmål=spørsmål/>
            </div>
            <#if spørsmål.options??>
                <div>Svaralternativene
                    var: <#list spørsmål.options as option>${option.label}<#sep>, </#sep></#list></div>
            </#if>
        </#if>
    </div>
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