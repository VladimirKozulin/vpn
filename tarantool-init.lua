-- Инициализация Tarantool для VPN сервера

box.cfg{
    listen = 3301,
    log_level = 5
}

-- Создаем пользователя admin
box.once('init', function()
    box.schema.user.create('admin', {password = 'secret', if_not_exists = true})
    box.schema.user.grant('admin', 'read,write,execute', 'universe', nil, {if_not_exists = true})
    
    -- Создаем space для VPN клиентов
    local space = box.schema.space.create('vpn_clients', {if_not_exists = true})
    
    -- Определяем формат
    space:format({
        {name = 'id', type = 'unsigned'},
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
    
    -- Создаем индексы
    space:create_index('primary', {parts = {'id'}, if_not_exists = true})
    space:create_index('uuid', {parts = {'uuid'}, unique = true, if_not_exists = true})
    
    -- Создаем sequence для auto_increment
    box.schema.sequence.create('vpn_clients_seq', {if_not_exists = true})
    
    print('Tarantool initialized successfully')
end)
