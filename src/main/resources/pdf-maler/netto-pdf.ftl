<#import "include/macros.ftl" as macros/>
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
</body>
</html>