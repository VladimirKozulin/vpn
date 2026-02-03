-- Инициализация Tarantool для VPN сервера с системой пользователей

box.cfg{
    listen = 3301,
    log_level = 5
}

-- Создаем пользователя admin
box.once('init', function()
    box.schema.user.create('admin', {password = 'secret', if_not_exists = true})
    box.schema.user.grant('admin', 'read,write,execute', 'universe', nil, {if_not_exists = true})
    
    -- ========================================
    -- Space для пользователей системы
    -- ========================================
    local users = box.schema.space.create('users', {if_not_exists = true})
    
    users:format({
        {name = 'id', type = 'unsigned'},
        {name = 'email', type = 'string'},
        {name = 'password_hash', type = 'string'},
        {name = 'name', type = 'string'},
        {name = 'role', type = 'string'}, -- 'GUEST', 'USER', 'ADMIN'
        {name = 'created_at', type = 'string'}
    })
    
    -- Индексы для users
    users:create_index('primary', {parts = {'id'}, if_not_exists = true})
    users:create_index('email', {parts = {'email'}, unique = true, if_not_exists = true})
    
    -- Sequence для auto_increment users
    box.schema.sequence.create('users_seq', {if_not_exists = true})
    
    -- ========================================
    -- Space для VPN клиентов (обновленный)
    -- ========================================
    local vpn_clients = box.schema.space.create('vpn_clients', {if_not_exists = true})
    
    vpn_clients:format({
        {name = 'id', type = 'unsigned'},
        {name = 'user_id', type = 'unsigned', is_nullable = true}, -- NULL для гостей
        {name = 'uuid', type = 'string'},
        {name = 'device_info', type = 'string', is_nullable = true},
        {name = 'ip_address', type = 'string', is_nullable = true},
        {name = 'country', type = 'string', is_nullable = true},
        {name = 'is_active', type = 'boolean'},
        {name = 'traffic_limit_gb', type = 'unsigned'},
        {name = 'traffic_used_gb', type = 'double'},
        {name = 'expires_at', type = 'string', is_nullable = true},
        {name = 'last_connected_at', type = 'string', is_nullable = true},
        {name = 'created_at', type = 'string'}
    })
    
    -- Индексы для vpn_clients
    vpn_clients:create_index('primary', {parts = {'id'}, if_not_exists = true})
    vpn_clients:create_index('uuid', {parts = {'uuid'}, unique = true, if_not_exists = true})
    vpn_clients:create_index('user_id', {parts = {'user_id'}, unique = false, if_not_exists = true})
    
    -- Sequence для auto_increment vpn_clients
    box.schema.sequence.create('vpn_clients_seq', {if_not_exists = true})
    
    -- ========================================
    -- Space для refresh токенов
    -- ========================================
    local refresh_tokens = box.schema.space.create('refresh_tokens', {if_not_exists = true})
    
    refresh_tokens:format({
        {name = 'id', type = 'unsigned'},
        {name = 'user_id', type = 'unsigned'},
        {name = 'token', type = 'string'},
        {name = 'expires_at', type = 'string'},
        {name = 'created_at', type = 'string'},
        {name = 'revoked', type = 'boolean'}
    })
    
    -- Индексы для refresh_tokens
    refresh_tokens:create_index('primary', {parts = {'id'}, if_not_exists = true})
    refresh_tokens:create_index('token', {parts = {'token'}, unique = true, if_not_exists = true})
    refresh_tokens:create_index('user_id', {parts = {'user_id'}, unique = false, if_not_exists = true})
    
    -- Sequence для auto_increment refresh_tokens
    box.schema.sequence.create('refresh_tokens_seq', {if_not_exists = true})
    
    -- Функция для удаления истекших токенов
    function delete_expired_refresh_tokens(current_time)
        local tokens = box.space.refresh_tokens:select()
        local deleted = 0
        for _, token in ipairs(tokens) do
            if token[4] < current_time then -- expires_at < current_time
                box.space.refresh_tokens:delete(token[1])
                deleted = deleted + 1
            end
        end
        return deleted
    end
    
    print('Tarantool initialized successfully with users, vpn_clients and refresh_tokens')
end)
