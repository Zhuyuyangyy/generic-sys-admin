import bcrypt

hashes = {
    'admin':   b'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS0/WhObS5l1Kx.Xu',
    'operator': b'$2a$10$EixZaYVK1fsbw1UrFbPFNXu9D0cN0lHhT6o5r6dqWp6q5XqXGqQqy',
    'viewer':   b'$2a$10$rOqEgqN5hSdhq6dLGqXyXOsN1cN1lHhT6o5r6dqWp6q5XqXGqQqy',
}

# Try various passwords
candidates = [
    'admin', '123456', 'password', 'admin123', 'admin888',
    'root', 'zyy123', 'zyy123456', 'admin123456',
    'admin0', 'admin1', 'Admin123', 'Administrator',
    'test', 'test1234', 'system', 'manager',
    'qwer1234', 'password1', 'pass1234', 'P@ssw0rd',
]

for username, h in hashes.items():
    print(f'\n=== {username} ===')
    for p in candidates:
        try:
            ok = bcrypt.checkpw(p.encode('utf-8'), h)
            print(f'  {p:20s} -> {ok}')
            if ok:
                print(f'  *** MATCH FOUND for {username}: {p}')
        except Exception as e:
            print(f'  {p:20s} -> ERR: {e}')
