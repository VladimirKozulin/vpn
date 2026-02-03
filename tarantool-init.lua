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
    
    print('Tarantool initialized successfully with users and vpn_clients')
end)
