INSERT INTO ai_chat.mcp_connection (
    id, name, url, tools_enabled, resources_enabled, prompts_enabled, created_at, updated_at)
VALUES (
    :id, :name, :url, :toolsEnabled, :resourcesEnabled, :promptsEnabled, :createdAt, :updatedAt)
