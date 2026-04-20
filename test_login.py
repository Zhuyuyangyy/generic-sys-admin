import urllib.request
import json

url = 'http://localhost:8081/api/users/login'
data = json.dumps({'username': 'admin', 'password': 'admin123'}).encode()
req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})

try:
    with urllib.request.urlopen(req, timeout=5) as resp:
        print(f"Status: {resp.status}")
        body = resp.read().decode()
        print(f"Body: {body[:300]}")
except Exception as e:
    print(f"Error: {e}")