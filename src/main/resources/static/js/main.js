window.AiChat = window.AiChat || {};

window.AiChat.escapeHtml = function escapeHtml(text) {
    if (text == null) {
        return '';
    }
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/&quot;/g, '"');
};

window.AiChat.apiHeaders = function apiHeaders(userId) {
    const headers = { 'Content-Type': 'application/json' };
    const config = window.AICHAT_CONFIG || {};
    if (!config.oauth2LoginEnabled) {
        headers['X-User-Id'] = userId;
    }
    return headers;
};

(function initMarkdown() {
    if (!window.marked) {
        return;
    }

    if (window.hljs) {
        try {
            window.hljs.configure({ ignoreUnescapedHTML: true });
        } catch (e) { /* noop */ }
        window.marked.setOptions({
            gfm: true,
            breaks: false,
            pedantic: false,
            highlight: function (code, lang) {
                if (!lang) {
                    return window.hljs.highlightAuto(code).value;
                }
                if (window.hljs.getLanguage(lang)) {
                    return window.hljs.highlight(code, { language: lang, ignoreIllegals: true }).value;
                }
                return window.hljs.highlightAuto(code).value;
            }
        });
    } else {
        window.marked.setOptions({ gfm: true, breaks: false });
    }

    const renderer = new window.marked.Renderer();
    const baseLink = renderer.link.bind(renderer);
    renderer.link = function (href, title, text) {
        const html = baseLink(href, title, text);
        return html.replace(/^<a /, '<a target="_blank" rel="noopener noreferrer" ');
    };
    window.marked.use({ renderer: renderer });
})();
