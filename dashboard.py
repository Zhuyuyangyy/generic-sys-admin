"""
Flask System Administration Dashboard
Monitors CPU, memory, and processes via web API.
"""

import os
import psutil
import time
from flask import Flask, jsonify, render_template_string

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# ─── System Info ──────────────────────────────────────────────────────────────

def get_cpu_info():
    return {
        "percent": psutil.cpu_percent(interval=0.1),
        "count_logical": psutil.cpu_count(),
        "count_physical": psutil.cpu_count(logical=False),
        "freq_mhz": psutil.cpu_freq().current if psutil.cpu_freq() else 0,
    }

def get_mem_info():
    mem = psutil.virtual_memory()
    return {
        "total_gb": round(mem.total / (1024**3), 2),
        "available_gb": round(mem.available / (1024**3), 2),
        "used_gb": round(mem.used / (1024**3), 2),
        "percent": mem.percent,
    }

def get_disk_info():
    partitions = []
    for p in psutil.disk_partitions():
        try:
            usage = psutil.disk_usage(p.mountpoint)
            partitions.append({
                "device": p.device,
                "mountpoint": p.mountpoint,
                "fstype": p.fstype,
                "total_gb": round(usage.total / (1024**3), 2),
                "used_gb": round(usage.used / (1024**3), 2),
                "free_gb": round(usage.free / (1024**3), 2),
                "percent": usage.percent,
            })
        except PermissionError:
            continue
    return partitions

def get_processes(top=15):
    processes = []
    for p in psutil.process_iter(["pid", "name", "cpu_percent", "memory_percent", "status"]):
        try:
            info = p.info
            processes.append({
                "pid": info["pid"],
                "name": info["name"],
                "cpu_percent": round(info.get("cpu_percent") or 0, 1),
                "memory_percent": round(info.get("memory_percent") or 0, 2),
                "status": info.get("status", "unknown"),
            })
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
    processes.sort(key=lambda x: x["cpu_percent"], reverse=True)
    return processes[:top]

def get_net_io():
    io = psutil.net_io_counters()
    return {
        "bytes_sent_mb": round(io.bytes_sent / (1024**2), 2),
        "bytes_recv_mb": round(io.bytes_recv / (1024**2), 2),
        "packets_sent": io.packets_sent,
        "packets_recv": io.packets_recv,
    }

# ─── Simple HTML Dashboard ────────────────────────────────────────────────────

DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="utf-8">
<title>System Admin Dashboard</title>
<style>
  body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
  h1 { color: #333; }
  .card { background: white; border-radius: 8px; padding: 16px; margin-bottom: 16px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
  .card h2 { margin-top: 0; color: #444; font-size: 16px; }
  .metric { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee; }
  .metric:last-child { border-bottom: none; }
  .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
  .badge-high { background: #ff4444; color: white; }
  .badge-mid { background: #ffaa44; color: white; }
  .badge-ok { background: #44bb44; color: white; }
  table { width: 100%%; border-collapse: collapse; font-size: 13px; }
  th { background: #eee; text-align: left; padding: 6px; }
  td { padding: 5px 6px; border-bottom: 1px solid #f0f0f0; }
  tr:hover { background: #f9f9f9; }
  .refresh { margin-bottom: 12px; }
  .refresh a { padding: 6px 16px; background: #2196F3; color: white; text-decoration: none;
               border-radius: 4px; font-size: 13px; }
</style>
</head>
<body>
<h1>System Admin Dashboard</h1>
<div class="refresh"><a href="/">Refresh</a></div>

<div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px;">

<div class="card">
  <h2>CPU</h2>
  <div class="metric"><span>使用率</span><span class="badge {{ 'badge-high' if cpu.percent>80 else 'badge-mid' if cpu.percent>50 else 'badge-ok' }}">{{ cpu.percent }}%%</span></div>
  <div class="metric"><span>逻辑核心</span><span>{{ cpu.count_logical }}</span></div>
  <div class="metric"><span>物理核心</span><span>{{ cpu.count_physical }}</span></div>
  <div class="metric"><span>频率</span><span>{{ cpu.freq_mhz }} MHz</span></div>
</div>

<div class="card">
  <h2>Memory</h2>
  <div class="metric"><span>使用率</span><span class="badge {{ 'badge-high' if mem.percent>80 else 'badge-mid' if mem.percent>50 else 'badge-ok' }}">{{ mem.percent }}%%</span></div>
  <div class="metric"><span>总量</span><span>{{ mem.total_gb }} GB</span></div>
  <div class="metric"><span>已用</span><span>{{ mem.used_gb }} GB</span></div>
  <div class="metric"><span>可用</span><span>{{ mem.available_gb }} GB</span></div>
</div>

<div class="card">
  <h2>Network I/O</h2>
  <div class="metric"><span>发送</span><span>{{ net.bytes_sent_mb }} MB</span></div>
  <div class="metric"><span>接收</span><span>{{ net.bytes_recv_mb }} MB</span></div>
  <div class="metric"><span>发送包</span><span>{{ net.packets_sent }}</span></div>
  <div class="metric"><span>接收包</span><span>{{ net.packets_recv }}</span></div>
</div>

</div>

<div class="card">
  <h2>Disk</h2>
  <table>
    <tr><th>盘符</th><th>文件系统</th><th>总量</th><th>已用</th><th>可用</th><th>使用率</th></tr>
    {% for d in disk %}
    <tr>
      <td>{{ d.device }}</td>
      <td>{{ d.fstype }}</td>
      <td>{{ d.total_gb }} GB</td>
      <td>{{ d.used_gb }} GB</td>
      <td>{{ d.free_gb }} GB</td>
      <td class="badge {{ 'badge-high' if d.percent>80 else 'badge-mid' if d.percent>50 else 'badge-ok' }}">{{ d.percent }}%%</td>
    </tr>
    {% endfor %}
  </table>
</div>

<div class="card">
  <h2>Top Processes (by CPU)</h2>
  <table>
    <tr><th>PID</th><th>Name</th><th>CPU %%</th><th>Memory %%</th><th>Status</th></tr>
    {% for p in processes %}
    <tr>
      <td>{{ p.pid }}</td>
      <td>{{ p.name }}</td>
      <td>{{ p.cpu_percent }}</td>
      <td>{{ p.memory_percent }}</td>
      <td>{{ p.status }}</td>
    </tr>
    {% endfor %}
  </table>
</div>

</body>
</html>
"""

# ─── Routes ──────────────────────────────────────────────────────────────────

@app.route("/")
def index():
    cpu = get_cpu_info()
    mem = get_mem_info()
    disk = get_disk_info()
    net = get_net_io()
    processes = get_processes()
    return render_template_string(DASHBOARD_HTML,
                                  cpu=cpu, mem=mem, disk=disk,
                                  net=net, processes=processes)

@app.route("/api/status")
def api_status():
    return jsonify({
        "cpu": get_cpu_info(),
        "memory": get_mem_info(),
        "disk": get_disk_info(),
        "network": get_net_io(),
        "processes": get_processes(),
        "timestamp": time.time(),
    })

@app.route("/api/processes")
def api_processes():
    return jsonify(get_processes(top=30))

@app.route("/api/health")
def api_health():
    return jsonify({"status": "ok", "uptime": time.time()})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    debug = os.environ.get("FLASK_DEBUG", "false").lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)
