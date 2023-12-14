<#-- This is an unchanged copy of Dokka's base.ftl -->
<#import "includes/page_metadata.ftl" as page_metadata>
<#import "includes/header.ftl" as header>
<#import "includes/footer.ftl" as footer>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <@template_cmd name="pathToRoot"><script>var pathToRoot = "${pathToRoot}";</script></@template_cmd>
    <script>document.documentElement.classList.replace("no-js","js");</script>
    <#-- This script doesn't need to be there but it is nice to have
    since app in dark mode doesn't 'blink' (class is added before it is rendered) -->
    <script>const storage = localStorage.getItem("dokka-dark-mode")
        if (storage == null) {
            const osDarkSchemePreferred = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            if (osDarkSchemePreferred === true) {
                document.getElementsByTagName("html")[0].classList.add("theme-dark")
            }
        } else {
            const savedDarkMode = JSON.parse(storage)
            if(savedDarkMode === true) {
                document.getElementsByTagName("html")[0].classList.add("theme-dark")
            }
        }
    </script>
    <#-- Resources (scripts, stylesheets) are handled by Dokka.
    Use customStyleSheets and customAssets to change them. -->
    <@resources/>
</head>
<body>
<div class="root">
    <@header.display/>
    <div id="container">
        <div class="sidebar" id="leftColumn">
            <div class="sidebar--inner" id="sideMenu"></div>
        </div>
        <div id="main">
            <@content/>
            <@footer.display/>
        </div>
    </div>
</div>
</body>
</html>

<#-- Add "skip to main content" buttons to the sidebar dynamically -->
<script>
    document.addEventListener('DOMContentLoaded', function() {
        function insertSkipLink(element) {
            const skipLink = document.createElement('div');

            // Create an anchor element with the href pointing to #main
            const anchor = document.createElement('a');
            anchor.classList.add('skip-to-content');
            anchor.href = '#main';
            anchor.innerHTML = 'Skip to Main Content';
            anchor.setAttribute("tabindex", "0");

            skipLink.appendChild(anchor);

            if (element.children.length > 1) {
                element.insertBefore(skipLink, element.children[1]);
            } else {
                element.appendChild(skipLink);
            }
        }

        const observerConfig = {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class']
        };

        function handleChanges(mutationsList) {
            for (const mutation of mutationsList) {
                if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                    // Check added nodes for elements with class 'sideMenuPart' and without class 'hidden'
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType === 1 && node.classList.contains('sideMenuPart') && !node.classList.contains('hidden')) {
                            insertSkipLink(node);
                        }
                    });
                } else if (mutation.type === 'attributes' && mutation.target.classList.contains('sideMenuPart') && !mutation.target.classList.contains('hidden')) {
                    // Handle changes in the 'class' attribute of existing elements
                    // Check if the element is 'sideMenuPart' and not 'hidden'
                    insertSkipLink(mutation.target);
                }
            }
        }

        // Create a MutationObserver with the callback function
        const observer = new MutationObserver(handleChanges);

        // Start observing changes in the document
        observer.observe(document.body, observerConfig);
    });
</script>