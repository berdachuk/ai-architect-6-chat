window.AiChat = window.AiChat || {};

window.AiChat.escapeHtml = function escapeHtml(text) {
    if (text == null) {
        return '';
    }
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
};

window.AiChat.apiHeaders = function apiHeaders(userId) {
    return {
        'X-User-Id': userId,
        'Content-Type': 'application/json'
    };
};
