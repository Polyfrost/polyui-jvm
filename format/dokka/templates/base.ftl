<#import "includes/page_metadata.ftl" as page_metadata>
<#import "includes/header.ftl" as header>
<#import "includes/footer.ftl" as footer>
<!DOCTYPE html>
<html lang="en" class="theme-dark">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <@template_cmd name="pathToRoot">
    <script>var pathToRoot = "${pathToRoot}";</script>
    </@template_cmd>
    <#-- Resources (scripts, stylesheets) are handled by Dokka.
    Use customStyleSheets and customAssets to change them. -->
    <@resources/>
</head>
<body>
    <@header.display/>
<div id="container">
    <div id="leftColumn">
        <div id="sideMenu"></div>
    </div>
    <div id="main">
        <@content/>
        <@footer.display/>
    </div>
    <script>cleanSamples()</script>
</div>
</body>
</html>