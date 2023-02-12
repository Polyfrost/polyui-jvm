<#macro display>
    <title>${pageName}</title>
    <@template_cmd name="pathToRoot">
    <link href="${pathToRoot}images/polyui_icon.svg" rel="icon" type="image/svg">
    </@template_cmd>
    <meta property="og:title" content="${pageName}"/>
    <meta property="og:type" content="website"/>
    <@template_cmd name="projectName">
    <meta property="og:site_name" content="${projectName}"/>
    </@template_cmd>
</#macro>