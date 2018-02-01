function formatMarked(item) {
    var elem = $(item);
    //&lt;md&gt;.*&lt;/md&gt;
    //var html = elem.html().replace("<md>", "").replace("</md>", "").replace(new RegExp("\\n\\s+"), "\n").trim();
    var html = elem.html().replace("<md>", "").replace("</md>", "").trim();

    html = html.replace(new RegExp("&amp;", 'g'), "&").replace(new RegExp("&lt;", 'g'), "<").replace(new RegExp("&gt;", 'g'), ">");
    elem.html(marked(html));
}

$(function () {

    var items = $("[data-marked-id$='marked']");

    for (var index = 0; index < items.length; index++)
        formatMarked(items[index]);

    $('code').each(function (i, block) {
        hljs.highlightBlock(block);
    });
});
