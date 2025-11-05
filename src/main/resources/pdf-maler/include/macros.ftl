<#macro finnSvar spørsmål>
    Svar:
    <#if spørsmål.options??>
        <#if spørsmål.svar?is_enumerable>
            <#assign alleSvar=spørsmål.svar/>
        <#else>
            <#assign alleSvar=[spørsmål.svar]/>
        </#if>

        <#list alleSvar as svar>
            <#list spørsmål.options as option>
                <#if svar == option.value>
                    ${option.label}<#if (alleSvar?size > 1)><#sep>,</#sep></#if>
                </#if>
            </#list>
        </#list>
    <#else>
        ${spørsmål.svar}
    </#if>
</#macro>