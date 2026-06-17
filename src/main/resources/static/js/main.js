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
    const headers = { 'Content-Type': 'application/json' };
    const config = window.AICHAT_CONFIG || {};
    if (!config.oauth2LoginEnabled) {
        headers['X-User-Id'] = userId;
    }
    return headers;
};
